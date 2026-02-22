package com.kresshy.weatherstation.repository;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.bluetooth.SimulatorDevice;
import com.kresshy.weatherstation.bluetooth.WeatherBluetoothManager;
import com.kresshy.weatherstation.connection.ConnectionManager;
import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.connection.RawDataCallback;
import com.kresshy.weatherstation.util.PermissionHelper;
import com.kresshy.weatherstation.util.Resource;
import com.kresshy.weatherstation.weather.ThermalAnalyzer;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherMessageParser;

import dagger.hilt.android.qualifiers.ApplicationContext;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link WeatherRepository} that manages the flow of weather data. It coordinates
 * parsing raw data strings, analyzing thermal trends, applying user-defined calibration offsets,
 * and managing hardware connection lifecycles (with auto-reconnect).
 */
@Singleton
public class WeatherRepositoryImpl implements WeatherRepository, RawDataCallback {

    private final Context context;
    private final ConnectionManager connectionManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final ThermalAnalyzer thermalAnalyzer;
    private final WeatherMessageParser messageParser;
    private final WeatherBluetoothManager bluetoothManager;
    private final SharedPreferences sharedPreferences;

    private final MutableLiveData<WeatherData> latestWeatherData = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> uiState = new MutableLiveData<>();
    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> logMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isDiscovering = new MutableLiveData<>();
    private final MutableLiveData<String> discoveryStatus = new MutableLiveData<>();
    private final MutableLiveData<List<Parcelable>> discoveredDevices =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<LaunchDecision> launchDecision =
            new MutableLiveData<>(LaunchDecision.WAITING);
    private final MutableLiveData<Double> tempTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> windTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> thermalScore = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> launchDetectorEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<String> connectedDeviceName = new MutableLiveData<>(null);
    private final List<WeatherData> historicalData = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 300;

    private double correctionWind = 0.0;
    private double correctionTemp = 0.0;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private Parcelable lastConnectedDevice;
    private boolean shouldReconnect = false;
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private long reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
    private static final long INITIAL_RECONNECT_DELAY_MS = 2000; // 2 seconds
    private static final long MAX_RECONNECT_DELAY_MS = 32000; // 32 seconds

    /** Primary constructor used by Hilt. */
    @Inject
    public WeatherRepositoryImpl(
            @ApplicationContext Context context,
            ThermalAnalyzer thermalAnalyzer,
            WeatherMessageParser messageParser,
            WeatherBluetoothManager bluetoothManager,
            SharedPreferences sharedPreferences,
            @Nullable BluetoothAdapter bluetoothAdapter,
            ConnectionManager connectionManager) {
        this.context = context;
        this.thermalAnalyzer = thermalAnalyzer;
        this.messageParser = messageParser;
        this.bluetoothManager = bluetoothManager;
        this.sharedPreferences = sharedPreferences;
        this.bluetoothAdapter = bluetoothAdapter;
        this.connectionManager = connectionManager;

        this.connectionManager.setCallback(this);

        loadCorrections(sharedPreferences);
        loadLaunchDetectorSettings(sharedPreferences);

        this.preferenceChangeListener =
                (prefs, key) -> {
                    if (KEY_WIND_DIFF.equals(key) || KEY_TEMP_DIFF.equals(key)) {
                        loadCorrections(prefs);
                    } else if (PREF_LAUNCH_DETECTOR_ENABLED.equals(key)
                            || PREF_LAUNCH_DETECTOR_SENSITIVITY.equals(key)) {
                        loadLaunchDetectorSettings(prefs);
                    }
                };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // Bridge BluetoothManager data to Repository LiveData
        bluetoothManager
                .getDiscoveredDevices()
                .observeForever(devices -> discoveredDevices.postValue(new ArrayList<>(devices)));
        bluetoothManager.getDiscoveryStatus().observeForever(discoveryStatus::postValue);

        // Set initial connection state
        connectionState.postValue(ConnectionState.stopped);
        isDiscovering.postValue(false);
    }

