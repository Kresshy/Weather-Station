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
    private final com.kresshy.weatherstation.bluetooth.WeatherConnectionController
            connectionController;
    private final com.kresshy.weatherstation.domain.GetWeatherUiStateUseCase
            getWeatherUiStateUseCase;
    private final com.kresshy.weatherstation.domain.ConnectToDeviceUseCase connectToDeviceUseCase;
    private final com.kresshy.weatherstation.domain.GetPairedDevicesUseCase getPairedDevicesUseCase;
    private final com.kresshy.weatherstation.domain.ManageDiscoveryUseCase manageDiscoveryUseCase;
    private final com.kresshy.weatherstation.domain.PairDeviceUseCase pairDeviceUseCase;

    private final androidx.lifecycle.MutableLiveData<List<android.os.Parcelable>> pairedDevices =
            new androidx.lifecycle.MutableLiveData<>(new java.util.ArrayList<>());

    /**
     * Initializes the ViewModel with its required dependencies. This ViewModel acts as the central
     * data hub for the dashboard UI.
     *
     * @param weatherRepository Data repository for weather measurements.
     * @param connectionController Controller for managing Bluetooth connections.
     * @param getWeatherUiStateUseCase Use case for aggregating UI state.
     * @param connectToDeviceUseCase Use case for initiating connections.
     * @param getPairedDevicesUseCase Use case for retrieving system-paired devices.
     * @param manageDiscoveryUseCase Use case for Bluetooth device discovery.
     * @param pairDeviceUseCase Use case for pairing new hardware.
     */
    @Inject
    public WeatherViewModel(
            WeatherRepository weatherRepository,
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController,
            com.kresshy.weatherstation.domain.GetWeatherUiStateUseCase getWeatherUiStateUseCase,
            com.kresshy.weatherstation.domain.ConnectToDeviceUseCase connectToDeviceUseCase,
            com.kresshy.weatherstation.domain.GetPairedDevicesUseCase getPairedDevicesUseCase,
            com.kresshy.weatherstation.domain.ManageDiscoveryUseCase manageDiscoveryUseCase,
            com.kresshy.weatherstation.domain.PairDeviceUseCase pairDeviceUseCase) {
        this.weatherRepository = weatherRepository;
        this.connectionController = connectionController;
        this.getWeatherUiStateUseCase = getWeatherUiStateUseCase;
        this.connectToDeviceUseCase = connectToDeviceUseCase;
        this.getPairedDevicesUseCase = getPairedDevicesUseCase;
        this.manageDiscoveryUseCase = manageDiscoveryUseCase;
        this.pairDeviceUseCase = pairDeviceUseCase;
    }

    /**
     * Provides an observable stream of the unified UI state. Fragments should observe this to
     * update the entire dashboard at once.
     *
     * @return Observable unified UI state containing all dashboard data.
     */
    public LiveData<WeatherUiState> getWeatherUiState() {
        return getWeatherUiStateUseCase.execute();
    }

    /**
     * Provides an observable list of devices currently paired with the Android system.
     *
     * @return Observable list of paired Bluetooth devices.
     */
    public LiveData<List<android.os.Parcelable>> getPairedDevices() {
        return pairedDevices;
    }

    /**
     * Provides an observable list of devices found during an active Bluetooth scan.
     *
     * @return List of discovered (unpaired) Bluetooth devices.
     */
    public LiveData<List<android.os.Parcelable>> getDiscoveredDevices() {
        return connectionController.getDiscoveredDevices();
    }

    /**
     * Triggers a refresh of the paired devices list from the Android system. This should be called
     * when the device selection UI is opened.
     */
    public void refreshPairedDevices() {
        pairedDevices.setValue(getPairedDevicesUseCase.execute());
    }

    /**
     * Clears any results found during the last Bluetooth scan. Used to reset the UI before a new
     * search.
     */
    public void clearDiscoveredDevices() {
        manageDiscoveryUseCase.clearResults();
    }

    /**
     * Starts a new Bluetooth discovery scan. This is needed to find weather stations that are in
     * pairing mode but not yet known to the system.
     */
    public void startDiscovery() {
        manageDiscoveryUseCase.startDiscovery();
    }

    /** Stops the currently active Bluetooth scan to conserve power and bandwidth. */
    public void stopDiscovery() {
        manageDiscoveryUseCase.stopDiscovery();
    }

    /**
     * Attempts to connect to a specific Bluetooth device by its MAC address. This initiates the
     * data link required for real-time monitoring.
     *
     * @param address The MAC address of the weather station.
     */
    public void connectToDeviceAddress(String address) {
        connectToDeviceUseCase.execute(address);
    }

    /**
     * Provides an observable state for the connection process. Useful for showing loading spinners
     * or error dialogs during initial setup.
     *
     * @return Observable UI state (LOADING, SUCCESS, ERROR).
     */
    public LiveData<Resource<Void>> getUiState() {
        return connectionController.getUiState();
    }

    /**
     * Provides the current launch suitability decision.
     *
     * @return Observable launch decision based on current air quality.
     */
    public LiveData<WeatherRepository.LaunchDecision> getLaunchDecision() {
        return weatherRepository.getLaunchDecision();
    }

    /**
     * Provides the current temperature trend.
     *
     * @return Observable temperature trend.
     */
    public LiveData<Double> getTempTrend() {
        return weatherRepository.getTempTrend();
    }

    /**
     * Provides the current wind speed trend.
     *
     * @return Observable wind speed trend.
     */
    public LiveData<Double> getWindTrend() {
        return weatherRepository.getWindTrend();
    }

    /**
     * Provides the current thermal suitability score.
     *
     * @return Observable 0-100 thermal suitability score.
     */
    public LiveData<Integer> getThermalScore() {
        return weatherRepository.getThermalScore();
    }

    /**
     * Checks if the analytical engine for thermal detection is enabled.
     *
     * @return Observable boolean for launch detector enabled state.
     */
    public LiveData<Boolean> isLaunchDetectorEnabled() {
        return weatherRepository.isLaunchDetectorEnabled();
    }

    /**
     * Provides the current Bluetooth connection state.
     *
     * @return Observable connection state (connected, disconnected, etc.).
     */
    public LiveData<ConnectionState> getConnectionState() {
        return connectionController.getConnectionState();
    }

    /**
     * Retrieves historical weather data for graph visualization.
     *
     * @return List of historical weather data points for chart persistence.
     */
    public List<WeatherData> getHistoricalWeatherData() {
        return weatherRepository.getHistoricalWeatherData();
    }

    /**
     * Provides an observable stream for transient UI notifications.
     *
     * @return Observable toast messages for UI notifications.
     */
    public LiveData<String> getToastMessage() {
        return weatherRepository.getToastMessage();
    }

    /**
     * Provides an observable stream for debug logging.
     *
     * @return Observable log messages for debugging.
     */
    public LiveData<String> getLogMessage() {
        return weatherRepository.getLogMessage();
    }

    /**
     * Checks if a Bluetooth scan is currently in progress.
     *
     * @return true if Bluetooth discovery is currently running.
     */
    public LiveData<Boolean> isDiscovering() {
        return connectionController.isDiscovering();
    }

    /**
     * Provides a status string explaining the current discovery phase.
     *
     * @return The current discovery status message (e.g., "Searching...").
     */
    public LiveData<String> getDiscoveryStatus() {
        return connectionController.getDiscoveryStatus();
    }

    /**
     * Provides the current state of the Bluetooth hardware.
     *
     * @return Observable Bluetooth adapter state (ON/OFF).
     */
    public LiveData<Integer> getBluetoothState() {
        return connectionController.getBluetoothState();
    }

    /**
     * Provides the name of the currently connected weather station.
     *
     * @return Observable currently connected device name.
     */
    public LiveData<String> getConnectedDeviceName() {
        return connectionController.getConnectedDeviceName();
    }

    /**
     * Initiates a pairing request with a specific Bluetooth device.
     *
     * @param device The device to pair with.
     */
    public void pairDevice(android.bluetooth.BluetoothDevice device) {
        pairDeviceUseCase.execute(device);
    }

    /**
     * Supplies a PIN code to respond to a pairing challenge.
     *
     * @param device The device requesting the PIN.
     * @param pin The PIN code.
     */
    public void setPin(android.bluetooth.BluetoothDevice device, String pin) {
        connectionController.setPin(device, pin);
    }

    /**
     * Provides an observable stream of incoming pairing requests.
     *
     * @return Observable event when a pairing request is received.
     */
    public LiveData<android.bluetooth.BluetoothDevice> getPairingRequest() {
        return connectionController.getPairingRequest();
    }
}
