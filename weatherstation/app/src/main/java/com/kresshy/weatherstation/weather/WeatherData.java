package com.kresshy.weatherstation.weather;

import java.util.Date;

/** Data model representing a single weather measurement from a sensor node. */
public class WeatherData {
    private double windSpeed;
    private double temperature;
    private int nodeId;
    private Date timestamp;
    private int rssi;

    /** Default constructor initializing values to zero. */
    public WeatherData() {
        this.windSpeed = 0;
        this.temperature = 0;
        this.nodeId = 0;
        this.timestamp = new Date();
    }

    /**
     * Constructor for basic wind speed and temperature data.
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
     * Constructor for multi-node setups.
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
     * @return The wind speed in m/s.
     */
    public double getWindSpeed() {
        return windSpeed;
    }

    /**
     * @param windSpeed The wind speed in m/s to set.
     */
    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    /**
     * @return The temperature in degrees Celsius.
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * @param temperature The temperature in degrees Celsius to set.
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * @return The ID of the node that sent this data.
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * @param nodeId The node ID to set.
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * @return The timestamp when this data was created/received.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return The Received Signal Strength Indication (RSSI) in dBm.
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * @param rssi The RSSI value to set.
     */
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
