package com.kresshy.weatherstation.connection;

/**
 * Listener interface for hardware-level events. Handles raw data packets, connection state
 * transitions, and system-level notifications from the hardware link.
 *
 * <p>Implemented by the Data Plane (to receive strings) and orchestrated by the Control Plane (to
 * manage lifecycle).
 */
public interface HardwareEventListener {
    /**
     * Called when a complete raw string message is received from the hardware.
     *
     * @param data The raw string payload (usually JSON or legacy format).
     */
    void onRawDataReceived(String data);

    /**
     * Called when the physical hardware connection state changes.
     *
     * @param state The new {@link ConnectionState}.
     */
    void onConnectionStateChange(ConnectionState state);

    /** Triggered when a physical connection is successfully established. */
    void onConnected();

    /**
     * Requests a user-facing toast message.
     *
     * @param message The message text.
     */
    void onToastMessage(String message);

    /**
     * Sends a technical message to the system debug log.
     *
     * @param message The log entry text.
     */
    void onLogMessage(String message);
}
