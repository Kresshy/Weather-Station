package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MeasurementTest {

    @Test
    public void defaultConstructor_initializesCorrectly() {
        Measurement m = new Measurement();
        assertEquals(1, m.getVersion());
        assertEquals(0, m.getNumberOfNodes());
        assertNotNull(m.getMeasurements());
        assertEquals(0, m.getMeasurements().size());
    }

    @Test
    public void parameterizedConstructor_initializesCorrectly() {
        Measurement m = new Measurement(2, 5);
        assertEquals(2, m.getVersion());
        assertEquals(5, m.getNumberOfNodes());
        assertNotNull(m.getMeasurements());
    }

    @Test
    public void addWeatherDataToMeasurement_addsSuccessfully() {
        Measurement m = new Measurement();
        WeatherData data = new WeatherData(1.0, 20.0, 0);
        m.addWeatherDataToMeasurement(data);

        assertEquals(1, m.getMeasurements().size());
        assertEquals(data, m.getMeasurements().get(0));
    }

    @Test
    public void hasNodeId_returnsCorrectResult() {
        Measurement m = new Measurement();
        m.addWeatherDataToMeasurement(new WeatherData(1.0, 20.0, 0));

        assertEquals(true, m.hasNodeId(0));
        assertEquals(false, m.hasNodeId(1));
    }

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

    @Test
    public void setters_updateCorrectly() {
        Measurement m = new Measurement();
        m.setVersion(2);
        m.setNumberOfNodes(5);
        List<WeatherData> measurements = new ArrayList<>();
        m.setMeasurements(measurements);

        assertEquals(2, m.getVersion());
        assertEquals(5, m.getNumberOfNodes());
        assertEquals(measurements, m.getMeasurements());
    }
}
