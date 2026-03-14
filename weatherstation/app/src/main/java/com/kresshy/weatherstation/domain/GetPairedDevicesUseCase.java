package com.kresshy.weatherstation.domain;

import android.os.Parcelable;

import java.util.List;

import javax.inject.Inject;

/**
 * UseCase for retrieving the list of paired weather stations. This component provides the UI with a
 * filtered set of available hardware and virtual devices based on the current system configuration.
 */
public class GetPairedDevicesUseCase {

    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;

    /**
     * Initializes the UseCase with the connection controller.
     *
     * @param connectionController The controller used to query the underlying hardware for paired
     *     devices.
     */
    @Inject
    public GetPairedDevicesUseCase(
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController) {
        this.connectionController = connectionController;
    }

    /**
     * Fetches the current list of paired devices. This is used to populate the device selection
     * screen with previously known weather stations.
     *
     * @return A list of available paired devices, including both physical Bluetooth hardware and
     *     virtual simulator nodes.
     */
    public List<Parcelable> execute() {
        return connectionController.getPairedDevices();
    }
}
