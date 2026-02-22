package com.kresshy.weatherstation.domain;

import android.content.Context;
import android.content.SharedPreferences;

import com.kresshy.weatherstation.R;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;

/**
 * UseCase for initiating a connection to a weather station. Handles the logic of saving the device
 * address, selecting between simulator and physical hardware, and triggering the repository's
 * connection sequence.
 */
public class ConnectToDeviceUseCase {

    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;
    private final SharedPreferences sharedPreferences;
    private final Context context;

    @Inject
    public ConnectToDeviceUseCase(
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController,
            SharedPreferences sharedPreferences,
            @ApplicationContext Context context) {
        this.connectionController = connectionController;
        this.sharedPreferences = sharedPreferences;
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

        // 2. Delegate connection to the Control Plane
        connectionController.connectToDeviceAddress(address);
    }
}
