package com.kresshy.weatherstation.weather;


import java.util.ArrayList;
import java.util.List;

import lombok.Data;


@Data
public class Measurement {
    private int version;
    private int numberOfNodes;
    private List<WeatherData> measurements;

    public Measurement() {
        this.version = 1;
        this.numberOfNodes = 1;
        this.measurements = new ArrayList<>();
    }

    public Measurement(int version, int numberOfNodes) {
        this.version = version;
        this.numberOfNodes = numberOfNodes;
        this.measurements = new ArrayList<>();
    }

    public Measurement(int version, int numberOfNodes, List<WeatherData> measurements) {
        this.version = version;
        this.numberOfNodes = numberOfNodes;
        this.measurements = measurements;
    }

    public void addWeatherDataToMeasurement(WeatherData weatherData) {
        measurements.add(weatherData);
    }

    public WeatherData getWeatherDataForNode(int nodeId) {
        WeatherData weatherData = null;

        for (WeatherData data : measurements) {
            if (data.getNodeId() == nodeId) {
                weatherData = data;
            }
        }

        return weatherData;
    }
}
