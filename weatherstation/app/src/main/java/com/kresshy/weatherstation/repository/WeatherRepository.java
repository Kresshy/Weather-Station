package com.kresshy.weatherstation.repository;

import androidx.lifecycle.LiveData;

import com.kresshy.weatherstation.weather.WeatherData;

import java.util.List;

/**
 * Interface defining the data operations and connection management for the weather station. Acts as
 * the single source of truth for the application's weather data and hardware state.
 */
public interface WeatherRepository {

    /** SharedPreferences key for enabling/disabling the launch detector. */
    String PREF_LAUNCH_DETECTOR_ENABLED = "pref_launch_detector_enabled";

    /** SharedPreferences key for launch detector sensitivity. */
    String PREF_LAUNCH_DETECTOR_SENSITIVITY = "pref_launch_detector_sensitivity";

    /** Enum representing the possible air quality decisions for flight. */
    enum LaunchDecision {
        WAITING,
        POOR,
        POTENTIAL,
        LAUNCH
    }

    /**
     * Provides an observable stream of processed weather data. This is the primary way for UI
     * components to receive atomic updates containing both raw sensor data and calculated trends.
     *
     * @return Observable atomic update containing both raw data and calculated trends.
     */
    LiveData<com.kresshy.weatherstation.weather.ProcessedWeatherData> getProcessedWeatherData();

    /**
     * Provides an observable stream of the current launch decision. Used by the UI to show if
     * conditions are suitable for flight.
     *
     * @return Observable launch decision.
     */
    LiveData<LaunchDecision> getLaunchDecision();

    /**
     * Provides an observable stream of the temperature trend. Useful for displaying whether the
     * temperature is rising or falling.
     *
     * @return Observable temperature trend.
     */
    LiveData<Double> getTempTrend();

    /**
     * Provides an observable stream of the wind speed trend. Useful for detecting sudden gusts or
     * calming patterns.
     *
     * @return Observable wind speed trend.
     */
    LiveData<Double> getWindTrend();

    /**
     * Provides an observable stream of the thermal score. This score represents a 0-100 rating of
     * how good the current conditions are for thermals.
     *
     * @return Observable thermal score (0-100).
     */
    LiveData<Integer> getThermalScore();

    /**
     * Provides an observable stream of the launch detector's enabled state. This allows UI
     * components to react to settings changes.
     *
     * @return Observable boolean for launch detector enabled state.
     */
    LiveData<Boolean> isLaunchDetectorEnabled();

    /**
     * Provides an observable stream of the latest raw weather measurement.
     *
     * @return Observable latest weather measurement.
     */
    LiveData<WeatherData> getLatestWeatherData();

    /**
     * Provides a list of historical weather data points. This is used to populate charts when a
     * fragment is first created or re-attached.
     *
     * @return List of historical weather data points for chart persistence.
     */
    List<WeatherData> getHistoricalWeatherData();

    /**
     * Provides an observable stream of toast messages. Used to communicate transient errors or
     * status updates to the user.
     *
     * @return Observable toast messages.
     */
    LiveData<String> getToastMessage();

    /**
     * Provides an observable stream of debug log messages. Primarily used for displaying internal
     * logs in a specialized UI component.
     *
     * @return Observable debug log messages.
     */
    LiveData<String> getLogMessage();

    /**
     * Posts a toast message to the UI. This is called by internal components to trigger a visual
     * notification.
     *
     * @param message The message to display.
     */
    void onToastMessage(String message);

    /**
     * Posts a log message for debugging. This is called by internal components to record
     * significant events.
     *
     * @param message The log entry.
     */
    void onLogMessage(String message);
}
