package com.kresshy.weatherstation.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.connection.HardwareEventListener;
import com.kresshy.weatherstation.weather.ThermalAnalyzer;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherMessageParser;

import dagger.hilt.android.qualifiers.ApplicationContext;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link WeatherRepository} that manages the flow of weather data. It coordinates
 * parsing raw data strings, analyzing thermal trends, applying user-defined calibration offsets,
 * and managing hardware connection lifecycles (with auto-reconnect).
 */
@Singleton
public class WeatherRepositoryImpl implements WeatherRepository, HardwareEventListener {

    private final Context context;
    private final ThermalAnalyzer thermalAnalyzer;
    private final WeatherMessageParser messageParser;
    private final SharedPreferences sharedPreferences;

    private final MutableLiveData<com.kresshy.weatherstation.weather.ProcessedWeatherData>
            processedWeatherData = new MutableLiveData<>();
    private final MutableLiveData<WeatherData> latestWeatherData = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> logMessage = new MutableLiveData<>();

    private final MutableLiveData<LaunchDecision> launchDecision =
            new MutableLiveData<>(LaunchDecision.WAITING);
    private final MutableLiveData<Double> tempTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> windTrend = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> thermalScore = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> launchDetectorEnabled = new MutableLiveData<>(false);
    private final List<WeatherData> historicalData = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 300;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private static final double MAX_TEMP_JUMP = 10.0; // Max physically possible jump in deg/sec
    private WeatherData lastSaneData = null;

    /**
     * Initializes the WeatherRepository implementation. Connects to the hardware controller, loads
     * initial settings, and sets up preference listeners for real-time configuration updates.
     *
     * @param context Application context.
     * @param thermalAnalyzer Component for calculating thermal trends.
     * @param messageParser Component for parsing raw sensor messages.
     * @param sharedPreferences Persistent storage for user settings.
     * @param connectionController Component managing the Bluetooth connection.
     */
    @Inject
    public WeatherRepositoryImpl(
            @ApplicationContext Context context,
            ThermalAnalyzer thermalAnalyzer,
            WeatherMessageParser messageParser,
            SharedPreferences sharedPreferences,
            com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController) {
        this.context = context;
        this.thermalAnalyzer = thermalAnalyzer;
        this.messageParser = messageParser;
        this.sharedPreferences = sharedPreferences;

        // Bridge with the Control Plane
        if (connectionController
                instanceof com.kresshy.weatherstation.bluetooth.WeatherConnectionControllerImpl) {
            ((com.kresshy.weatherstation.bluetooth.WeatherConnectionControllerImpl)
                            connectionController)
                    .setHardwareEventListener(this);
        }

        loadLaunchDetectorSettings(sharedPreferences);

        this.preferenceChangeListener =
                (prefs, key) -> {
                    if (PREF_LAUNCH_DETECTOR_ENABLED.equals(key)
                            || PREF_LAUNCH_DETECTOR_SENSITIVITY.equals(key)) {
                        loadLaunchDetectorSettings(prefs);
                    }
                };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @VisibleForTesting
    void loadLaunchDetectorSettings(SharedPreferences sharedPreferences) {
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

    @VisibleForTesting
    double parseDoubleSafe(String value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            // Replace comma with dot to handle European locales correctly
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            Timber.w("Failed to parse double: %s, using default: %f", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Returns an observable stream of processed weather data.
     *
     * @return A LiveData containing processed data and trends.
     */
    @Override
    public LiveData<com.kresshy.weatherstation.weather.ProcessedWeatherData>
            getProcessedWeatherData() {
        return processedWeatherData;
    }

    /**
     * Returns an observable stream of the launch decision.
     *
     * @return A LiveData containing the launch decision.
     */
    @Override
    public LiveData<LaunchDecision> getLaunchDecision() {
        return launchDecision;
    }

    /**
     * Returns an observable stream of the temperature trend.
     *
     * @return A LiveData containing the temperature trend.
     */
    @Override
    public LiveData<Double> getTempTrend() {
        return tempTrend;
    }

    /**
     * Returns an observable stream of the wind speed trend.
     *
     * @return A LiveData containing the wind speed trend.
     */
    @Override
    public LiveData<Double> getWindTrend() {
        return windTrend;
    }

    /**
     * Returns an observable stream of the thermal score.
     *
     * @return A LiveData containing the thermal score.
     */
    @Override
    public LiveData<Integer> getThermalScore() {
        return thermalScore;
    }

    /**
     * Checks if the launch detector is currently enabled.
     *
     * @return A LiveData indicating the enabled state.
     */
    @Override
    public LiveData<Boolean> isLaunchDetectorEnabled() {
        return launchDetectorEnabled;
    }

    /**
     * Returns an observable stream of the latest raw weather data.
     *
     * @return A LiveData containing the latest measurement.
     */
    @Override
    public LiveData<WeatherData> getLatestWeatherData() {
        return latestWeatherData;
    }

    /**
     * Retrieves a copy of the historical weather data points.
     *
     * @return A list of historical data.
     */
    @Override
    public List<WeatherData> getHistoricalWeatherData() {
        synchronized (historicalData) {
            return new ArrayList<>(historicalData);
        }
    }

    /**
     * Returns an observable stream of toast messages for the UI.
     *
     * @return A LiveData containing toast messages.
     */
    @Override
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    /**
     * Returns an observable stream of log messages for debugging.
     *
     * @return A LiveData containing log messages.
     */
    @Override
    public LiveData<String> getLogMessage() {
        return logMessage;
    }

    // --- HardwareEventListener Implementation ---

    /**
     * Called when a raw string message is received from the hardware. Parses the message, applies
     * calibration, and triggers thermal analysis.
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

            // Track historical data for chart persistence
            synchronized (historicalData) {
                historicalData.add(weatherData);
                if (historicalData.size() > MAX_HISTORY_SIZE) {
                    historicalData.remove(0);
                }
            }

            ThermalAnalyzer.AnalysisResult result = thermalAnalyzer.analyze(weatherData);

            // Atomic Heartbeat Update
            processedWeatherData.postValue(
                    new com.kresshy.weatherstation.weather.ProcessedWeatherData(
                            weatherData,
                            result.decision,
                            result.tempTrend,
                            result.windTrend,
                            result.score));

            // Keep legacy individual posts for now to prevent breaking other observers
            launchDecision.postValue(result.decision);
            tempTrend.postValue(result.tempTrend);
            windTrend.postValue(result.windTrend);
            thermalScore.postValue(result.score);
            latestWeatherData.postValue(weatherData);
        }
    }

    /**
     * Responds to changes in the hardware connection state. Currently handled by the controller.
     *
     * @param state The new connection state.
     */
    @Override
    public void onConnectionStateChange(ConnectionState state) {
        // Handled by Controller
    }

    /** Called when the hardware connection is successfully established. */
    @Override
    public void onConnected() {
        // Handled by Controller
    }

    /**
     * Posts a toast message to be displayed on the UI.
     *
     * @param message The message to display.
     */
    @Override
    public void onToastMessage(String message) {
        toastMessage.postValue(message);
    }

    /**
     * Posts a log message to be displayed in the debug console.
     *
     * @param message The log entry.
     */
    @Override
    public void onLogMessage(String message) {
        logMessage.postValue(message);
    }
}
