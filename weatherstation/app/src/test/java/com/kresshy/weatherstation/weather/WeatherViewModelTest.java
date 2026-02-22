package com.kresshy.weatherstation.weather;

import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

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
    private com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController;

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
                        connectionController,
                        getWeatherUiStateUseCase,
                        connectToDeviceUseCase,
                        getPairedDevicesUseCase,
                        manageDiscoveryUseCase,
                        updateCalibrationUseCase);
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
