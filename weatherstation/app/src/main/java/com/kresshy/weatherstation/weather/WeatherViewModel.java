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
    private final com.kresshy.weatherstation.domain.GetWeatherUiStateUseCase getWeatherUiStateUseCase;
    private final com.kresshy.weatherstation.domain.ConnectToDeviceUseCase connectToDeviceUseCase;
    private final com.kresshy.weatherstation.domain.GetPairedDevicesUseCase getPairedDevicesUseCase;
    private final com.kresshy.weatherstation.domain.ManageDiscoveryUseCase manageDiscoveryUseCase;
    private final com.kresshy.weatherstation.domain.UpdateCalibrationUseCase updateCalibrationUseCase;

    private final androidx.lifecycle.MutableLiveData<List<android.os.Parcelable>> pairedDevices = 
            new androidx.lifecycle.MutableLiveData<>(new java.util.ArrayList<>());

    @Inject
    public WeatherViewModel(
            WeatherRepository weatherRepository,
            com.kresshy.weatherstation.domain.GetWeatherUiStateUseCase getWeatherUiStateUseCase,
            com.kresshy.weatherstation.domain.ConnectToDeviceUseCase connectToDeviceUseCase,
            com.kresshy.weatherstation.domain.GetPairedDevicesUseCase getPairedDevicesUseCase,
            com.kresshy.weatherstation.domain.ManageDiscoveryUseCase manageDiscoveryUseCase,
            com.kresshy.weatherstation.domain.UpdateCalibrationUseCase updateCalibrationUseCase) {
        this.weatherRepository = weatherRepository;
        this.getWeatherUiStateUseCase = getWeatherUiStateUseCase;
        this.connectToDeviceUseCase = connectToDeviceUseCase;
        this.getPairedDevicesUseCase = getPairedDevicesUseCase;
        this.manageDiscoveryUseCase = manageDiscoveryUseCase;
        this.updateCalibrationUseCase = updateCalibrationUseCase;
    }

    /**
     * @return Observable unified UI state containing all dashboard data.
     */
    public LiveData<WeatherUiState> getWeatherUiState() {
        return getWeatherUiStateUseCase.execute();
    }

    /**
     * @return Observable list of paired Bluetooth devices.
     */
    public LiveData<List<android.os.Parcelable>> getPairedDevices() {
        return pairedDevices;
    }

    /**
     * @return List of discovered (unpaired) Bluetooth devices.
     */
    public LiveData<List<android.os.Parcelable>> getDiscoveredDevices() {
        return weatherRepository.getDiscoveredDevices();
    }

    /** Refreshes the list of paired devices. */
    public void refreshPairedDevices() {
        pairedDevices.setValue(getPairedDevicesUseCase.execute());
    }

    /** Clears the list of discovered devices. */
    public void clearDiscoveredDevices() {
        manageDiscoveryUseCase.clearResults();
    }

    /** Starts scanning for new Bluetooth devices. */
    public void startDiscovery() {
        manageDiscoveryUseCase.startDiscovery();
    }

    /** Stops the active Bluetooth scan. */
    public void stopDiscovery() {
        manageDiscoveryUseCase.stopDiscovery();
    }

    /**
     * Attempts to connect to a specific Bluetooth device by its MAC address.
     *
     * @param address The MAC address of the weather station.
     */
    public void connectToDeviceAddress(String address) {
        connectToDeviceUseCase.execute(address);
    }

    /**
     * Persists new calibration offsets for wind and temperature.
     *
     * @param windOffset The wind speed offset (m/s).
     * @param tempOffset The temperature offset (°C).
     */
    public void updateCalibration(String windOffset, String tempOffset) {
        updateCalibrationUseCase.execute(windOffset, tempOffset);
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
     * @return Observable boolean for launch detector enabled state.
     */
    public LiveData<Boolean> isLaunchDetectorEnabled() {
        return weatherRepository.isLaunchDetectorEnabled();
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
     * @return List of historical weather data points for chart persistence.
     */
    public List<WeatherData> getHistoricalWeatherData() {
        return weatherRepository.getHistoricalWeatherData();
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
     * @return Observable currently connected device name.
     */
    public LiveData<String> getConnectedDeviceName() {
        return weatherRepository.getConnectedDeviceName();
    }
}
