package com.kresshy.weatherstation.weather;

import java.util.Date;


public class WeatherData {

    private double windSpeed;
    private double temperature;
    private Date timestamp;

    public WeatherData(double windSpeed, double temperature) {
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.timestamp = new Date();
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "windSpeed=" + windSpeed +
                ", temperature=" + temperature +
                ", timestamp=" + timestamp +
                '}';
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public double getTemperature() {
        return temperature;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }
}
