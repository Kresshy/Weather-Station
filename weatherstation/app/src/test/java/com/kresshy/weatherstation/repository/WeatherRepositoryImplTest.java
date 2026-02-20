package com.kresshy.weatherstation.repository;

import static com.kresshy.weatherstation.repository.WeatherRepository.KEY_TEMP_DIFF;
import static com.kresshy.weatherstation.repository.WeatherRepository.KEY_WIND_DIFF;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.bluetooth.WeatherBluetoothManager;
import com.kresshy.weatherstation.connection.ConnectionManager;
import com.kresshy.weatherstation.weather.ThermalAnalyzer;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherMessageParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

/**
 * Unit tests for {@link WeatherRepositoryImpl}. Verifies data flow, calibration logic, and
 * connection lifecycle management.
 */
public class WeatherRepositoryImplTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Context context;
    @Mock private ThermalAnalyzer thermalAnalyzer;
    @Mock private WeatherMessageParser messageParser;
    @Mock private WeatherBluetoothManager bluetoothManager;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private BluetoothAdapter bluetoothAdapter;
    @Mock private ConnectionManager connectionManager;

    private WeatherRepositoryImpl repository;
    private MockedStatic<PreferenceManager> mockedPreferenceManager;
    private MockedStatic<BluetoothAdapter> mockedBluetoothAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedPreferenceManager = Mockito.mockStatic(PreferenceManager.class);
        mockedBluetoothAdapter = Mockito.mockStatic(BluetoothAdapter.class);

        mockedPreferenceManager
                .when(() -> PreferenceManager.getDefaultSharedPreferences(any(Context.class)))
                .thenReturn(sharedPreferences);
        mockedBluetoothAdapter
                .when(BluetoothAdapter::getDefaultAdapter)
                .thenReturn(bluetoothAdapter);

        // Mock BluetoothManager LiveData
        when(bluetoothManager.getDiscoveredDevices())
                .thenReturn(new MutableLiveData<>(new ArrayList<>()));
        when(bluetoothManager.getDiscoveryStatus()).thenReturn(new MutableLiveData<>(""));
        when(sharedPreferences.getString(anyString(), anyString())).thenReturn("0.0");

        repository =
                new WeatherRepositoryImpl(
                        context,
                        thermalAnalyzer,
                        messageParser,
                        bluetoothManager,
                        connectionManager);
    }

    @After
    public void tearDown() {
        mockedPreferenceManager.close();
        mockedBluetoothAdapter.close();
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

        // Verify LiveData updates
        assertEquals(parsedData, repository.getLatestWeatherData().getValue());
        assertEquals(
                WeatherRepository.LaunchDecision.LAUNCH, repository.getLaunchDecision().getValue());
        assertEquals(0.5, repository.getTempTrend().getValue(), 0.001);
        assertEquals(-0.2, repository.getWindTrend().getValue(), 0.001);
        assertEquals(80, (int) repository.getThermalScore().getValue());
    }

    /** Verifies that user-defined calibration offsets are applied before data is broadcast. */
    @Test
    public void onRawDataReceived_AppliesCorrections() {
        // Set corrections via shared preferences
        when(sharedPreferences.getString(KEY_WIND_DIFF, "0.0")).thenReturn("1.0");
        when(sharedPreferences.getString(KEY_TEMP_DIFF, "0.0")).thenReturn("-2.0");

        // Re-initialize repository to pick up corrections
        repository =
                new WeatherRepositoryImpl(
                        context,
                        thermalAnalyzer,
                        messageParser,
                        bluetoothManager,
                        connectionManager);

        String rawData = "WS_data_end";
        WeatherData parsedData = new WeatherData(5.0, 25.0);
        when(messageParser.parse(rawData)).thenReturn(parsedData);
        when(thermalAnalyzer.analyze(any()))
                .thenReturn(
                        new ThermalAnalyzer.AnalysisResult(
                                WeatherRepository.LaunchDecision.WAITING, 0, 0, 0));

        repository.onRawDataReceived(rawData);

        // Verify corrections applied: 5.0 + 1.0 = 6.0, 25.0 - 2.0 = 23.0
        assertEquals(6.0, repository.getLatestWeatherData().getValue().getWindSpeed(), 0.001);
        assertEquals(23.0, repository.getLatestWeatherData().getValue().getTemperature(), 0.001);
    }

    @Test
    public void startConnection_CallsManager() {
        repository.startConnection();
        verify(connectionManager).startConnection();
    }

    @Test
    public void stopConnection_CallsManagerAndResetsAnalyzer() {
        repository.stopConnection();
        verify(connectionManager).stopConnection();
        verify(thermalAnalyzer).reset();
    }

    @Test
    public void onConnectionStateChange_Connecting_UpdatesUiStateToLoading() {
        repository.onConnectionStateChange(
                com.kresshy.weatherstation.connection.ConnectionState.connecting);
        assertEquals(
                com.kresshy.weatherstation.util.Resource.Status.LOADING,
                repository.getUiState().getValue().status);
    }

    /** Verifies the auto-reconnect logic with exponential backoff. */
    @Test
    public void onConnectionStateChange_Disconnected_SchedulesReconnect()
            throws InterruptedException {
        // Setup for reconnection: need a device and shouldReconnect = true
        android.bluetooth.BluetoothDevice mockDevice =
                mock(android.bluetooth.BluetoothDevice.class);
        when(mockDevice.getName()).thenReturn("Test Device");

        // This sets lastConnectedDevice and shouldReconnect = true
        repository.connectToDevice(mockDevice);

        // Trigger disconnected state
        repository.onConnectionStateChange(
                com.kresshy.weatherstation.connection.ConnectionState.disconnected);

        // Wait for the INITIAL_RECONNECT_DELAY_MS (2000ms)
        Thread.sleep(2500);

        // Verify manager connectToDevice was called again (one for initial, one for reconnect)
        verify(connectionManager, Mockito.atLeast(2)).connectToDevice(mockDevice);
    }
}
