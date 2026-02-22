package com.kresshy.weatherstation.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherUiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetWeatherUiStateUseCaseTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private WeatherRepository repository;

    private GetWeatherUiStateUseCase useCase;

    private final MutableLiveData<WeatherData> latestWeatherData = new MutableLiveData<>();
    private final MutableLiveData<WeatherRepository.LaunchDecision> launchDecision = new MutableLiveData<>();
    private final MutableLiveData<Double> tempTrend = new MutableLiveData<>();
    private final MutableLiveData<Double> windTrend = new MutableLiveData<>();
    private final MutableLiveData<Integer> thermalScore = new MutableLiveData<>();
    private final MutableLiveData<Boolean> launchDetectorEnabled = new MutableLiveData<>();
    private final MutableLiveData<com.kresshy.weatherstation.connection.ConnectionState> connectionState = new MutableLiveData<>();
    private final MutableLiveData<String> connectedDeviceName = new MutableLiveData<>();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock returns for all repository LiveData streams
        when(repository.getLatestWeatherData()).thenReturn(latestWeatherData);
        when(repository.getLaunchDecision()).thenReturn(launchDecision);
        when(repository.getTempTrend()).thenReturn(tempTrend);
        when(repository.getWindTrend()).thenReturn(windTrend);
        when(repository.getThermalScore()).thenReturn(thermalScore);
        when(repository.isLaunchDetectorEnabled()).thenReturn(launchDetectorEnabled);
        when(repository.getConnectionState()).thenReturn(connectionState);
        when(repository.getConnectedDeviceName()).thenReturn(connectedDeviceName);

        useCase = new GetWeatherUiStateUseCase(repository);
    }

    @Test
    public void execute_synchronizesAllValuesWhenWeatherDataChanges() {
        // Must observe MediatorLiveData for it to trigger updates
        useCase.execute().observeForever(state -> {});

        // 1. Prepare new data in the repository
        WeatherData mockData = new WeatherData(5.0, 25.0);
        launchDecision.setValue(WeatherRepository.LaunchDecision.LAUNCH);
        tempTrend.setValue(0.5);
        windTrend.setValue(-0.2);
        thermalScore.setValue(85);
        launchDetectorEnabled.setValue(true);
        connectionState.setValue(com.kresshy.weatherstation.connection.ConnectionState.connected);
        connectedDeviceName.setValue("Test Station");

        // 2. Trigger the "heartbeat" (weather data)
        latestWeatherData.setValue(mockData);

        // 3. Verify the unified UI state has pulled everything correctly
        WeatherUiState uiState = useCase.execute().getValue();
        
        assertEquals(mockData, uiState.getLatestData());
        assertEquals(WeatherRepository.LaunchDecision.LAUNCH, uiState.getLaunchDecision());
        assertEquals(0.5, uiState.getTempTrend(), 0.001);
        assertEquals(-0.2, uiState.getWindTrend(), 0.001);
        assertEquals(85, uiState.getThermalScore());
        assertEquals(true, uiState.isLaunchDetectorEnabled());
        assertEquals("Test Station", uiState.getConnectedDeviceName());
    }
}
