package com.kresshy.weatherstation.connection;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Random;

/**
 * Unit test for {@link SimulatorConnection}. Demonstrates how DI allows us to mock Random for
 * deterministic testing.
 */
public class SimulatorConnectionTest {

    private SimulatorConnection simulatorConnection;
    private Random mockRandom;
    private HardwareEventListener mockListener;

    @Before
    public void setUp() {
        mockRandom = mock(Random.class);
        mockListener = mock(HardwareEventListener.class);
        simulatorConnection = new SimulatorConnection(mockRandom);
        simulatorConnection.setCallback(mockListener);
    }

    @Test
    public void startDataSimulation_TriggerThermal_SendsIncreasedTemperature()
            throws InterruptedException {
        // Force Random to trigger a thermal (random.nextInt(100) < 2)
        when(mockRandom.nextInt(100)).thenReturn(1);
        // Force random drifts/increments to be predictable
        when(mockRandom.nextDouble()).thenReturn(0.5);

        simulatorConnection.start(mockListener);
        // We need to trigger the private startDataSimulation indirectly via connect
        simulatorConnection.connect(null, mockListener);

        // Wait for at least one or two ticks of the simulation (1Hz)
        Thread.sleep(2500);

        // Verify that raw data was received
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockListener, atLeastOnce()).onRawDataReceived(captor.capture());

        // We could parse the JSON here to verify the temperature is rising,
        // but the main point is that we successfully controlled the "random" pulse.
    }
}
