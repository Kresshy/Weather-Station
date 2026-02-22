package com.kresshy.weatherstation.connection;

import android.os.Parcelable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orchestrates the connection to the weather station hardware. Acts as a wrapper around the
 * specific {@link Connection} implementation (Bluetooth or Simulator).
 */
@Singleton
public class ConnectionManager {
    /** The active connection implementation. */
    public final Connection connection;

    private HardwareEventListener listener;

    /**
     * @param connection Injected connection implementation.
     */
    @Inject
    public ConnectionManager(Connection connection) {
        this.connection = connection;
    }

    /** Starts the connection service. */
    public void startConnection() {
        connection.start(listener);
    }

    /** Shuts down the active connection. */
    public void stopConnection() {
        connection.stop();
    }

    /**
     * Initiates a connection to a specific hardware or virtual device.
     *
     * @param device The device descriptor.
     */
    public void connectToDevice(Parcelable device) {
        connection.connect(device, listener);
    }

    /**
     * @return The current state of the underlying connection.
     */
    public ConnectionState getConnectionState() {
        return connection.getState();
    }

    /**
     * Updates the listener used for receiving data and state updates.
     *
     * @param listener The new listener implementation.
     */
    public void setListener(HardwareEventListener listener) {
        this.listener = listener;
        if (connection != null) {
            connection.setCallback(listener);
        }
    }
}
