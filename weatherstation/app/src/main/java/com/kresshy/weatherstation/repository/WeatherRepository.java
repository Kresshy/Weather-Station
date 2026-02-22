package com.kresshy.weatherstation.repository;

import androidx.lifecycle.LiveData;

import com.kresshy.weatherstation.weather.WeatherData;

import java.util.List;

/**
 * Interface defining the data operations and connection management for the weather station. Acts as
 * the single source of truth for the application's weather data and hardware state.
 */
public interface WeatherRepository {

    /** SharedPreferences key for wind speed calibration offset. */
    String KEY_WIND_DIFF = "KEY_WIND_DIFF";

    /** SharedPreferences key for temperature calibration offset. */
    String KEY_TEMP_DIFF = "KEY_TEMP_DIFF";

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
     * @return Observable atomic update containing both raw data and calculated trends.
     */
    LiveData<com.kresshy.weatherstation.weather.ProcessedWeatherData> getProcessedWeatherData();

    /**
     * @return Observable launch decision.
     */
    LiveData<LaunchDecision> getLaunchDecision();

    /**
     * @return Observable temperature trend.
     */
    LiveData<Double> getTempTrend();

    /**
     * @return Observable wind speed trend.
     */
    LiveData<Double> getWindTrend();

    /**
     * @return Observable thermal score (0-100).
     */
    LiveData<Integer> getThermalScore();

    /**
     * @return Observable boolean for launch detector enabled state.
     */
    LiveData<Boolean> isLaunchDetectorEnabled();

    /**
     * @return List of historical weather data points for chart persistence.
     */
    List<WeatherData> getHistoricalWeatherData();

    /**
     * @return Observable toast messages.
     */
    LiveData<String> getToastMessage();

    /**
     * @return Observable debug log messages.
     */
    LiveData<String> getLogMessage();

    /**
     * Posts a toast message to the UI.
     *
     * @param message The message to display.
     */
    void onToastMessage(String message);

    /**
     * Posts a log message for debugging.
     *
     * @param message The log entry.
     */
    void onLogMessage(String message);
}
