package com.kresshy.weatherstation.domain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.WeatherUiState;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * UseCase that aggregates multiple data streams into a single unified {@link WeatherUiState}. This
 * component centralizes the complex logic of merging raw sensor readings, connection status, and
 * analytical results into a format optimized for UI consumption.
 */
@Singleton
public class GetWeatherUiStateUseCase {

    private final WeatherRepository repository;
    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;
    private final MediatorLiveData<WeatherUiState> uiState = new MediatorLiveData<>();

    /**
     * Initializes the UseCase and sets up the reactive data streams. It orchestrates multiple
     * LiveData sources to ensure the UI state is always consistent and reflects the latest hardware
     * and analysis updates.
     *
     * @param repository The repository providing raw and processed weather data.
     * @param connectionController The controller providing connection state and device details.
     */
    @Inject
    public GetWeatherUiStateUseCase(
            WeatherRepository repository,
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController) {
        this.repository = repository;
        this.connectionController = connectionController;

        // Initialize with empty state
        uiState.setValue(WeatherUiState.empty());

        // Single Source of Truth: Only update state when a fully processed heartbeat arrives.
        uiState.addSource(repository.getProcessedWeatherData(), data -> updateState(data));

        // Also update for independent state changes from Control Plane
        uiState.addSource(connectionController.getConnectionState(), state -> updateState(null));
        uiState.addSource(connectionController.getConnectedDeviceName(), name -> updateState(null));

        // And toggle changes from Data Plane
        uiState.addSource(repository.isLaunchDetectorEnabled(), enabled -> updateState(null));
    }

    /**
     * Provides the unified UI state stream. This is used by the fragments to observe all relevant
     * weather and connection data through a single, stable interface.
     *
     * @return A LiveData stream of the unified {@link WeatherUiState}.
     */
    public LiveData<WeatherUiState> execute() {
        return uiState;
    }

    /**
     * Merges current values into a new immutable state object.
     *
     * @param heartbeat Optional fresh data from repository.
     */
    private void updateState(
            @androidx.annotation.Nullable com.kresshy.weatherstation.weather.ProcessedWeatherData heartbeat) {
        WeatherUiState currentState = uiState.getValue();
        if (currentState == null) currentState = WeatherUiState.empty();

        if (heartbeat != null) {
            // Atomic update from heartbeat
            uiState.setValue(
                    new WeatherUiState(
                            heartbeat.getWeatherData(),
                            heartbeat.getLaunchDecision(),
                            heartbeat.getTempTrend(),
                            heartbeat.getWindTrend(),
                            heartbeat.getThermalScore(),
                            repository.isLaunchDetectorEnabled().getValue() != null
                                    ? repository.isLaunchDetectorEnabled().getValue()
                                    : false,
                            connectionController.getConnectionState().getValue(),
                            connectionController.getConnectedDeviceName().getValue()));
        } else {
            // Refresh from repository for other state changes (connection, enabled toggle)
            WeatherUiState current = uiState.getValue();
            com.kresshy.weatherstation.weather.WeatherData latest =
                    (current != null) ? current.getLatestData() : null;

            uiState.setValue(
                    new WeatherUiState(
                            latest,
                            repository.getLaunchDecision().getValue(),
                            repository.getTempTrend().getValue() != null
                                    ? repository.getTempTrend().getValue()
                                    : 0.0,
                            repository.getWindTrend().getValue() != null
                                    ? repository.getWindTrend().getValue()
                                    : 0.0,
                            repository.getThermalScore().getValue() != null
                                    ? repository.getThermalScore().getValue()
                                    : 0,
                            repository.isLaunchDetectorEnabled().getValue() != null
                                    ? repository.isLaunchDetectorEnabled().getValue()
                                    : false,
                            connectionController.getConnectionState().getValue(),
                            connectionController.getConnectedDeviceName().getValue()));
        }
    }
}
