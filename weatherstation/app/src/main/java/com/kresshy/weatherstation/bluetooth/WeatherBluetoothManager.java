package com.kresshy.weatherstation.bluetooth;

/**
 * Interface for managing Bluetooth hardware state and device discovery. Separates Android Bluetooth
 * APIs from the rest of the application logic.
 */
public interface WeatherBluetoothManager {
    /**
     * Starts scanning for nearby Bluetooth devices. This is necessary to find and connect to
     * available weather station hardware.
     */
    void startDiscovery();

    /**
     * Stops the active Bluetooth scan. This should be called to conserve battery once the desired
     * device is found or the operation is cancelled.
     */
    void stopDiscovery();

    /**
     * Requests to enable the Bluetooth adapter. This ensures the hardware is ready for discovery
     * and connection operations.
     */
    void enableBluetooth();

    /**
     * Disables the Bluetooth adapter. This is used to shut down Bluetooth hardware when it is no
     * longer needed by the application.
     */
    void disableBluetooth();

    /**
     * Checks if the Bluetooth adapter is currently enabled.
     *
     * @return true if Bluetooth is ON and available for operations.
     */
    boolean isBluetoothEnabled();

    /**
     * Provides an observable stream of the discovery status. This allows the UI to show progress
     * indicators during a scan.
     *
     * @return LiveData indicating if a scan is currently active.
     */
    androidx.lifecycle.LiveData<Boolean> isDiscovering();

    /**
     * Registers system broadcast receivers. This is required to receive updates about discovered
     * devices and adapter state changes.
     */
    void registerReceivers();

    /**
     * Unregisters system broadcast receivers. This must be called to prevent memory leaks and
     * unnecessary battery drain.
     */
    void unregisterReceivers();

    /**
     * Provides an observable list of discovered Bluetooth devices. This allows the UI to display a
     * dynamic list of nearby hardware.
     *
     * @return LiveData containing the list of devices found during the current scan.
     */
    androidx.lifecycle.LiveData<java.util.List<android.bluetooth.BluetoothDevice>>
            getDiscoveredDevices();

    /**
     * Provides a user-friendly status string for the current scan operation.
     *
     * @return LiveData containing a status description.
     */
    androidx.lifecycle.LiveData<String> getDiscoveryStatus();

    /**
     * Provides an observable stream of the adapter's power state (ON/OFF). This allows the app to
     * respond to hardware toggles.
     *
     * @return LiveData containing the adapter state constant.
     */
    androidx.lifecycle.LiveData<Integer> getBluetoothState();

    /**
     * Retrieves the signal strength for a specific device. This can be used to indicate proximity
     * or connection quality.
     *
     * @param address The device MAC address.
     * @return The last known RSSI value, or 0 if unavailable.
     */
    int getDeviceRssi(String address);

    /**
     * Initiates the pairing (bonding) process with a device. This is necessary for secure
     * communication with some hardware modules.
     *
     * @param device The device to pair with.
     */
    void pairDevice(android.bluetooth.BluetoothDevice device);

    /**
     * Supplies a PIN for an active pairing request. This is used to complete secure authentication
     * during the bonding process.
     *
     * @param device The device requesting the PIN.
     * @param pin The PIN code string.
     */
    void setPin(android.bluetooth.BluetoothDevice device, String pin);

    /**
     * Provides an observable event for incoming pairing requests. This allows the app to prompt the
     * user for PIN entry if required.
     *
     * @return LiveData emitting the device that requested pairing.
     */
    androidx.lifecycle.LiveData<android.bluetooth.BluetoothDevice> getPairingRequest();
}
