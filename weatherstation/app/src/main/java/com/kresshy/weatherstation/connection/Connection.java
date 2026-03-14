package com.kresshy.weatherstation.connection;

import android.os.Parcelable;

/** Interface representing a generic connection to a weather station (hardware or simulator). */
public interface Connection {
    /**
     * Prepares the connection service. This is required to initialize the hardware or simulator
     * environment before any connection attempts are made.
     *
     * @param listener The listener to receive state and data updates.
     */
    void start(HardwareEventListener listener);

    /**
     * Attempts to establish a connection to a specific hardware or virtual device. This is the
     * entry point for starting active communication with a weather station.
     *
     * @param device The target device object.
     * @param listener The listener to receive data and state updates for this specific connection.
     */
    void connect(Parcelable device, HardwareEventListener listener);

    /**
     * Gracefully shuts down the connection and releases all associated hardware and network
     * resources. This should be called when communication is no longer required to conserve power.
     */
    void stop();

    /**
     * Transmits raw data bytes to the weather station. This is used for sending commands or
     * requests to the station hardware.
     *
     * @param out The data payload to transmit.
     */
    void write(byte[] out);

    /**
     * Retrieves the current status of the connection. This allows the application to remain
     * synchronized with the underlying hardware state.
     *
     * @return The current ConnectionState.
     */
    ConnectionState getState();

    /**
     * Updates the active data and state listener. This is used to re-route hardware events when the
     * application context or UI state changes.
     *
     * @param listener The new listener instance.
     */
    void setCallback(HardwareEventListener listener);
}
