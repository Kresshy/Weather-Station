package com.kresshy.weatherstation.weather;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for a batch of weather measurements, supporting multiple sensor nodes. Used for JSON
 * deserialization of the modern communication protocol.
 */
public class Measurement {
    private int version;
    private int numberOfNodes;
    private List<WeatherData> measurements;

    /** Default constructor. */
    public Measurement() {
        this.version = 1;
        this.numberOfNodes = 0;
        this.measurements = new ArrayList<>();
    }

    /**
     * @param version Protocol version.
     * @param numberOfNodes Number of sensor nodes included.
     */
    public Measurement(int version, int numberOfNodes) {
        this.version = version;
        this.numberOfNodes = numberOfNodes;
        this.measurements = new ArrayList<>();
    }

    /**
     * @param version Protocol version.
     * @param numberOfNodes Number of sensor nodes.
     * @param measurements List of weather data points.
     */
    public Measurement(int version, int numberOfNodes, List<WeatherData> measurements) {
        this.version = version;
        this.numberOfNodes = numberOfNodes;
        this.measurements = measurements;
    }

    /**
     * Adds a new data point to this measurement batch.
     *
     * @param weatherData The weather data to add.
     */
    public void addWeatherDataToMeasurement(WeatherData weatherData) {
        measurements.add(weatherData);
    }

    /**
     * Finds weather data for a specific sensor node.
     *
     * @param nodeId The ID of the node to find.
     * @return The WeatherData for that node, or null if not found.
     */
    public WeatherData getWeatherDataForNode(int nodeId) {
        WeatherData weatherData = null;

        for (WeatherData data : measurements) {
            if (data.getNodeId() == nodeId) {
                weatherData = data;
            }
        }

        return weatherData;
    }

    /**
     * Checks if this batch contains data from a specific node.
     *
     * @param nodeId The node ID to check.
     * @return true if data exists for the given node.
     */
    public boolean hasNodeId(int nodeId) {
        for (WeatherData data : measurements) {
            if (data.getNodeId() == nodeId) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return Protocol version.
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version Protocol version to set.
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return Number of nodes in this batch.
     */
    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    /**
     * @param numberOfNodes Number of nodes to set.
     */
    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    /**
     * @return List of all measurements in this batch.
     */
    public List<WeatherData> getMeasurements() {
        return measurements;
    }

    /**
     * @param measurements List of measurements to set.
     */
    public void setMeasurements(List<WeatherData> measurements) {
        this.measurements = measurements;
    }
}
