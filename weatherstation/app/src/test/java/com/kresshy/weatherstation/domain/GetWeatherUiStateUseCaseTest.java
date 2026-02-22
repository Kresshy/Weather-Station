package com.kresshy.weatherstation.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.ProcessedWeatherData;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherUiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetWeatherUiStateUseCaseTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private WeatherRepository repository;

    @Mock
    private com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController;

    private GetWeatherUiStateUseCase useCase;

    private final MutableLiveData<ProcessedWeatherData> processedWeatherData =
            new MutableLiveData<>();
    private final MutableLiveData<WeatherRepository.LaunchDecision> launchDecision =
            new MutableLiveData<>();
    private final MutableLiveData<Double> tempTrend = new MutableLiveData<>();
    private final MutableLiveData<Double> windTrend = new MutableLiveData<>();
    private final MutableLiveData<Integer> thermalScore = new MutableLiveData<>();
    private final MutableLiveData<Boolean> launchDetectorEnabled = new MutableLiveData<>();
    private final MutableLiveData<com.kresshy.weatherstation.connection.ConnectionState>
            connectionState = new MutableLiveData<>();
    private final MutableLiveData<String> connectedDeviceName = new MutableLiveData<>();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock returns for repository (Data Plane)
        when(repository.getProcessedWeatherData()).thenReturn(processedWeatherData);
        when(repository.getLaunchDecision()).thenReturn(launchDecision);
        when(repository.getTempTrend()).thenReturn(tempTrend);
        when(repository.getWindTrend()).thenReturn(windTrend);
        when(repository.getThermalScore()).thenReturn(thermalScore);
        when(repository.isLaunchDetectorEnabled()).thenReturn(launchDetectorEnabled);

        // Setup mock returns for controller (Control Plane)
        when(connectionController.getConnectionState()).thenReturn(connectionState);
        when(connectionController.getConnectedDeviceName()).thenReturn(connectedDeviceName);

        useCase = new GetWeatherUiStateUseCase(repository, connectionController);
    }

    @Test
    public void execute_synchronizesAllValuesFromHeartbeat() {
        // Must observe MediatorLiveData for it to trigger updates
        useCase.execute().observeForever(state -> {});

        // 1. Prepare heartbeat
        WeatherData mockData = new WeatherData(5.0, 25.0);
        ProcessedWeatherData heartbeat =
                new ProcessedWeatherData(
                        mockData, WeatherRepository.LaunchDecision.LAUNCH, 0.5, -0.2, 85);

        // 2. Trigger the heartbeat
        processedWeatherData.setValue(heartbeat);

        // 3. Verify the unified UI state has everything from the heartbeat
        WeatherUiState uiState = useCase.execute().getValue();

        assertEquals(mockData, uiState.getLatestData());
        assertEquals(WeatherRepository.LaunchDecision.LAUNCH, uiState.getLaunchDecision());
        assertEquals(0.5, uiState.getTempTrend(), 0.001);
        assertEquals(-0.2, uiState.getWindTrend(), 0.001);
        assertEquals(85, uiState.getThermalScore());
    }

    @Test
    public void execute_synchronizesIndependentStateChanges() {
        useCase.execute().observeForever(state -> {});

        // 1. Change connection state
        connectionState.setValue(com.kresshy.weatherstation.connection.ConnectionState.connected);
        connectedDeviceName.setValue("Test Station");

        // 2. Verify UI state updated even without a weather heartbeat
        WeatherUiState uiState = useCase.execute().getValue();
        assertEquals(
                com.kresshy.weatherstation.connection.ConnectionState.connected,
                uiState.getConnectionState());
        assertEquals("Test Station", uiState.getConnectedDeviceName());
    }
}
