package com.kresshy.weatherstation.weather;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothDevice;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;
import com.kresshy.weatherstation.domain.ConnectToDeviceUseCase;
import com.kresshy.weatherstation.domain.GetPairedDevicesUseCase;
import com.kresshy.weatherstation.domain.GetWeatherUiStateUseCase;
import com.kresshy.weatherstation.domain.ManageDiscoveryUseCase;
import com.kresshy.weatherstation.domain.PairDeviceUseCase;
import com.kresshy.weatherstation.repository.WeatherRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WeatherViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private WeatherRepository weatherRepository;
    @Mock private WeatherConnectionController connectionController;
    @Mock private GetWeatherUiStateUseCase getWeatherUiStateUseCase;
    @Mock private ConnectToDeviceUseCase connectToDeviceUseCase;
    @Mock private GetPairedDevicesUseCase getPairedDevicesUseCase;
    @Mock private ManageDiscoveryUseCase manageDiscoveryUseCase;
    @Mock private PairDeviceUseCase pairDeviceUseCase;

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
                        pairDeviceUseCase);
    }

    @Test
    public void getWeatherUiState_callsUseCase() {
        weatherViewModel.getWeatherUiState();
        verify(getWeatherUiStateUseCase).execute();
    }

    @Test
    public void refreshPairedDevices_callsUseCase() {
        weatherViewModel.refreshPairedDevices();
        verify(getPairedDevicesUseCase).execute();
    }

    @Test
    public void clearDiscoveredDevices_callsUseCase() {
        weatherViewModel.clearDiscoveredDevices();
        verify(manageDiscoveryUseCase).clearResults();
    }

    @Test
    public void startDiscovery_callsUseCase() {
        weatherViewModel.startDiscovery();
        verify(manageDiscoveryUseCase).startDiscovery();
    }

    @Test
    public void stopDiscovery_callsUseCase() {
        weatherViewModel.stopDiscovery();
        verify(manageDiscoveryUseCase).stopDiscovery();
    }

    @Test
    public void connectToDeviceAddress_callsUseCase() {
        String address = "00:11:22:33:44:55";
        weatherViewModel.connectToDeviceAddress(address);
        verify(connectToDeviceUseCase).execute(address);
    }

    @Test
    public void pairDevice_callsUseCase() {
        BluetoothDevice device = mock(BluetoothDevice.class);
        weatherViewModel.pairDevice(device);
        verify(pairDeviceUseCase).execute(device);
    }

    @Test
    public void setPin_callsController() {
        BluetoothDevice device = mock(BluetoothDevice.class);
        String pin = "1234";
        weatherViewModel.setPin(device, pin);
        verify(connectionController).setPin(device, pin);
    }

    @Test
    public void getDiscoveredDevices_delegatesToController() {
        weatherViewModel.getDiscoveredDevices();
        verify(connectionController).getDiscoveredDevices();
    }

    @Test
    public void getUiState_delegatesToController() {
        weatherViewModel.getUiState();
        verify(connectionController).getUiState();
    }

    @Test
    public void getLaunchDecision_delegatesToRepository() {
        weatherViewModel.getLaunchDecision();
        verify(weatherRepository).getLaunchDecision();
    }

    @Test
    public void getTempTrend_delegatesToRepository() {
        weatherViewModel.getTempTrend();
        verify(weatherRepository).getTempTrend();
    }

    @Test
    public void getWindTrend_delegatesToRepository() {
        weatherViewModel.getWindTrend();
        verify(weatherRepository).getWindTrend();
    }

    @Test
    public void getThermalScore_delegatesToRepository() {
        weatherViewModel.getThermalScore();
        verify(weatherRepository).getThermalScore();
    }

    @Test
    public void isLaunchDetectorEnabled_delegatesToRepository() {
        weatherViewModel.isLaunchDetectorEnabled();
        verify(weatherRepository).isLaunchDetectorEnabled();
    }

    @Test
    public void getConnectionState_delegatesToController() {
        weatherViewModel.getConnectionState();
        verify(connectionController).getConnectionState();
    }

    @Test
    public void getHistoricalWeatherData_delegatesToRepository() {
        weatherViewModel.getHistoricalWeatherData();
        verify(weatherRepository).getHistoricalWeatherData();
    }

    @Test
    public void getToastMessage_delegatesToRepository() {
        weatherViewModel.getToastMessage();
        verify(weatherRepository).getToastMessage();
    }

    @Test
    public void getLogMessage_delegatesToRepository() {
        weatherViewModel.getLogMessage();
        verify(weatherRepository).getLogMessage();
    }

    @Test
    public void isDiscovering_delegatesToController() {
        weatherViewModel.isDiscovering();
        verify(connectionController).isDiscovering();
    }

    @Test
    public void getDiscoveryStatus_delegatesToController() {
        weatherViewModel.getDiscoveryStatus();
        verify(connectionController).getDiscoveryStatus();
    }

    @Test
    public void getBluetoothState_delegatesToController() {
        weatherViewModel.getBluetoothState();
        verify(connectionController).getBluetoothState();
    }

    @Test
    public void getConnectedDeviceName_delegatesToController() {
        weatherViewModel.getConnectedDeviceName();
        verify(connectionController).getConnectedDeviceName();
    }

    @Test
    public void getPairingRequest_delegatesToController() {
        weatherViewModel.getPairingRequest();
        verify(connectionController).getPairingRequest();
    }
}
