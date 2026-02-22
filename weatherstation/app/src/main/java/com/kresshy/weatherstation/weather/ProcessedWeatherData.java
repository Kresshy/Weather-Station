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

    public WeatherData getWeatherData() {
        return weatherData;
    }

    public WeatherRepository.LaunchDecision getLaunchDecision() {
        return launchDecision;
    }

    public double getTempTrend() {
        return tempTrend;
    }

    public double getWindTrend() {
        return windTrend;
    }

    public int getThermalScore() {
        return thermalScore;
    }
}
