package com.kresshy.weatherstation.weather;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.util.Resource;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.util.List;

import javax.inject.Inject;

/**
 * ViewModel responsible for providing weather data and connection status to the UI. Acts as a
 * bridge between the Fragments and the WeatherRepository.
 */
@HiltViewModel
public class WeatherViewModel extends ViewModel {
    private final WeatherRepository weatherRepository;

    @Inject
    public WeatherViewModel(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    /**
     * @return Observable UI state (LOADING, SUCCESS, ERROR).
     */
    public LiveData<Resource<Void>> getUiState() {
        return weatherRepository.getUiState();
    }

    /**
     * @return Observable launch decision based on current air quality.
     */
    public LiveData<WeatherRepository.LaunchDecision> getLaunchDecision() {
        return weatherRepository.getLaunchDecision();
    }

    /**
     * @return Observable temperature trend.
     */
    public LiveData<Double> getTempTrend() {
        return weatherRepository.getTempTrend();
    }

    /**
     * @return Observable wind speed trend.
     */
    public LiveData<Double> getWindTrend() {
        return weatherRepository.getWindTrend();
    }

    /**
     * @return Observable 0-100 thermal suitability score.
     */
    public LiveData<Integer> getThermalScore() {
        return weatherRepository.getThermalScore();
    }

    /**
     * @return Observable connection state (connected, disconnected, etc.).
     */
    public LiveData<ConnectionState> getConnectionState() {
        return weatherRepository.getConnectionState();
    }

    /**
     * @return Observable latest weather data point.
     */
    public LiveData<WeatherData> getLatestWeatherData() {
        return weatherRepository.getLatestWeatherData();
    }

    /**
     * @return Observable toast messages for UI notifications.
     */
    public LiveData<String> getToastMessage() {
        return weatherRepository.getToastMessage();
    }

    /**
     * @return Observable log messages for debugging.
     */
    public LiveData<String> getLogMessage() {
        return weatherRepository.getLogMessage();
    }

    /**
     * @return true if Bluetooth discovery is currently running.
     */
    public LiveData<Boolean> isDiscovering() {
        return weatherRepository.isDiscovering();
    }

    /**
     * @return The current discovery status message (e.g., "Searching...").
     */
    public LiveData<String> getDiscoveryStatus() {
        return weatherRepository.getDiscoveryStatus();
    }

    /**
     * @return Observable Bluetooth adapter state (ON/OFF).
     */
    public LiveData<Integer> getBluetoothState() {
        return weatherRepository.getBluetoothState();
    }

    /**
     * @return List of previously paired Bluetooth devices.
     */
    public LiveData<List<android.os.Parcelable>> getPairedDevices() {
        return weatherRepository.getPairedDevices();
    }

    /**
     * @return List of discovered (unpaired) Bluetooth devices.
     */
    public LiveData<List<android.os.Parcelable>> getDiscoveredDevices() {
        return weatherRepository.getDiscoveredDevices();
    }

    /** Refreshes the list of paired devices from the Bluetooth adapter. */
    public void refreshPairedDevices() {
        weatherRepository.refreshPairedDevices();
    }

    /** Clears the list of discovered devices. */
    public void clearDiscoveredDevices() {
        weatherRepository.clearDiscoveredDevices();
    }

    /** Starts scanning for new Bluetooth devices. */
    public void startDiscovery() {
        weatherRepository.startDiscovery();
    }

    /** Stops the active Bluetooth scan. */
    public void stopDiscovery() {
        weatherRepository.stopDiscovery();
    }

    /**
     * Attempts to connect to a specific Bluetooth device by its MAC address.
     *
     * @param address The MAC address of the weather station.
     */
    public void connectToDeviceAddress(String address) {
        weatherRepository.connectToDeviceAddress(address);
    }
}
