package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for the {@link WeatherData} model. Verifies property integrity through constructors
 * and setters.
 */
public class WeatherDataTest {

    /** Verifies that the parameterized constructor correctly initializes fields. */
    @Test
    public void weatherData_ConstructorAndGettersWork() {
        WeatherData data = new WeatherData(5.5, 22.2);

        assertEquals(5.5, data.getWindSpeed(), 0.001);
        assertEquals(22.2, data.getTemperature(), 0.001);
    }

    /** Verifies that setter methods correctly update field values. */
    @Test
    public void weatherData_SettersWork() {
        WeatherData data = new WeatherData(0.0, 0.0);
        data.setWindSpeed(10.0);
        data.setTemperature(30.0);
        data.setNodeId(1);

        assertEquals(10.0, data.getWindSpeed(), 0.001);
        assertEquals(30.0, data.getTemperature(), 0.001);
        assertEquals(1, data.getNodeId());
    }
}
