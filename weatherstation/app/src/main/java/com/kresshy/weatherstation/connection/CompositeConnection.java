package com.kresshy.weatherstation.connection;

import android.bluetooth.BluetoothDevice;
import android.os.Parcelable;

import com.kresshy.weatherstation.bluetooth.BleConnection;
import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.bluetooth.SimulatorDevice;

import timber.log.Timber;

import javax.inject.Inject;

/**
 * A composite connection that routes commands to the appropriate implementation (Classic BT, BLE,
 * or Simulator) based on the device type.
 */
public class CompositeConnection implements Connection {

    private final BluetoothConnection classicConnection;
    private final BleConnection bleConnection;
    private final SimulatorConnection simulatorConnection;

    private Connection activeConnection;
    private HardwareEventListener listener;
    private Parcelable currentDevice;

    /**
     * Constructs a new CompositeConnection.
     *
     * @param classicConnection The Bluetooth Classic implementation.
     * @param bleConnection The Bluetooth Low Energy implementation.
     * @param simulatorConnection The virtual station implementation.
     */
    @Inject
    public CompositeConnection(
            BluetoothConnection classicConnection,
            BleConnection bleConnection,
            SimulatorConnection simulatorConnection) {
        this.classicConnection = classicConnection;
        this.bleConnection = bleConnection;
        this.simulatorConnection = simulatorConnection;
    }

    /**
     * Initializes all underlying connection services.
     *
     * @param listener The listener to receive hardware events.
     */
    @Override
    public void start(HardwareEventListener listener) {
        this.listener = listener;
        classicConnection.start(listener);
        bleConnection.start(listener);
        simulatorConnection.start(listener);
    }

    /**
     * Routes a connection request to the appropriate implementation based on the provided device
     * type (Classic, BLE, or Simulator). Stops any currently active connection only if a switch in
     * the underlying implementation is required (e.g., from Classic to BLE). This prevents
     * reconnection loops when device metadata is updated during discovery.
     *
     * @param device The target device.
     * @param listener The listener to receive hardware events.
     */
    @Override
    public void connect(Parcelable device, HardwareEventListener listener) {
        this.listener = listener;

        Timber.d("connect() called with device: %s", device);
        Parcelable oldDevice = currentDevice;

        // 1. Resolve the correct implementation based on device type and persistence
        Connection targetConnection = resolveTargetConnection(device, oldDevice);
        if (targetConnection == null) {
            Timber.e("Unknown device type: %s", device.getClass().getName());
            return;
        }

        // 2. Perform Unified Handover / Redundancy Check
        if (activeConnection != null) {
            boolean isSameAddress = isSameDevice(oldDevice, device);
            boolean isSameDriver = (activeConnection == targetConnection);

            if (isSameAddress && isSameDriver) {
                ConnectionState state = activeConnection.getState();
                if (state == ConnectionState.connecting || state == ConnectionState.connected) {
                    Timber.d("Already %s to this device. Ignoring redundant update.", state);
                    return;
                }
            } else {
                // Address changed OR Driver changed (Upgrade/Switch)
                Timber.d(
                        "Stopping existing connection (Handover/Upgrade): %s -> %s",
                        activeConnection.getClass().getSimpleName(),
                        targetConnection.getClass().getSimpleName());
                activeConnection.stop();
            }
        }

        // 3. Start the target connection
        currentDevice = device;
        activeConnection = targetConnection;
        activeConnection.connect(device, listener);
    }

    private Connection resolveTargetConnection(Parcelable device, Parcelable oldDevice) {
        if (device instanceof SimulatorDevice) {
            return simulatorConnection;
        } else if (device instanceof BluetoothDevice) {
            BluetoothDevice btDevice = (BluetoothDevice) device;
            int type = btDevice.getType();

            if (type == BluetoothDevice.DEVICE_TYPE_LE
                    || type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                return bleConnection;
            } else if (type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                return classicConnection;
            } else {
                // Type is UNKNOWN (0) - Use persistence if address matches active session
                if (activeConnection != null
                        && activeConnection != simulatorConnection
                        && isSameDevice(oldDevice, device)) {
                    Timber.i(
                            "Device type is UNKNOWN for %s, persisting active %s driver.",
                            btDevice.getAddress(), activeConnection.getClass().getSimpleName());
                    return activeConnection;
                }
                // Default to Classic for unknown new devices
                return classicConnection;
            }
        }
        return null;
    }

    /** Terminates the currently active connection if one exists. */
    @Override
    public void stop() {
        if (activeConnection != null) {
            Timber.d("Stopping active connection: %s", activeConnection.getClass().getSimpleName());
            activeConnection.stop();
            activeConnection = null;
            currentDevice = null;
        }
    }

    private boolean isSameDevice(Parcelable d1, Parcelable d2) {
        if (d1 == null || d2 == null) return false;
        if (d1.getClass() != d2.getClass()) return false;

        if (d1 instanceof BluetoothDevice) {
            return ((BluetoothDevice) d1).getAddress().equals(((BluetoothDevice) d2).getAddress());
        } else if (d1 instanceof SimulatorDevice) {
            return ((SimulatorDevice) d1).getAddress().equals(((SimulatorDevice) d2).getAddress());
        }
        return false;
    }

    /**
     * Transmits data via the currently active connection implementation.
     *
     * @param out The data payload to send.
     */
    @Override
    public void write(byte[] out) {
        if (activeConnection != null) {
            activeConnection.write(out);
        }
    }

    /**
     * Provides the state of the currently active connection.
     *
     * @return The current ConnectionState, or ConnectionState.stopped if no connection is active.
     */
    @Override
    public ConnectionState getState() {
        return activeConnection != null ? activeConnection.getState() : ConnectionState.stopped;
    }

    /**
     * Updates the active event listener for all underlying connection implementations.
     *
     * @param listener The new listener.
     */
    @Override
    public void setCallback(HardwareEventListener listener) {
        this.listener = listener;
        classicConnection.setCallback(listener);
        bleConnection.setCallback(listener);
        simulatorConnection.setCallback(listener);
    }
}
