package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.repository.WeatherRepository;

import org.junit.Test;

public class WeatherUiStateTest {

    @Test
    public void weatherUiState_constructor_initializesFields() {
        WeatherData latest = new WeatherData(5.0, 25.0);
        
        WeatherUiState state = new WeatherUiState(
                latest,
                WeatherRepository.LaunchDecision.LAUNCH,
                0.5,
                -0.2,
                85,
                true,
                ConnectionState.connected,
                "Test Device"
        );

        assertEquals(latest, state.getLatestData());
        assertEquals(WeatherRepository.LaunchDecision.LAUNCH, state.getLaunchDecision());
        assertEquals(0.5, state.getTempTrend(), 0.001);
        assertEquals(-0.2, state.getWindTrend(), 0.001);
        assertEquals(85, state.getThermalScore());
        assertTrue(state.isLaunchDetectorEnabled());
        assertEquals(ConnectionState.connected, state.getConnectionState());
        assertEquals("Test Device", state.getConnectedDeviceName());
    }

    @Test
    public void empty_returnsEmptyState() {
        WeatherUiState state = WeatherUiState.empty();
        
        assertNull(state.getLatestData());
        assertEquals(WeatherRepository.LaunchDecision.WAITING, state.getLaunchDecision());
        assertEquals(0.0, state.getTempTrend(), 0.001);
        assertEquals(0.0, state.getWindTrend(), 0.001);
        assertEquals(0, state.getThermalScore());
        assertFalse(state.isLaunchDetectorEnabled());
        assertEquals(ConnectionState.stopped, state.getConnectionState());
        assertNull(state.getConnectedDeviceName());
    }
}
