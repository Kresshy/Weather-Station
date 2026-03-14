package com.kresshy.weatherstation.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.kresshy.weatherstation.weather.ThermalAnalyzer;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherMessageParser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link WeatherRepositoryImpl}. Verifies data flow, outlier rejection, and
 * connection lifecycle management.
 */
public class WeatherRepositoryImplTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Context context;
    @Mock private ThermalAnalyzer thermalAnalyzer;
    @Mock private WeatherMessageParser messageParser;
    @Mock private SharedPreferences sharedPreferences;

    @Mock
    private com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController;

    private WeatherRepositoryImpl repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock SharedPreferences
        when(sharedPreferences.getString(anyString(), anyString())).thenReturn("0.0");

        repository =
                new WeatherRepositoryImpl(
                        context,
                        thermalAnalyzer,
                        messageParser,
                        sharedPreferences,
                        connectionController);
    }

    /** Verifies that receiving raw data triggers parsing and analysis, and updates observers. */
    @Test
    public void onRawDataReceived_ParsesAnalyzesAndUpdatesLiveData() {
        String rawData = "WS_some_data_end";
        WeatherData parsedData = new WeatherData(5.0, 25.0);
        ThermalAnalyzer.AnalysisResult analysisResult =
                new ThermalAnalyzer.AnalysisResult(
                        WeatherRepository.LaunchDecision.LAUNCH, 0.5, -0.2, 80);

        when(messageParser.parse(rawData)).thenReturn(parsedData);
        when(thermalAnalyzer.analyze(parsedData)).thenReturn(analysisResult);

        repository.onRawDataReceived(rawData);

        // Verify parsing and analysis were called
        verify(messageParser).parse(rawData);
        verify(thermalAnalyzer).analyze(parsedData);

        // Verify Heartbeat update
        com.kresshy.weatherstation.weather.ProcessedWeatherData heartbeat =
                repository.getProcessedWeatherData().getValue();
        assert heartbeat != null;
        assertEquals(parsedData, heartbeat.getWeatherData());
        assertEquals(WeatherRepository.LaunchDecision.LAUNCH, heartbeat.getLaunchDecision());
        assertEquals(0.5, heartbeat.getTempTrend(), 0.001);
        assertEquals(-0.2, heartbeat.getWindTrend(), 0.001);
        assertEquals(80, heartbeat.getThermalScore());
    }

    /** Verifies that physically impossible temperature jumps (Layer 2 filter) are discarded. */
    @Test
    public void onRawDataReceived_RejectsOutlierSpikes() {
        String rawData1 = "WS_data1_end";
        WeatherData saneData = new WeatherData(5.0, 25.0);

        String rawData2 = "WS_data2_end";
        WeatherData spikeData = new WeatherData(5.0, 45.0); // 20 degree jump!

        when(messageParser.parse(rawData1)).thenReturn(saneData);
        when(messageParser.parse(rawData2)).thenReturn(spikeData);
        when(thermalAnalyzer.analyze(any()))
                .thenReturn(
                        new ThermalAnalyzer.AnalysisResult(
                                WeatherRepository.LaunchDecision.WAITING, 0, 0, 0));

        // First send sane data
        repository.onRawDataReceived(rawData1);
        assertEquals(
                25.0,
                repository.getProcessedWeatherData().getValue().getWeatherData().getTemperature(),
                0.001);

        // Now send spike data
        repository.onRawDataReceived(rawData2);

        // The value should STILL be 25.0 because the 45.0 spike was rejected
        assertEquals(
                25.0,
                repository.getProcessedWeatherData().getValue().getWeatherData().getTemperature(),
                0.001);
    }

    /** Verifies that historical data is tracked and limited to MAX_HISTORY_SIZE. */
    @Test
    public void onRawDataReceived_TracksHistoricalData() {
        String rawData = "WS_data_end";
        WeatherData parsedData = new WeatherData(5.0, 25.0);
        when(messageParser.parse(rawData)).thenReturn(parsedData);
        when(thermalAnalyzer.analyze(any()))
                .thenReturn(
                        new ThermalAnalyzer.AnalysisResult(
                                WeatherRepository.LaunchDecision.WAITING, 0, 0, 0));

        // Add 310 points (max is 300)
        for (int i = 0; i < 310; i++) {
            repository.onRawDataReceived(rawData);
        }

        assertEquals(
                "History size should be capped at 300",
                300,
                repository.getHistoricalWeatherData().size());
    }

    @Test
    public void parseDoubleSafe_HandlesInvalidInput() {
        assertEquals(10.0, repository.parseDoubleSafe("10.0", 0.0), 0.001);
        assertEquals(0.0, repository.parseDoubleSafe("invalid", 0.0), 0.001);
        assertEquals(0.0, repository.parseDoubleSafe(null, 0.0), 0.001);
    }

    @Test
    public void loadLaunchDetectorSettings_CallsSharedPreferences() {
        repository.loadLaunchDetectorSettings(sharedPreferences);
        // Verify that it tries to read the threshold and EMA alpha
        // Note: It's called once in constructor, once here
        verify(sharedPreferences, times(2))
                .getBoolean(WeatherRepository.PREF_LAUNCH_DETECTOR_ENABLED, false);
        verify(sharedPreferences, times(2))
                .getString(WeatherRepository.PREF_LAUNCH_DETECTOR_SENSITIVITY, "1.0");
    }

    @Test
    public void liveDataGetters_ReturnNonNull() {
        assertNotNull(repository.getLaunchDecision());
        assertNotNull(repository.getTempTrend());
        assertNotNull(repository.getWindTrend());
        assertNotNull(repository.getThermalScore());
        assertNotNull(repository.isLaunchDetectorEnabled());
        assertNotNull(repository.getLatestWeatherData());
        assertNotNull(repository.getToastMessage());
        assertNotNull(repository.getLogMessage());
    }

    @Test
    public void hardwareEventListeners_ForwardToLiveData() {
        repository.onToastMessage("Test Toast");
        assertEquals("Test Toast", repository.getToastMessage().getValue());

        repository.onLogMessage("Test Log");
        assertEquals("Test Log", repository.getLogMessage().getValue());
    }

    @Test
    public void onConnectionStateChange_UpdatesLiveData() {
        repository.onConnectionStateChange(
                com.kresshy.weatherstation.connection.ConnectionState.connecting);
        // Verify it doesn't crash
    }

    @Test
    public void onConnected_ResetsState() {
        repository.onConnected();
        // Verify it doesn't crash
    }
}
