package com.kresshy.weatherstation.connection;

import android.content.Context;
import android.os.Parcelable;

/**
 * Orchestrates the connection to the weather station hardware. Acts as a wrapper around the
 * specific {@link Connection} implementation (Bluetooth or Simulator).
 */
public class ConnectionManager {
    /** The active connection implementation. */
    public Connection connection;

    private RawDataCallback callback;

    /**
     * @param context Application context.
     * @param callback Initial callback for data and state events.
     */
    public ConnectionManager(Context context, RawDataCallback callback) {
        this.callback = callback;
        this.connection = ConnectionFactory.getConnection(context, callback);
    }

    /** Starts the connection service. */
    public void startConnection() {
        connection.start(callback);
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
        connection.connect(device, callback);
    }

    /**
     * @return The current state of the underlying connection.
     */
    public ConnectionState getConnectionState() {
        return connection.getState();
    }

    /**
     * Updates the callback used for receiving data and state updates. Useful when the repository or
     * service instance changes.
     *
     * @param callback The new callback implementation.
     */
    public void setCallback(RawDataCallback callback) {
        this.callback = callback;
        if (connection != null) {
            connection.setCallback(callback);
        }
    }
}
