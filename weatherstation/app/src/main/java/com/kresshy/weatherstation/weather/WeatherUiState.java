package com.kresshy.weatherstation.weather;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.repository.WeatherRepository;

/**
 * Immutable data class representing the complete state of the weather dashboard. Supports
 * Uni-directional Data Flow (UDF) by providing a single source of truth for the UI.
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

    /**
     * Initializes a new WeatherUiState. This object acts as a single source of truth for the entire
     * dashboard UI, ensuring consistency across all displayed components.
     *
     * @param latestData The most recent raw sensor reading.
     * @param launchDecision The current flight suitability decision.
     * @param tempTrend The calculated temperature trend.
     * @param windTrend The calculated wind speed trend.
     * @param thermalScore The 0-100 thermal score.
     * @param launchDetectorEnabled Whether the launch detector is active.
     * @param connectionState Current Bluetooth connection status.
     * @param connectedDeviceName The name of the connected weather station.
     */
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

    /**
     * @return The latest raw weather measurement.
     */
    public WeatherData getLatestData() {
        return latestData;
    }

    /**
     * @return The current launch suitability decision (e.g., LAUNCH, POOR).
     */
    public WeatherRepository.LaunchDecision getLaunchDecision() {
        return launchDecision;
    }

    /**
     * @return The calculated temperature trend.
     */
    public double getTempTrend() {
        return tempTrend;
    }

    /**
     * @return The calculated wind speed trend.
     */
    public double getWindTrend() {
        return windTrend;
    }

    /**
     * @return The 0-100 thermal suitability rating.
     */
    public int getThermalScore() {
        return thermalScore;
    }

    /**
     * @return true if the analytical engine is currently processing trends.
     */
    public boolean isLaunchDetectorEnabled() {
        return launchDetectorEnabled;
    }

    /**
     * @return The current hardware connection status.
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    }

    /**
     * @return The human-readable name of the connected device.
     */
    public String getConnectedDeviceName() {
        return connectedDeviceName;
    }

    /**
     * Creates an initial empty state with default values. Used when the application first starts or
     * when no device is connected.
     *
     * @return A new empty WeatherUiState.
     */
    public static WeatherUiState empty() {
        return new WeatherUiState(
                null,
                WeatherRepository.LaunchDecision.WAITING,
                0.0,
                0.0,
                0,
                false,
                ConnectionState.stopped,
                null);
    }
}
