package com.kresshy.weatherstation.connection;

/**
 * Interface used by connection implementations to report events back to the manager or repository.
 * Handles raw data packets, connection state changes, and user-facing messages.
 */
public interface RawDataCallback {
    /**
     * Called when a complete raw string message is received from the hardware.
     *
     * @param data The raw string payload.
     */
    void onRawDataReceived(String data);

    /**
     * Called when the connection state (e.g., connected, disconnected) changes.
     *
     * @param state The new state.
     */
    void onConnectionStateChange(ConnectionState state);

    /** Triggered when a connection is successfully established. */
    void onConnected();

    /**
     * Requests a toast message to be displayed on the UI.
     *
     * @param message The message text.
     */
    void onToastMessage(String message);

    /**
     * Sends a message to the debug log system.
     *
     * @param message The log entry text.
     */
    void onLogMessage(String message);
}
