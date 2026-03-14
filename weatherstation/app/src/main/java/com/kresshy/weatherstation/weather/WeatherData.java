package com.kresshy.weatherstation.weather;

import java.util.Date;

/** Data model representing a single weather measurement from a sensor node. */
public class WeatherData {
    private double windSpeed;
    private double temperature;
    private int nodeId;
    private Date timestamp;
    private int rssi;

    /**
     * Initializes a new WeatherData instance with default values (zeros). This is used for creating
     * empty placeholders or during deserialization.
     */
    public WeatherData() {
        this.windSpeed = 0;
        this.temperature = 0;
        this.nodeId = 0;
        this.timestamp = new Date();
    }

    /**
     * Initializes a new WeatherData instance with basic sensor readings. This is the most common
     * constructor for single-node setups.
     *
     * @param windSpeed The wind speed in m/s.
     * @param temperature The temperature in degrees Celsius.
     */
    public WeatherData(double windSpeed, double temperature) {
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.nodeId = 0;
        this.timestamp = new Date();
    }

    /**
     * Initializes a new WeatherData instance for a specific sensor node. Used in multi-node
     * environments to identify the source of the data.
     *
     * @param windSpeed The wind speed in m/s.
     * @param temperature The temperature in degrees Celsius.
     * @param nodeId The unique identifier of the sensor node.
     */
    public WeatherData(double windSpeed, double temperature, int nodeId) {
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.nodeId = nodeId;
        this.timestamp = new Date();
    }

    /**
     * Provides the wind speed measured at the sensor.
     *
     * @return The wind speed in m/s.
     */
    public double getWindSpeed() {
        return windSpeed;
    }

    /**
     * Updates the wind speed value.
     *
     * @param windSpeed The wind speed in m/s to set.
     */
    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    /**
     * Provides the temperature measured at the sensor.
     *
     * @return The temperature in degrees Celsius.
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Updates the temperature value.
     *
     * @param temperature The temperature in degrees Celsius to set.
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * Provides the unique ID of the node that sent this data.
     *
     * @return The ID of the node.
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Updates the node identifier.
     *
     * @param nodeId The node ID to set.
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Provides the exact time when this data point was generated or received.
     *
     * @return The timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Updates the timestamp.
     *
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Provides the signal strength of the wireless connection when this data was received.
     *
     * @return The Received Signal Strength Indication (RSSI) in dBm.
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * Updates the RSSI value.
     *
     * @param rssi The RSSI value to set.
     */
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
