package com.kresshy.weatherstation.activity;

import android.Manifest;
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

    public interface Callback {
        void onPermissionsGranted();

        void onPermissionsDenied();
    }

    private final AppCompatActivity activity;
    private final Callback callback;
    private final ActivityResultLauncher<String[]> launcher;

    public PermissionDelegate(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.launcher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        this::handleResult);
    }

    /** Triggers the permission request flow. */
    public void requestPermissions() {
        ArrayList<String> permissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            permissionList.add(Manifest.permission.BLUETOOTH);
            permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        Timber.d("PermissionDelegate: Launching request...");
        launcher.launch(permissionList.toArray(new String[0]));
    }

    private void handleResult(Map<String, Boolean> result) {
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
