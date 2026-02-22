package com.kresshy.weatherstation.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.connection.RawDataCallback;
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
public class WeatherRepositoryImpl implements WeatherRepository, RawDataCallback {

    private final Context context;
    private final ThermalAnalyzer thermalAnalyzer;
    private final WeatherMessageParser messageParser;
    private final SharedPreferences sharedPreferences;

    private final MutableLiveData<com.kresshy.weatherstation.weather.ProcessedWeatherData>
            processedWeatherData = new MutableLiveData<>();
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

    private double correctionWind = 0.0;
    private double correctionTemp = 0.0;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    /** Primary constructor used by Hilt. */
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
                    .setDataCallback(this);
        }

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
        synchronized (historicalData) {
            return new ArrayList<>(historicalData);
        }
    }

    @Override
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    @Override
    public LiveData<String> getLogMessage() {
        return logMessage;
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
        }
    }

    private void applyCorrections(WeatherData data) {
        data.setWindSpeed(data.getWindSpeed() + correctionWind);
        data.setTemperature(data.getTemperature() + correctionTemp);
    }

    @Override
    public void onConnectionStateChange(ConnectionState state) {
        // Handled by Controller
    }

    @Override
    public void onConnected() {
        // Handled by Controller
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
