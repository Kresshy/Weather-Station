package com.kresshy.weatherstation.weather;

import com.kresshy.weatherstation.repository.WeatherRepository;

/**
 * Encapsulates a complete weather measurement event, including raw data and all calculated
 * analytical trends/decisions. This ensures atomic updates across the application pipeline.
 */
public class ProcessedWeatherData {
    private final WeatherData weatherData;
    private final WeatherRepository.LaunchDecision launchDecision;
    private final double tempTrend;
    private final double windTrend;
    private final int thermalScore;

    /**
     * Initializes a new ProcessedWeatherData instance. This object bundles raw data with all
     * calculated analytical results to ensure atomic state updates.
     *
     * @param weatherData The raw sensor reading.
     * @param launchDecision The current suitability for launching.
     * @param tempTrend The calculated temperature trend.
     * @param windTrend The calculated wind speed trend.
     * @param thermalScore The 0-100 thermal rating.
     */
    public ProcessedWeatherData(
            WeatherData weatherData,
            WeatherRepository.LaunchDecision launchDecision,
            double tempTrend,
            double windTrend,
            int thermalScore) {
        this.weatherData = weatherData;
        this.launchDecision = launchDecision;
        this.tempTrend = tempTrend;
        this.windTrend = windTrend;
        this.thermalScore = thermalScore;
    }

    /**
     * @return The raw weather sensor data.
     */
    public WeatherData getWeatherData() {
        return weatherData;
    }

    /**
     * @return The current launch suitability decision.
     */
    public WeatherRepository.LaunchDecision getLaunchDecision() {
        return launchDecision;
    }

    /**
     * @return The calculated temperature trend (delta between fast and slow EMA).
     */
    public double getTempTrend() {
        return tempTrend;
    }

    /**
     * @return The calculated wind speed trend (delta between fast and slow EMA).
     */
    public double getWindTrend() {
        return windTrend;
    }

    /**
     * @return The 0-100 thermal suitability score.
     */
    public int getThermalScore() {
        return thermalScore;
    }
}
