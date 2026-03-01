package com.kresshy.weatherstation.domain;

import android.bluetooth.BluetoothDevice;

import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;

import javax.inject.Inject;

/** UseCase for initiating a pairing request with a Bluetooth device. */
public class PairDeviceUseCase {
    private final WeatherConnectionController connectionController;

    @Inject
    public PairDeviceUseCase(WeatherConnectionController connectionController) {
        this.connectionController = connectionController;
    }

    public void execute(BluetoothDevice device) {
        connectionController.pairDevice(device);
    }
}
