package com.kresshy.weatherstation.domain;

import android.bluetooth.BluetoothDevice;

import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;

import javax.inject.Inject;

/**
 * UseCase for initiating a pairing request with a Bluetooth device. This component abstracts the
 * pairing logic, allowing the UI to trigger the secure handshake required for communication with
 * physical weather station hardware.
 */
public class PairDeviceUseCase {
    private final WeatherConnectionController connectionController;

    /**
     * Initializes the UseCase with the connection controller.
     *
     * @param connectionController The controller used to manage the pairing process.
     */
    @Inject
    public PairDeviceUseCase(WeatherConnectionController connectionController) {
        this.connectionController = connectionController;
    }

    /**
     * Initiates the pairing sequence for a specific Bluetooth device. This is necessary to
     * establish a secure, authenticated link between the Android device and the weather station.
     *
     * @param device The Bluetooth device to be paired.
     */
    public void execute(BluetoothDevice device) {
        connectionController.pairDevice(device);
    }
}
