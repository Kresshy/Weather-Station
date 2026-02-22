package com.kresshy.weatherstation.fakes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.WeatherData;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of {@link WeatherRepository} for use in UI and integration tests. Provides
 * manual control methods to push specific data or states into the observers.
 */
public class FakeWeatherRepository implements WeatherRepository {

    private final MutableLiveData<com.kresshy.weatherstation.weather.ProcessedWeatherData>
            processedWeatherData = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> logMessage = new MutableLiveData<>();

    private final MutableLiveData<LaunchDecision> launchDecision =
            new MutableLiveData<>(LaunchDecision.WAITING);
    private final MutableLiveData<Double> tempTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> windTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> thermalScore = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> launchDetectorEnabled = new MutableLiveData<>(true);

    // --- Control Methods for Testing ---

    /** Manually updates the atomic heartbeat. */
    public void setProcessedWeatherData(
            com.kresshy.weatherstation.weather.ProcessedWeatherData data) {
        processedWeatherData.postValue(data);
    }

    /** Manually updates the launch decision. */
    public void setLaunchDecision(LaunchDecision decision) {
        launchDecision.postValue(decision);
    }

    /** Manually updates the launch detector enabled state. */
    public void setLaunchDetectorEnabled(boolean enabled) {
        launchDetectorEnabled.postValue(enabled);
    }

    // --- WeatherRepository Interface Implementation ---

    @Override
    public LiveData<com.kresshy.weatherstation.weather.ProcessedWeatherData>
            getProcessedWeatherData() {
        return processedWeatherData;
    }

    @Override
    public LiveData<LaunchDecision> getLaunchDecision() {
        return launchDecision;
    }

    @Override
    public LiveData<Double> getTempTrend() {
        return tempTrend;
    }

    @Override
    public LiveData<Double> getWindTrend() {
        return windTrend;
    }

    @Override
    public LiveData<Integer> getThermalScore() {
        return thermalScore;
    }

    @Override
    public LiveData<Boolean> isLaunchDetectorEnabled() {
        return launchDetectorEnabled;
    }

    @Override
    public List<WeatherData> getHistoricalWeatherData() {
        return new ArrayList<>();
    }

    @Override
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    @Override
    public LiveData<String> getLogMessage() {
        return logMessage;
    }

    @Override
    public void onToastMessage(String message) {
        toastMessage.postValue(message);
    }

    @Override
    public void onLogMessage(String message) {
        logMessage.postValue(message);
    }
}
