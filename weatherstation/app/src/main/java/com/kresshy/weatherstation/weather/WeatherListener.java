package com.kresshy.weatherstation.weather;

/**
 * Interface for components that wish to be notified of new weather measurements. Used for
 * broadcasting parsed data across the application.
 */
public interface WeatherListener {
    /**
     * Called when a single node's weather data is available.
     *
     * @param weatherData The parsed weather reading.
     */
    void weatherDataReceived(WeatherData weatherData);

    /**
     * Called when a full measurement batch (potentially multi-node) is available.
     *
     * @param measurement The full measurement container.
     */
    void measurementReceived(Measurement measurement);
}
