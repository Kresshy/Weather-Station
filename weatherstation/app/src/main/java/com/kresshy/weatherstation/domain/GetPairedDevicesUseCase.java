package com.kresshy.weatherstation.domain;

import android.os.Parcelable;

import java.util.List;

import javax.inject.Inject;

/**
 * UseCase for retrieving the list of paired weather stations. Handles filtering logic based on
 * simulator mode and permission status.
 */
public class GetPairedDevicesUseCase {

    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;

    @Inject
    public GetPairedDevicesUseCase(
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController) {
        this.connectionController = connectionController;
    }

    /**
     * @return A list of available paired devices (Bluetooth and/or Simulator).
     */
    public List<Parcelable> execute() {
        return connectionController.getPairedDevices();
    }
}
