package com.kresshy.weatherstation.fakes;

import android.os.Parcelable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.util.Resource;
import com.kresshy.weatherstation.weather.WeatherData;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation of {@link WeatherRepository} for use in UI and integration tests. Provides
 * manual control methods to push specific data or states into the observers.
 */
public class FakeWeatherRepository implements WeatherRepository {

    private final MutableLiveData<WeatherData> latestWeatherData = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> uiState = new MutableLiveData<>();
    private final MutableLiveData<ConnectionState> connectionState =
            new MutableLiveData<>(ConnectionState.stopped);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> logMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isDiscovering = new MutableLiveData<>(false);
    private final MutableLiveData<String> discoveryStatus = new MutableLiveData<>("");
    private final MutableLiveData<List<Parcelable>> pairedDevices =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Parcelable>> discoveredDevices =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<LaunchDecision> launchDecision =
            new MutableLiveData<>(LaunchDecision.WAITING);
    private final MutableLiveData<Double> tempTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> windTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> thermalScore = new MutableLiveData<>(0);

    // --- Control Methods for Testing ---

    /** Manually updates the latest weather data. */
    public void setWeatherData(WeatherData data) {
        latestWeatherData.postValue(data);
    }

    /** Manually updates the UI state. */
    public void setUiState(Resource<Void> state) {
        uiState.postValue(state);
    }

    /** Manually updates the connection state. */
    public void setConnectionState(ConnectionState state) {
        connectionState.postValue(state);
    }

    /** Manually updates the launch decision. */
    public void setLaunchDecision(LaunchDecision decision) {
        launchDecision.postValue(decision);
    }

    // --- WeatherRepository Interface Implementation ---

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
    public LiveData<Resource<Void>> getUiState() {
        return uiState;
    }

    @Override
    public LiveData<WeatherData> getLatestWeatherData() {
        return latestWeatherData;
    }

    @Override
    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
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
    public LiveData<Boolean> isDiscovering() {
        return isDiscovering;
    }

    @Override
    public LiveData<String> getDiscoveryStatus() {
        return discoveryStatus;
    }

    @Override
    public LiveData<List<Parcelable>> getPairedDevices() {
        return pairedDevices;
    }

    @Override
    public LiveData<List<Parcelable>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    @Override
    public void refreshPairedDevices() {}

    @Override
    public void clearDiscoveredDevices() {}

    @Override
    public void startConnection() {
        connectionState.postValue(ConnectionState.connecting);
    }

    @Override
    public void stopConnection() {
        connectionState.postValue(ConnectionState.stopped);
    }

    @Override
    public void startDiscovery() {
        isDiscovering.postValue(true);
    }

    @Override
    public void stopDiscovery() {
        isDiscovering.postValue(false);
    }

    @Override
    public void connectToDevice(Parcelable device) {
        connectionState.postValue(ConnectionState.connecting);
    }

    @Override
    public void connectToDeviceAddress(String address) {
        connectionState.postValue(ConnectionState.connecting);
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
