package com.kresshy.weatherstation.weather;

import java.util.Date;

public class WeatherData {
    private double windSpeed;
    private double temperature;
    private int nodeId;
    private Date timestamp;

    public WeatherData() {
        this.windSpeed = 0;
        this.temperature = 0;
        this.nodeId = 0;
        this.timestamp = new Date();
    }

    public WeatherData(double windSpeed, double temperature) {
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.nodeId = 0;
        this.timestamp = new Date();
    }

    public WeatherData(double windSpeed, double temperature, int nodeId) {
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.nodeId = nodeId;
        this.timestamp = new Date();
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
