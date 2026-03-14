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
     * Provides an observable high-level UI status. This is used by UI components to show loading
     * indicators, success states, or error messages during connection operations.
     *
     * @return LiveData containing the UI status resource.
     */
    LiveData<Resource<Void>> getUiState();

    /**
     * Provides an observable connection state. This allows the application to respond to state
     * changes like "connected", "disconnected", or "connecting".
     *
     * @return LiveData containing the connection state.
     */
    LiveData<ConnectionState> getConnectionState();

    /**
     * Provides an observable name of the currently connected device. This is typically used to
     * update the UI title or status display.
     *
     * @return LiveData containing the device name string.
     */
    LiveData<String> getConnectedDeviceName();

    /**
     * Provides an observable discovery status. This informs the UI whether a Bluetooth scan is
     * currently in progress.
     *
     * @return LiveData indicating if discovery is active.
     */
    LiveData<Boolean> isDiscovering();

    /**
     * Provides an observable status string for the current scan. This is used to display a
     * human-readable description of the scanning process.
     *
     * @return LiveData containing the status string.
     */
    LiveData<String> getDiscoveryStatus();

    /**
     * Provides an observable adapter state. This is used to monitor if Bluetooth is being toggled
     * externally or via the app.
     *
     * @return LiveData containing the Bluetooth adapter state constant.
     */
    LiveData<Integer> getBluetoothState();

    /**
     * Provides an observable list of discovered (unpaired) devices. This is used to populate the
     * "Available Devices" section of the connection list.
     *
     * @return LiveData containing a list of discovered devices.
     */
    LiveData<List<Parcelable>> getDiscoveredDevices();

    /**
     * Retrieves a list of currently paired devices and compatible simulator devices. This is used
     * to populate the "Paired Devices" section of the list.
     *
     * @return A static list of paired hardware and virtual devices.
     */
    List<Parcelable> getPairedDevices();

    /**
     * Checks the current enablement state of the Bluetooth hardware.
     *
     * @return true if Bluetooth is currently enabled and ready.
     */
    boolean isBluetoothEnabled();

    /**
     * Requests to enable the Bluetooth hardware. This ensures the system is ready for discovery and
     * connection.
     */
    void enableBluetooth();

    /**
     * Disables the Bluetooth hardware. This can be used to conserve power or as part of a clean
     * application exit.
     */
    void disableBluetooth();

    /**
     * Clears the current list of discovered devices. This is typically called before starting a
     * fresh scan.
     */
    void clearDiscoveredDevices();

    /**
     * Enables active connection management. This allows the controller to automatically handle
     * reconnections if enabled.
     */
    void startConnection();

    /**
     * Disables connection management. This stops any active hardware connection and prevents
     * automatic reconnection attempts.
     */
    void stopConnection();

    /** Starts searching for nearby weather station hardware. */
    void startDiscovery();

    /** Terminates any active Bluetooth device scan. */
    void stopDiscovery();

    /**
     * Attempts to establish a connection with the specified device.
     *
     * @param device The target device object.
     */
    void connectToDevice(Parcelable device);

    /**
     * Sets up system-level receivers for Bluetooth events. This is required for the controller to
     * remain synchronized with hardware state changes.
     */
    void registerReceivers();

    /** Unregisters system broadcast receivers to prevent memory leaks. */
    void unregisterReceivers();

    /**
     * Attempts to establish a connection to a specific device identified by its MAC address.
     *
     * @param address The MAC address of the device.
     */
    void connectToDeviceAddress(String address);

    /**
     * Initiates the standard Bluetooth pairing process.
     *
     * @param device The device to pair with.
     */
    void pairDevice(android.bluetooth.BluetoothDevice device);

    /**
     * Supplies a PIN code for an ongoing pairing request.
     *
     * @param device The device requesting the PIN.
     * @param pin The PIN code string.
     */
    void setPin(android.bluetooth.BluetoothDevice device, String pin);

    /**
     * Provides an observable event for incoming pairing requests from remote devices.
     *
     * @return LiveData emitting the device that requested pairing.
     */
    androidx.lifecycle.LiveData<android.bluetooth.BluetoothDevice> getPairingRequest();
}
