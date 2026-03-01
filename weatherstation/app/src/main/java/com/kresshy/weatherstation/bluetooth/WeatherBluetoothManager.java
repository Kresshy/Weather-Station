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

    /**
     * @return Observable boolean for discovery status.
     */
    androidx.lifecycle.LiveData<Boolean> isDiscovering();

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

    /**
     * @return Observable integer representing the current Bluetooth adapter state (ON/OFF).
     */
    androidx.lifecycle.LiveData<Integer> getBluetoothState();

    /**
     * @param address The device address.
     * @return The last known RSSI for this device, or 0 if not found.
     */
    int getDeviceRssi(String address);

    /**
     * Initiates pairing with a Bluetooth device.
     *
     * @param device The device to pair with.
     */
    void pairDevice(android.bluetooth.BluetoothDevice device);

    /**
     * Sets the PIN for a pairing request.
     *
     * @param device The device requesting the PIN.
     * @param pin The PIN code.
     */
    void setPin(android.bluetooth.BluetoothDevice device, String pin);

    /**
     * @return Observable event when a pairing request is received.
     */
    androidx.lifecycle.LiveData<android.bluetooth.BluetoothDevice> getPairingRequest();
}
