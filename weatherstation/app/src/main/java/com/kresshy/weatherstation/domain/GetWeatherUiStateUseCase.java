package com.kresshy.weatherstation.domain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.WeatherUiState;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * UseCase that aggregates multiple data streams from the repository into a single unified
 * {@link WeatherUiState}. This centralizes the logic for merging connection status, sensor data,
 * and thermal analysis results.
 */
@Singleton
public class GetWeatherUiStateUseCase {

    private final WeatherRepository repository;
    private final MediatorLiveData<WeatherUiState> uiState = new MediatorLiveData<>();

    @Inject
    public GetWeatherUiStateUseCase(WeatherRepository repository) {
        this.repository = repository;

        // Initialize with empty state
        uiState.setValue(WeatherUiState.empty());

        // Performance Optimization: Only trigger updateState when latestWeatherData changes.
        // Other fields change simultaneously with weather data in the repository,
        // so we can just pull their latest values once per weather message.
        uiState.addSource(repository.getLatestWeatherData(), data -> updateState());
        
        // Also update when connection state or launcher toggle changes, 
        // as these might happen independently of sensor data.
        uiState.addSource(repository.getConnectionState(), state -> updateState());
        uiState.addSource(repository.isLaunchDetectorEnabled(), enabled -> updateState());
    }

    /**
     * @return A LiveData stream of the unified UI state.
     */
    public LiveData<WeatherUiState> execute() {
        return uiState;
    }

    /** Merges all current values from the repository into a new immutable state object. */
    private void updateState() {
        WeatherUiState currentState = uiState.getValue();
        if (currentState == null) currentState = WeatherUiState.empty();

        uiState.setValue(new WeatherUiState(
                repository.getLatestWeatherData().getValue(),
                repository.getLaunchDecision().getValue(),
                repository.getTempTrend().getValue() != null ? repository.getTempTrend().getValue() : 0.0,
                repository.getWindTrend().getValue() != null ? repository.getWindTrend().getValue() : 0.0,
                repository.getThermalScore().getValue() != null ? repository.getThermalScore().getValue() : 0,
                repository.isLaunchDetectorEnabled().getValue() != null ? repository.isLaunchDetectorEnabled().getValue() : false,
                repository.getConnectionState().getValue(),
                repository.getConnectedDeviceName().getValue()
        ));
    }
}
