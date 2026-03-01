package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MeasurementTest {

    @Test
    public void getWeatherDataForNode_returnsCorrectNode() {
        List<WeatherData> measurements = new ArrayList<>();
        measurements.add(new WeatherData(1.0, 20.0, 0));
        measurements.add(new WeatherData(2.0, 22.0, 1));
        
        // Correct constructor: version, numberOfNodes, measurements
        Measurement m = new Measurement(1, 2, measurements);
        
        WeatherData node0 = m.getWeatherDataForNode(0);
        assertNotNull(node0);
        assertEquals(1.0, node0.getWindSpeed(), 0.001);
        
        WeatherData node1 = m.getWeatherDataForNode(1);
        assertNotNull(node1);
        assertEquals(2.0, node1.getWindSpeed(), 0.001);
    }

    @Test
    public void getWeatherDataForNode_returnsNullIfNotFound() {
        List<WeatherData> measurements = new ArrayList<>();
        measurements.add(new WeatherData(1.0, 20.0, 0));
        
        Measurement m = new Measurement(1, 1, measurements);
        
        assertNull(m.getWeatherDataForNode(99));
    }
}
