package com.kresshy.weatherstation.connection;

import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;

/**
 * Generic callback interface for connection-related events. Provides hooks for data reception,
 * connection lifecycle, and diagnostic messaging.
 */
public interface ConnectionCallback {
    /** Called when a single node's data is received. */
    void onWeatherDataReceived(WeatherData weatherData);

    /** Called when a multi-node measurement batch is received. */
    void onMeasurementReceived(Measurement measurement);

    /** Requests a message to be displayed to the user. */
    void onToastMessage(String message);

    /** Called when the connection state changes. */
    void onConnectionStateChange(ConnectionState state);

    /** Triggered when a connection attempt succeeds. */
    void onConnected();

    /** Sends a diagnostic log message. */
    void onLogMessage(String message);
}