    private void loadCorrections(SharedPreferences sharedPreferences) {
        correctionWind = parseDoubleSafe(sharedPreferences.getString(KEY_WIND_DIFF, "0.0"), 0.0);
        correctionTemp = parseDoubleSafe(sharedPreferences.getString(KEY_TEMP_DIFF, "0.0"), 0.0);
        Timber.d("Loaded corrections - wind: %f, temp: %f", correctionWind, correctionTemp);
    }

    private void loadLaunchDetectorSettings(SharedPreferences sharedPreferences) {
        boolean enabled = sharedPreferences.getBoolean(PREF_LAUNCH_DETECTOR_ENABLED, false);
        double sensitivity =
                parseDoubleSafe(
                        sharedPreferences.getString(PREF_LAUNCH_DETECTOR_SENSITIVITY, "1.0"), 1.0);

        thermalAnalyzer.setEnabled(enabled);
        thermalAnalyzer.setSensitivity(sensitivity);
        launchDetectorEnabled.postValue(enabled);

        if (!enabled) {
            launchDecision.postValue(LaunchDecision.WAITING);
            thermalScore.postValue(0);
        }

        Timber.d(
                "Loaded Launch Detector Settings - enabled: %b, sensitivity: %.1f",
                enabled, sensitivity);
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            // Replace comma with dot to handle European locales correctly
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            Timber.w("Failed to parse double: %s, using default: %f", value, defaultValue);
            return defaultValue;
        }
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
    public LiveData<String> getConnectedDeviceName() {
        return connectedDeviceName;
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
    public List<WeatherData> getHistoricalWeatherData() {
        synchronized (historicalData) {
            return new ArrayList<>(historicalData);
        }
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
    public LiveData<Integer> getBluetoothState() {
        return bluetoothManager.getBluetoothState();
    }

    @Override
    public LiveData<List<Parcelable>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    @Override
    public void clearDiscoveredDevices() {
        discoveredDevices.postValue(new ArrayList<>());
    }

    // --- RawDataCallback Implementation ---

    private static final double MAX_TEMP_JUMP = 10.0; // Max physically possible jump in deg/sec
    private WeatherData lastSaneData = null;

    /**
     * Called when a raw string message is received from the hardware. Parses the message, applies
     * calibration, and triggers thermal analysis.
     *
     * @param data The raw string from the sensor.
     */
    @Override
    public void onRawDataReceived(String data) {
        WeatherData weatherData = messageParser.parse(data);

        if (weatherData != null) {
            // --- Layer 2 Outlier Rejection ---
            // Air temperature doesn't jump 10 degrees in a second. Discard glitches.
            if (lastSaneData != null) {
                double tempDelta =
                        Math.abs(weatherData.getTemperature() - lastSaneData.getTemperature());
                if (tempDelta > MAX_TEMP_JUMP) {
                    Timber.w("OUTLIER DETECTED: Discarding temp jump of %.2f", tempDelta);
                    return; // Reject this glitchy reading
                }
            }
            lastSaneData = weatherData;

            applyCorrections(weatherData);

            // Add the last known RSSI if available
            if (lastConnectedDevice != null) {
                String address = "";
                if (lastConnectedDevice instanceof BluetoothDevice) {
                    address = ((BluetoothDevice) lastConnectedDevice).getAddress();
                } else if (lastConnectedDevice instanceof SimulatorDevice) {
                    address = ((SimulatorDevice) lastConnectedDevice).getAddress();
                }
                int rssi = bluetoothManager.getDeviceRssi(address);
                weatherData.setRssi(rssi);
            }

            // Track historical data for chart persistence
            synchronized (historicalData) {
                historicalData.add(weatherData);
                if (historicalData.size() > MAX_HISTORY_SIZE) {
                    historicalData.remove(0);
                }
            }

            ThermalAnalyzer.AnalysisResult result = thermalAnalyzer.analyze(weatherData);
            launchDecision.postValue(result.decision);
            tempTrend.postValue(result.tempTrend);
            windTrend.postValue(result.windTrend);
            thermalScore.postValue(result.score);

            latestWeatherData.postValue(weatherData);
        }
    }

    private void applyCorrections(WeatherData data) {
        data.setWindSpeed(data.getWindSpeed() + correctionWind);
        data.setTemperature(data.getTemperature() + correctionTemp);
    }

    /**
     * Called when the underlying hardware connection state changes. Triggers UI state updates and
     * manages the auto-reconnect logic.
     *
     * @param state The new {@link ConnectionState}.
     */
    @Override
    public void onConnectionStateChange(ConnectionState state) {
        connectionState.postValue(state);
        switch (state) {
            case connecting:
                uiState.postValue(Resource.loading(null));
                break;
            case connected:
                onConnected();
                break;
            case disconnected:
            case stopped:
                uiState.postValue(Resource.error("Disconnected", null));
                connectedDeviceName.postValue(null);
                if (shouldReconnect && lastConnectedDevice != null) {
                    scheduleReconnect();
                }
                break;
        }
    }

    /** Schedules a reconnection attempt with exponential backoff. */
    private void scheduleReconnect() {
        Timber.d("Scheduling reconnect in %d ms", reconnectDelayMs);
        reconnectExecutor.schedule(
                () -> {
                    if (shouldReconnect && lastConnectedDevice != null) {
                        String name = "Device";
                        if (lastConnectedDevice instanceof BluetoothDevice) {
                            if (PermissionHelper.hasConnectPermission(context)) {
                                name = ((BluetoothDevice) lastConnectedDevice).getName();
                            } else {
                                name = "Weather Station";
                            }
                        } else if (lastConnectedDevice instanceof SimulatorDevice) {
                            name = ((SimulatorDevice) lastConnectedDevice).getName();
                        }
                        Timber.d("Attempting auto-reconnect to: %s", name);
                        connectionManager.connectToDevice(lastConnectedDevice);
                    }
                },
                reconnectDelayMs,
                TimeUnit.MILLISECONDS);

        // Exponential backoff
        reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
    }

    @Override
    public void onConnected() {
        Timber.d("Connection established.");
        connectionState.postValue(ConnectionState.connected);
        uiState.postValue(Resource.success(null));
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;

        if (lastConnectedDevice != null) {
            if (lastConnectedDevice instanceof BluetoothDevice) {
                if (PermissionHelper.hasConnectPermission(context)) {
                    connectedDeviceName.postValue(((BluetoothDevice) lastConnectedDevice).getName());
                } else {
                    connectedDeviceName.postValue("Weather Station");
                }
            } else if (lastConnectedDevice instanceof SimulatorDevice) {
                connectedDeviceName.postValue(((SimulatorDevice) lastConnectedDevice).getName());
            }
        }
    }

    @Override
    public void onToastMessage(String message) {
        toastMessage.postValue(message);
        if (message.toLowerCase().contains("failed") || message.toLowerCase().contains("missing")) {
            uiState.postValue(Resource.error(message, null));
        }
    }

    @Override
    public void onLogMessage(String message) {
        logMessage.postValue(message);
    }

    // Proxy methods for ConnectionManager actions
    @Override
    public void startConnection() {
        shouldReconnect = true;
        connectionManager.startConnection();
    }

    @Override
    public void stopConnection() {
        shouldReconnect = false;
        thermalAnalyzer.reset();
        synchronized (historicalData) {
            historicalData.clear();
        }
        connectionManager.stopConnection();
    }

    @Override
    public void startDiscovery() {
        bluetoothManager.startDiscovery();
    }

    @Override
    public void stopDiscovery() {
        bluetoothManager.stopDiscovery();
    }

    @Override
    public void connectToDevice(Parcelable device) {
        lastConnectedDevice = device;
        shouldReconnect = true;
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
        connectionManager.connectToDevice(device);
    }
}
