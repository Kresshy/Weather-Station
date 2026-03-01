package com.kresshy.weatherstation.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.os.Parcelable;

import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class GetPairedDevicesUseCaseTest {

    @Mock private WeatherConnectionController connectionController;
    private GetPairedDevicesUseCase useCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPairedDevicesUseCase(connectionController);
    }

    @Test
    public void execute_returnsListFromController() {
        List<Parcelable> expectedDevices = new ArrayList<>();
        when(connectionController.getPairedDevices()).thenReturn(expectedDevices);

        List<Parcelable> actualDevices = useCase.execute();

        assertEquals(expectedDevices, actualDevices);
    }
}
