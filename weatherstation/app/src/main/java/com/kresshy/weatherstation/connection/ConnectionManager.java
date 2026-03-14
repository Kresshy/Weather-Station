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
     * Initializes the ConnectionManager with a specific connection implementation.
     *
     * @param connection Injected connection implementation, allowing for flexible switching between
     *     physical Bluetooth and simulation.
     */
    @Inject
    public ConnectionManager(Connection connection) {
        this.connection = connection;
    }

    /**
     * Starts the connection service. This is necessary to initialize the hardware link and prepare
     * it for discovery or connection attempts.
     */
    public void startConnection() {
        connection.start(listener);
    }

    /**
     * Shuts down the active connection. This should be called to release hardware resources and
     * stop background processing when the connection is no longer needed.
     */
    public void stopConnection() {
        connection.stop();
    }

    /**
     * Initiates a connection to a specific hardware or virtual device. This begins the handshake
     * process required to start receiving weather data.
     *
     * @param device The device descriptor containing the necessary addressing information.
     */
    public void connectToDevice(Parcelable device) {
        connection.connect(device, listener);
    }

    /**
     * Retrieves the current state of the underlying connection. This is used to determine if the
     * system is currently connected, connecting, or disconnected.
     *
     * @return The current state of the underlying connection.
     */
    public ConnectionState getConnectionState() {
        return connection.getState();
    }

    /**
     * Updates the listener used for receiving data and state updates. This ensures that the
     * appropriate component receives real-time updates from the hardware layer.
     *
     * @param listener The new listener implementation to be notified of hardware events.
     */
    public void setListener(HardwareEventListener listener) {
        this.listener = listener;
        if (connection != null) {
            connection.setCallback(listener);
        }
    }
}
