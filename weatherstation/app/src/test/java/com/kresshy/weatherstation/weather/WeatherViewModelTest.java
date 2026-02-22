package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.repository.WeatherRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for the {@link WeatherViewModel}. Verifies that the ViewModel correctly interacts with
 * the {@link WeatherRepository}.
 */
public class WeatherViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private WeatherRepository weatherRepository;

    @Mock
    private com.kresshy.weatherstation.domain.GetWeatherUiStateUseCase getWeatherUiStateUseCase;

    @Mock private com.kresshy.weatherstation.domain.ConnectToDeviceUseCase connectToDeviceUseCase;
    @Mock private com.kresshy.weatherstation.domain.GetPairedDevicesUseCase getPairedDevicesUseCase;
    @Mock private com.kresshy.weatherstation.domain.ManageDiscoveryUseCase manageDiscoveryUseCase;

    @Mock
    private com.kresshy.weatherstation.domain.UpdateCalibrationUseCase updateCalibrationUseCase;

    private WeatherViewModel weatherViewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        weatherViewModel =
                new WeatherViewModel(
                        weatherRepository,
                        getWeatherUiStateUseCase,
                        connectToDeviceUseCase,
                        getPairedDevicesUseCase,
                        manageDiscoveryUseCase,
                        updateCalibrationUseCase);
    }

    /**
     * Verifies that weather data updates in the repository are correctly reflected in the
     * ViewModel.
     */
    @Test
    public void getLatestWeatherData_returnsDataFromRepository() {
        WeatherData mockData = new WeatherData(10.0, 25.0);
        MutableLiveData<WeatherData> liveData = new MutableLiveData<>();
        liveData.setValue(mockData);

        when(weatherRepository.getLatestWeatherData()).thenReturn(liveData);

        assertEquals(mockData, weatherViewModel.getLatestWeatherData().getValue());
        verify(weatherRepository).getLatestWeatherData();
    }

    /** Verifies that the ViewModel correctly delegates the refresh paired devices request. */
    @Test
    public void refreshPairedDevices_callsUseCase() {
        weatherViewModel.refreshPairedDevices();
        verify(getPairedDevicesUseCase).execute();
    }

    /** Verifies that the ViewModel correctly delegates the clear discovered devices request. */
    @Test
    public void clearDiscoveredDevices_callsUseCase() {
        weatherViewModel.clearDiscoveredDevices();
        verify(manageDiscoveryUseCase).clearResults();
    }
}
