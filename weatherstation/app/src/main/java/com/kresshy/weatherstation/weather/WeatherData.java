package com.kresshy.weatherstation.weather;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;


public class WeatherData {

    @Getter
    @Setter
    private double windSpeed;

    @Getter
    @Setter
    private double temperature;

    @Getter
    private Date timestamp;

    @Getter
    @Setter
    private int nodeId;

    public WeatherData() {
        this.windSpeed = 0;
        this.temperature = 0;
        this.nodeId = 1;
        this.timestamp = new Date();
    }

    public WeatherData(double windSpeed, double temperature) {
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.nodeId = 1;
        this.timestamp = new Date();
    }

    public WeatherData(double windSpeed, double temperature, int nodeId) {
        this.windSpeed = windSpeed;
        this.temperature = temperature;
        this.nodeId = nodeId;
        this.timestamp = new Date();
    }
}
