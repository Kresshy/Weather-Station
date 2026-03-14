package com.kresshy.weatherstation.connection;

/**
 * Listener interface for hardware-level events. This interface defines the contract for handling
 * raw data packets, connection state transitions, and system-level notifications from the hardware
 * link.
 *
 * <p>It is implemented by the data processing layer to receive raw strings and orchestrated by the
 * control layer to manage the hardware lifecycle.
 */
public interface HardwareEventListener {
    /**
     * Called when a complete raw string message is received from the hardware. This allows the
     * parser to transform the raw data into structured weather measurements.
     *
     * @param data The raw string payload (usually JSON or legacy format).
     */
    void onRawDataReceived(String data);

    /**
     * Called when the physical hardware connection state changes. This provides the necessary
     * signals to update the UI and internal state machines.
     *
     * @param state The new {@link ConnectionState}.
     */
    void onConnectionStateChange(ConnectionState state);

    /**
     * Triggered when a physical connection is successfully established. This indicates that the
     * hardware is ready for bidirectional communication.
     */
    void onConnected();

    /**
     * Requests a user-facing toast message. This is used to communicate important status updates or
     * errors directly to the user.
     *
     * @param message The message text to be displayed.
     */
    void onToastMessage(String message);

    /**
     * Sends a technical message to the system debug log. This facilitates troubleshooting and
     * performance monitoring of the hardware link.
     *
     * @param message The log entry text.
     */
    void onLogMessage(String message);
}
