package com.kresshy.weatherstation.domain;

import com.kresshy.weatherstation.repository.WeatherRepository;

import javax.inject.Inject;

/**
 * UseCase for managing the Bluetooth discovery lifecycle.
 * Acts as a clean interface for starting and stopping scans for new weather stations.
 */
public class ManageDiscoveryUseCase {

    private final WeatherRepository repository;

    @Inject
    public ManageDiscoveryUseCase(WeatherRepository repository) {
        this.repository = repository;
    }

    /** Starts scanning for new devices. */
    public void startDiscovery() {
        repository.startDiscovery();
    }

    /** Stops any active scan. */
    public void stopDiscovery() {
        repository.stopDiscovery();
    }

    /** Clears previously discovered devices from the repository buffer. */
    public void clearResults() {
        repository.clearDiscoveredDevices();
    }
}
