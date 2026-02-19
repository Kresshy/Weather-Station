package com.kresshy.weatherstation.repository;

import androidx.lifecycle.LiveData;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.util.Resource;
import com.kresshy.weatherstation.weather.WeatherData;

import java.util.List;

/**
 * Interface defining the data operations and connection management for the weather station. Acts as
 * the single source of truth for the application's weather data and hardware state.
 */
public interface WeatherRepository {

    /** Enum representing the possible air quality decisions for flight. */
    enum LaunchDecision {
        WAITING,
        POOR,
        POTENTIAL,
        LAUNCH
    }

    /**
     * @return Observable launch decision.
     */
    LiveData<LaunchDecision> getLaunchDecision();

    /**
     * @return Observable temperature trend.
     */
    LiveData<Double> getTempTrend();

    /**
     * @return Observable wind speed trend.
     */
    LiveData<Double> getWindTrend();

    /**
     * @return Observable thermal score (0-100).
     */
    LiveData<Integer> getThermalScore();

    /**
     * @return Observable high-level UI status (LOADING, SUCCESS, ERROR).
     */
    LiveData<Resource<Void>> getUiState();

    /**
     * @return Observable latest weather measurement.
     */
    LiveData<WeatherData> getLatestWeatherData();

    /**
     * @return Observable connection state.
     */
    LiveData<ConnectionState> getConnectionState();

    /**
     * @return Observable toast messages.
     */
    LiveData<String> getToastMessage();

    /**
     * @return Observable debug log messages.
     */
    LiveData<String> getLogMessage();

    /**
     * @return Observable discovery status (true if scanning).
     */
    LiveData<Boolean> isDiscovering();

    /**
     * @return Observable discovery status string.
     */
    LiveData<String> getDiscoveryStatus();

    /**
     * @return Observable list of paired Bluetooth devices.
     */
    LiveData<List<android.os.Parcelable>> getPairedDevices();

    /**
     * @return Observable list of discovered (unpaired) Bluetooth devices.
     */
    LiveData<List<android.os.Parcelable>> getDiscoveredDevices();

    /** Refreshes the list of paired devices. */
    void refreshPairedDevices();

    /** Clears the current discovery results. */
    void clearDiscoveredDevices();

    /** Enables background connection management. */
    void startConnection();

    /** Disables connection management and resets analysis. */
    void stopConnection();

    /** Starts a Bluetooth device scan. */
    void startDiscovery();

    /** Stops the Bluetooth device scan. */
    void stopDiscovery();

    /**
     * Connects to a specific device.
     *
     * @param device The BluetoothDevice or SimulatorDevice to connect to.
     */
    void connectToDevice(android.os.Parcelable device);

    /**
     * Connects to a device by its MAC address.
     *
     * @param address MAC address of the target device.
     */
    void connectToDeviceAddress(String address);

    /**
     * Posts a toast message to the UI.
     *
     * @param message The message to display.
     */
    void onToastMessage(String message);

    /**
     * Posts a log message for debugging.
     *
     * @param message The log entry.
     */
    void onLogMessage(String message);
}
