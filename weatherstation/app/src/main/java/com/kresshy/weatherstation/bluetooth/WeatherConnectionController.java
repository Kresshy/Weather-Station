package com.kresshy.weatherstation.bluetooth;

import android.os.Parcelable;

import androidx.lifecycle.LiveData;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.util.Resource;

import java.util.List;

/**
 * Interface for the Control Plane. Manages the lifecycle of the connection to the weather station,
 * Bluetooth hardware state, and device discovery.
 */
public interface WeatherConnectionController {

    /**
     * @return Observable high-level UI status (LOADING, SUCCESS, ERROR).
     */
    LiveData<Resource<Void>> getUiState();

    /**
     * @return Observable connection state.
     */
    LiveData<ConnectionState> getConnectionState();

    /**
     * @return Observable currently connected device name.
     */
    LiveData<String> getConnectedDeviceName();

    /**
     * @return Observable discovery status (true if scanning).
     */
    LiveData<Boolean> isDiscovering();

    /**
     * @return Observable discovery status string.
     */
    LiveData<String> getDiscoveryStatus();

    /**
     * @return Observable Bluetooth adapter state (ON/OFF).
     */
    LiveData<Integer> getBluetoothState();

    /**
     * @return Observable list of discovered (unpaired) devices.
     */
    LiveData<List<Parcelable>> getDiscoveredDevices();

    /**
     * @return true if Bluetooth is currently enabled.
     */
    boolean isBluetoothEnabled();

    /** Requests to enable the Bluetooth adapter. */
    void enableBluetooth();

    /** Disables the Bluetooth adapter. */
    void disableBluetooth();

    /** Clears the current discovery results. */
    void clearDiscoveredDevices();

    /** Enables background connection management. */
    void startConnection();

    /** Disables connection management. */
    void stopConnection();

    /** Starts a Bluetooth device scan. */
    void startDiscovery();

    /** Stops the Bluetooth device scan. */
    void stopDiscovery();

    /** Connects to a specific device. */
    void connectToDevice(Parcelable device);

    /** Registers Bluetooth and state receivers. */
    void registerReceivers();

    /** Unregisters system receivers. */
    void unregisterReceivers();

    /**
     * @param address MAC address to connect to.
     */
    void connectToDeviceAddress(String address);
}
