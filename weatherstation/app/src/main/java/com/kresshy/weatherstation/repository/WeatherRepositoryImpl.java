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
    private final MutableLiveData<List<Parcelable>> pairedDevices =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Parcelable>> discoveredDevices =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<LaunchDecision> launchDecision =
            new MutableLiveData<>(LaunchDecision.WAITING);
    private final MutableLiveData<Double> tempTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> windTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> thermalScore = new MutableLiveData<>(0);

    private double correctionWind = 0.0;
    private double correctionTemp = 0.0;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private BluetoothDevice lastConnectedDevice;
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

        this.preferenceChangeListener =
                (prefs, key) -> {
                    if (KEY_WIND_DIFF.equals(key) || KEY_TEMP_DIFF.equals(key)) {
                        loadCorrections(prefs);
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
        correctionWind = Double.parseDouble(sharedPreferences.getString(KEY_WIND_DIFF, "0.0"));
        correctionTemp = Double.parseDouble(sharedPreferences.getString(KEY_TEMP_DIFF, "0.0"));
        Timber.d("Loaded corrections - wind: %f, temp: %f", correctionWind, correctionTemp);
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
    public void refreshPairedDevices() {
        boolean useSimulator = sharedPreferences.getBoolean("pref_simulator_mode", false);

        List<Parcelable> devices = new ArrayList<>();

        if (useSimulator) {
            devices.add(
                    new SimulatorDevice("Simulator Station", SimulatorDevice.SIMULATOR_ADDRESS));
        }

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                devices.addAll(bondedDevices);
            } else {
                Timber.d("refreshPairedDevices: BLUETOOTH_CONNECT permission not granted");
            }
        }
        pairedDevices.postValue(devices);
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
                int rssi = bluetoothManager.getDeviceRssi(lastConnectedDevice.getAddress());
                weatherData.setRssi(rssi);
            }

            latestWeatherData.postValue(weatherData);

            ThermalAnalyzer.AnalysisResult result = thermalAnalyzer.analyze(weatherData);
            launchDecision.postValue(result.decision);
            tempTrend.postValue(result.tempTrend);
            windTrend.postValue(result.windTrend);
            thermalScore.postValue(result.score);
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
                uiState.postValue(Resource.success(null));
                reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
                break;
            case disconnected:
            case stopped:
                uiState.postValue(Resource.error("Disconnected", null));
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
                        Timber.d("Attempting auto-reconnect to: %s", lastConnectedDevice.getName());
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
        if (device instanceof BluetoothDevice) {
            lastConnectedDevice = (BluetoothDevice) device;
            shouldReconnect = true;
            reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
        }
        connectionManager.connectToDevice(device);
    }

    @Override
    public void connectToDeviceAddress(String address) {
        sharedPreferences
                .edit()
                .putString(context.getString(R.string.PREFERENCE_DEVICE_ADDRESS), address)
                .apply();

        if (address.equals(SimulatorDevice.SIMULATOR_ADDRESS)
                && sharedPreferences.getBoolean("pref_simulator_mode", false)) {
            connectToDevice(new SimulatorDevice("Simulator Station", address));
        } else if (bluetoothAdapter != null) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            connectToDevice(device);
        } else {
            Timber.e("Cannot connect: BluetoothAdapter is null");
        }
    }
}
