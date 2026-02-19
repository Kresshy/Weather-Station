package com.kresshy.weatherstation.bluetooth;

/**
 * Interface for managing Bluetooth hardware state and device discovery. Separates Android Bluetooth
 * APIs from the rest of the application logic.
 */
public interface WeatherBluetoothManager {
    /** Starts scanning for nearby Bluetooth devices. */
    void startDiscovery();

    /** Stops the active Bluetooth scan. */
    void stopDiscovery();

    /** Requests to enable the Bluetooth adapter. */
    void enableBluetooth();

    /** Disables the Bluetooth adapter. */
    void disableBluetooth();

    /**
     * @return true if Bluetooth is currently enabled.
     */
    boolean isBluetoothEnabled();

    /** Registers Intent Filters for Bluetooth discovery and state changes. */
    void registerReceivers();

    /** Unregisters system receivers. */
    void unregisterReceivers();

    /**
     * @return Observable list of Bluetooth devices found during discovery.
     */
    androidx.lifecycle.LiveData<java.util.List<android.bluetooth.BluetoothDevice>>
            getDiscoveredDevices();

    /**
     * @return Observable string representing the current scan status.
     */
    androidx.lifecycle.LiveData<String> getDiscoveryStatus();
}
