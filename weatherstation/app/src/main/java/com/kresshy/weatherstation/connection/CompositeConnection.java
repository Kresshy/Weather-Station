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

    @Inject
    public CompositeConnection(
            BluetoothConnection classicConnection,
            BleConnection bleConnection,
            SimulatorConnection simulatorConnection) {
        this.classicConnection = classicConnection;
        this.bleConnection = bleConnection;
        this.simulatorConnection = simulatorConnection;
    }

    @Override
    public void start(HardwareEventListener listener) {
        this.listener = listener;
        classicConnection.start(listener);
        bleConnection.start(listener);
        simulatorConnection.start(listener);
    }

    @Override
    public void connect(Parcelable device, HardwareEventListener listener) {
        this.listener = listener;
        Timber.d("connect() called with device: %s", device);

        // 1. Stop any existing connection
        if (activeConnection != null) {
            Timber.d(
                    "Stopping existing connection before switching: %s",
                    activeConnection.getClass().getSimpleName());
            activeConnection.stop();
        }

        // 2. Identify and delegate to the correct implementation
        if (device instanceof SimulatorDevice) {
            Timber.i("Routing to SimulatorConnection");
            activeConnection = simulatorConnection;
        } else if (device instanceof BluetoothDevice) {
            BluetoothDevice btDevice = (BluetoothDevice) device;
            int type = btDevice.getType();

            if (type == BluetoothDevice.DEVICE_TYPE_LE
                    || type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                Timber.i(
                        "Routing to BleConnection (Type: %d, Address: %s)",
                        type, btDevice.getAddress());
                activeConnection = bleConnection;
            } else {
                Timber.i(
                        "Routing to BluetoothConnection (Classic) (Type: %d, Address: %s)",
                        type, btDevice.getAddress());
                activeConnection = classicConnection;
            }
        }

        if (activeConnection != null) {
            activeConnection.connect(device, listener);
        } else {
            Timber.e("Unknown device type: %s", device.getClass().getName());
        }
    }

    @Override
    public void stop() {
        if (activeConnection != null) {
            Timber.d("Stopping active connection: %s", activeConnection.getClass().getSimpleName());
            activeConnection.stop();
            activeConnection = null;
        }
    }

    @Override
    public void write(byte[] out) {
        if (activeConnection != null) {
            activeConnection.write(out);
        }
    }

    @Override
    public ConnectionState getState() {
        return activeConnection != null ? activeConnection.getState() : ConnectionState.stopped;
    }

    @Override
    public void setCallback(HardwareEventListener listener) {
        this.listener = listener;
        classicConnection.setCallback(listener);
        bleConnection.setCallback(listener);
        simulatorConnection.setCallback(listener);
    }
}
