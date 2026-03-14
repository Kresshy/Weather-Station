package com.kresshy.weatherstation.domain;

import javax.inject.Inject;

/**
 * UseCase for managing the Bluetooth discovery lifecycle. This component provides a clean interface
 * for the UI to trigger and control scans for nearby weather station hardware without needing
 * knowledge of low-level Bluetooth APIs.
 */
public class ManageDiscoveryUseCase {

    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;

    /**
     * Initializes the UseCase with the connection controller.
     *
     * @param connectionController The controller used to interact with the device's Bluetooth
     *     hardware.
     */
    @Inject
    public ManageDiscoveryUseCase(
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController) {
        this.connectionController = connectionController;
    }

    /**
     * Starts scanning for new devices. This is triggered when the user opens the device selection
     * screen to find new hardware.
     */
    public void startDiscovery() {
        connectionController.startDiscovery();
    }

    /**
     * Stops any active scan. This should be called when a device is selected or when the user
     * navigates away from the selection screen to conserve battery.
     */
    public void stopDiscovery() {
        connectionController.stopDiscovery();
    }

    /**
     * Clears previously discovered devices from the internal buffer. This ensures that the UI
     * reflects only the results of the most recent scan.
     */
    public void clearResults() {
        connectionController.clearDiscoveredDevices();
    }
}
