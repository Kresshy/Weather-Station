package com.kresshy.weatherstation.weather;

import java.util.Date;

import lombok.Data;
import lombok.Getter;

@Data
public class WeatherData {

    private double windSpeed;
    private double temperature;
    private int nodeId;

    @Getter
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
}
