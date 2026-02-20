package com.kresshy.weatherstation.connection;

import android.os.Parcelable;

/** Interface representing a generic connection to a weather station (hardware or simulator). */
public interface Connection {
    /**
     * Starts the connection service.
     *
     * @param callback Callback for state changes.
     */
    public void start(RawDataCallback callback);

    /**
     * Attempts to connect to a specific device.
     *
     * @param device The device to connect to.
     * @param callback Callback for data and state updates.
     */
    public void connect(Parcelable device, RawDataCallback callback);

    /** Shuts down the connection and cleans up resources. */
    public void stop();

    /**
     * Sends raw bytes to the weather station.
     *
     * @param out The data to send.
     */
    public void write(byte[] out);

    /**
     * @return Current connection state.
     */
    public ConnectionState getState();

    /**
     * Updates the active data/state callback.
     *
     * @param callback The new callback.
     */
    void setCallback(RawDataCallback callback);
}
