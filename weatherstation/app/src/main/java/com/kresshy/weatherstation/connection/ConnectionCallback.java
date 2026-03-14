package com.kresshy.weatherstation.connection;

import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;

/**
 * Generic callback interface for connection-related events. This interface allows components to
 * react to data reception, connection lifecycle changes, and diagnostic messages from the hardware
 * layer.
 */
public interface ConnectionCallback {
    /**
     * Called when a single node's data is received. This is used to update the UI with the latest
     * individual sensor readings.
     */
    void onWeatherDataReceived(WeatherData weatherData);

    /**
     * Called when a multi-node measurement batch is received. This is used for processing data from
     * multiple sensors simultaneously.
     */
    void onMeasurementReceived(Measurement measurement);

    /**
     * Requests a message to be displayed to the user via a toast. This provides feedback for
     * asynchronous events like connection failures or successful pairing.
     */
    void onToastMessage(String message);

    /**
     * Called when the connection state changes. This allows the UI to update its status indicators
     * (e.g., showing a loading spinner or a connected icon).
     */
    void onConnectionStateChange(ConnectionState state);

    /**
     * Triggered when a connection attempt succeeds. This signals that the data stream is ready to
     * be processed.
     */
    void onConnected();

    /**
     * Sends a diagnostic log message. This is used for debugging and monitoring the health of the
     * hardware connection.
     */
    void onLogMessage(String message);
}
