package com.kresshy.weatherstation.domain;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;

import com.kresshy.weatherstation.bluetooth.SimulatorDevice;
import com.kresshy.weatherstation.util.PermissionHelper;

import dagger.hilt.android.qualifiers.ApplicationContext;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * UseCase for retrieving the list of paired weather stations. Handles filtering logic based on
 * simulator mode and permission status.
 */
public class GetPairedDevicesUseCase {

    private final SharedPreferences sharedPreferences;
    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;

    @Inject
    public GetPairedDevicesUseCase(
            SharedPreferences sharedPreferences,
            @androidx.annotation.Nullable BluetoothAdapter bluetoothAdapter,
            @ApplicationContext Context context) {
        this.sharedPreferences = sharedPreferences;
        this.bluetoothAdapter = bluetoothAdapter;
        this.context = context;
    }

    /**
     * @return A list of available paired devices (Bluetooth and/or Simulator).
     */
    public List<Parcelable> execute() {
        boolean useSimulator = sharedPreferences.getBoolean("pref_simulator_mode", false);
        List<Parcelable> devices = new ArrayList<>();

        // 1. Add Simulator if enabled
        if (useSimulator) {
            devices.add(
                    new SimulatorDevice("Simulator Station", SimulatorDevice.SIMULATOR_ADDRESS));
        }

        // 2. Add Physical Paired Devices if Bluetooth is available and enabled
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (PermissionHelper.hasConnectPermission(context)) {
                Set<android.bluetooth.BluetoothDevice> bondedDevices =
                        bluetoothAdapter.getBondedDevices();
                if (bondedDevices != null) {
                    devices.addAll(bondedDevices);
                }
            } else {
                Timber.d("GetPairedDevicesUseCase: BLUETOOTH_CONNECT permission not granted");
            }
        }

        return devices;
    }
}
