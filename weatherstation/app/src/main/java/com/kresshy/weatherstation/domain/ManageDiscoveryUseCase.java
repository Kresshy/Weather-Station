package com.kresshy.weatherstation.domain;

import javax.inject.Inject;

/**
 * UseCase for managing the Bluetooth discovery lifecycle. Acts as a clean interface for starting
 * and stopping scans for new weather stations.
 */
public class ManageDiscoveryUseCase {

    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;

    @Inject
    public ManageDiscoveryUseCase(
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController) {
        this.connectionController = connectionController;
    }

    /** Starts scanning for new devices. */
    public void startDiscovery() {
        connectionController.startDiscovery();
    }

    /** Stops any active scan. */
    public void stopDiscovery() {
        connectionController.stopDiscovery();
    }

    /** Clears previously discovered devices from the controller buffer. */
    public void clearResults() {
        connectionController.clearDiscoveredDevices();
    }
}
