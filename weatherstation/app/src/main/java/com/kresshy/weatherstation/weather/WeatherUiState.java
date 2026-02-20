package com.kresshy.weatherstation.weather;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.repository.WeatherRepository;

/**
 * Immutable data class representing the complete state of the weather dashboard.
 * Supports Uni-directional Data Flow (UDF) by providing a single source of truth for the UI.
 */
public class WeatherUiState {
    private final WeatherData latestData;
    private final WeatherRepository.LaunchDecision launchDecision;
    private final double tempTrend;
    private final double windTrend;
    private final int thermalScore;
    private final boolean launchDetectorEnabled;
    private final ConnectionState connectionState;
    private final String connectedDeviceName;

    public WeatherUiState(
            WeatherData latestData,
            WeatherRepository.LaunchDecision launchDecision,
            double tempTrend,
            double windTrend,
            int thermalScore,
            boolean launchDetectorEnabled,
            ConnectionState connectionState,
            String connectedDeviceName) {
        this.latestData = latestData;
        this.launchDecision = launchDecision;
        this.tempTrend = tempTrend;
        this.windTrend = windTrend;
        this.thermalScore = thermalScore;
        this.launchDetectorEnabled = launchDetectorEnabled;
        this.connectionState = connectionState;
        this.connectedDeviceName = connectedDeviceName;
    }

    // Getters
    public WeatherData getLatestData() { return latestData; }
    public WeatherRepository.LaunchDecision getLaunchDecision() { return launchDecision; }
    public double getTempTrend() { return tempTrend; }
    public double getWindTrend() { return windTrend; }
    public int getThermalScore() { return thermalScore; }
    public boolean isLaunchDetectorEnabled() { return launchDetectorEnabled; }
    public ConnectionState getConnectionState() { return connectionState; }
    public String getConnectedDeviceName() { return connectedDeviceName; }

    /** Creates an initial empty state. */
    public static WeatherUiState empty() {
        return new WeatherUiState(
                null,
                WeatherRepository.LaunchDecision.WAITING,
                0.0,
                0.0,
                0,
                false,
                ConnectionState.stopped,
                null
        );
    }
}
