package com.kresshy.weatherstation.domain;

import android.content.Context;
import android.content.SharedPreferences;

import com.kresshy.weatherstation.R;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;

/**
 * UseCase for initiating a connection to a weather station. This component handles the business
 * logic for saving the device's unique identifier, selecting the appropriate hardware abstraction,
 * and triggering the connection sequence.
 */
public class ConnectToDeviceUseCase {

    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;
    private final SharedPreferences sharedPreferences;
    private final Context context;

    /**
     * Initializes the UseCase with necessary dependencies for connection management and
     * persistence.
     *
     * @param connectionController Controller for the hardware connection lifecycle.
     * @param sharedPreferences For persisting the device address for future auto-reconnects.
     * @param context Application context for accessing resource strings.
     */
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
     * Executes the connection logic for a specific device. This involves persisting the device
     * address to ensure seamless reconnection in future sessions and instructing the controller to
     * establish the physical link.
     *
     * @param address The MAC address of the physical weather station or a simulator identifier.
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
