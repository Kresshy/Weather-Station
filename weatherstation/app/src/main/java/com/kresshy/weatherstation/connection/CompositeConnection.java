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

        // 1. Check if we are already trying to connect to this EXACT device address
        if (currentDevice != null && isSameDevice(currentDevice, device)) {
            if (activeConnection != null
                    && activeConnection.getState() != ConnectionState.stopped) {
                Timber.d("Already connecting/connected to this device: %s. Ignoring.", device);
                return;
            }
        }

        Timber.d("connect() called with device: %s", device);
        currentDevice = device;

        Connection targetConnection = null;

        // 2. Identify the correct implementation
        if (device instanceof SimulatorDevice) {
            Timber.i("Routing to SimulatorConnection");
            targetConnection = simulatorConnection;
        } else if (device instanceof BluetoothDevice) {
            BluetoothDevice btDevice = (BluetoothDevice) device;
            int type = btDevice.getType();

            if (type == BluetoothDevice.DEVICE_TYPE_LE
                    || type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                Timber.i(
                        "Routing to BleConnection (Type: %d, Address: %s)",
                        type, btDevice.getAddress());
                targetConnection = bleConnection;
            } else {
                Timber.i(
                        "Routing to BluetoothConnection (Classic) (Type: %d, Address: %s)",
                        type, btDevice.getAddress());
                targetConnection = classicConnection;
            }
        }

        // 3. Only switch if the implementation changed
        if (targetConnection != null) {
            if (activeConnection != null && activeConnection != targetConnection) {
                Timber.d(
                        "Stopping existing connection before switching: %s -> %s",
                        activeConnection.getClass().getSimpleName(),
                        targetConnection.getClass().getSimpleName());
                activeConnection.stop();
            }
            activeConnection = targetConnection;
            activeConnection.connect(device, listener);
        } else {
            Timber.e("Unknown device type: %s", device.getClass().getName());
        }
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
