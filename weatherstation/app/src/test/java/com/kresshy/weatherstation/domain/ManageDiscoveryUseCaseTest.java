package com.kresshy.weatherstation.domain;

import static org.mockito.Mockito.verify;

import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ManageDiscoveryUseCaseTest {

    @Mock private WeatherConnectionController connectionController;
    private ManageDiscoveryUseCase useCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ManageDiscoveryUseCase(connectionController);
    }

    @Test
    public void startDiscovery_callsController() {
        useCase.startDiscovery();
        verify(connectionController).startDiscovery();
    }

    @Test
    public void stopDiscovery_callsController() {
        useCase.stopDiscovery();
        verify(connectionController).stopDiscovery();
    }

    @Test
    public void clearResults_callsController() {
        useCase.clearResults();
        verify(connectionController).clearDiscoveredDevices();
    }
}
