package com.kresshy.weatherstation.domain;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.bluetooth.SimulatorDevice;
import com.kresshy.weatherstation.repository.WeatherRepository;

import dagger.hilt.android.qualifiers.ApplicationContext;

import timber.log.Timber;

import javax.inject.Inject;

/**
 * UseCase for initiating a connection to a weather station. Handles the logic of saving the device
 * address, selecting between simulator and physical hardware, and triggering the repository's
 * connection sequence.
 */
public class ConnectToDeviceUseCase {

    private final WeatherRepository repository;
    private final SharedPreferences sharedPreferences;
    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;

    @Inject
    public ConnectToDeviceUseCase(
            WeatherRepository repository,
            SharedPreferences sharedPreferences,
            @androidx.annotation.Nullable BluetoothAdapter bluetoothAdapter,
            @ApplicationContext Context context) {
        this.repository = repository;
        this.sharedPreferences = sharedPreferences;
        this.bluetoothAdapter = bluetoothAdapter;
        this.context = context;
    }

    /**
     * Connects to a device by its MAC address.
     *
     * @param address The MAC address of the weather station.
     */
    public void execute(String address) {
        // 1. Persist the address for auto-reconnect
        sharedPreferences
                .edit()
                .putString(context.getString(R.string.PREFERENCE_DEVICE_ADDRESS), address)
                .apply();

        // 2. Decide on the device type
        if (address.equals(SimulatorDevice.SIMULATOR_ADDRESS)
                && sharedPreferences.getBoolean("pref_simulator_mode", false)) {
            SimulatorDevice simDevice = new SimulatorDevice("Simulator Station", address);
            repository.connectToDevice(simDevice);
        } else if (bluetoothAdapter != null) {
            try {
                android.bluetooth.BluetoothDevice device =
                        bluetoothAdapter.getRemoteDevice(address);
                repository.connectToDevice(device);
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Invalid Bluetooth address: %s", address);
            }
        } else {
            Timber.e("Cannot connect: BluetoothAdapter is null and not in simulator mode");
        }
    }
}
