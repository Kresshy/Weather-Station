package com.kresshy.weatherstation.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.kresshy.weatherstation.util.PermissionHelper;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.Map;

/**
 * Delegate responsible for handling runtime permission requests and results. Centralizes logic for
 * Bluetooth, Location, and Notification permissions.
 */
public class PermissionDelegate {

    /**
     * Interface for communicating the result of the permission request process back to the host.
     */
    public interface Callback {
        /**
         * Triggered when all mandatory Bluetooth and Location permissions have been successfully
         * granted by the user.
         */
        void onPermissionsGranted();

        /**
         * Triggered when one or more required permissions were denied, preventing normal hardware
         * operations.
         */
        void onPermissionsDenied();

        /** Triggered when the user responds to the interactive Bluetooth enablement prompt. */
        void onBluetoothEnableResult(boolean enabled);
    }

    private final AppCompatActivity activity;
    private final Callback callback;
    private final ActivityResultLauncher<String[]> permissionLauncher;
    private final ActivityResultLauncher<Intent> bluetoothLauncher;

    /**
     * Constructs a new PermissionDelegate and registers the activity result launchers.
     *
     * @param activity The host activity.
     * @param callback The callback to handle permission outcomes.
     */
    public PermissionDelegate(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.permissionLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        this::handlePermissionResult);
        this.bluetoothLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            boolean enabled = result.getResultCode() == AppCompatActivity.RESULT_OK;
                            callback.onBluetoothEnableResult(enabled);
                        });
    }

    /**
     * Initiates the system permission request flow. This dynamically selects the required
     * permissions based on the device's Android version (SDK level).
     */
    public void requestPermissions() {
        ArrayList<String> permissionList = new ArrayList<>();

        // Location is generally required for Bluetooth discovery on all versions
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            permissionList.add(Manifest.permission.BLUETOOTH);
            permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        Timber.d("PermissionDelegate: Launching request...");
        permissionLauncher.launch(permissionList.toArray(new String[0]));
    }

    /** Launches the system dialog to request Bluetooth enablement. */
    public void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        bluetoothLauncher.launch(enableBtIntent);
    }

    private void handlePermissionResult(Map<String, Boolean> result) {
        boolean scanGranted = PermissionHelper.hasScanPermission(activity);
        boolean connectGranted = PermissionHelper.hasConnectPermission(activity);
        boolean mandatoryGranted = scanGranted && connectGranted;

        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            Timber.d(
                    "Permission: %s was %s!",
                    entry.getKey(), entry.getValue() ? "granted" : "not granted");
        }

        if (mandatoryGranted) {
            callback.onPermissionsGranted();
        } else {
            callback.onPermissionsDenied();
        }
    }
}
