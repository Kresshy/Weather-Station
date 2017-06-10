package com.kresshy.weatherstation.weather;


import java.util.ArrayList;
import java.util.List;

import lombok.Data;


@Data
public class Measurement {
    private int version;
    private int numberOfNodes;
    private List<WeatherData> weatherDataList;

    public Measurement() {
        this.version = 1;
        this.numberOfNodes = 1;
        this.weatherDataList = new ArrayList<>();
    }

    public Measurement(int version, int numberOfNodes) {
        this.version = version;
        this.numberOfNodes = numberOfNodes;
        this.weatherDataList = new ArrayList<>();
    }

    public Measurement(int version, int numberOfNodes, List<WeatherData> weatherDataList) {
        this.version = version;
        this.numberOfNodes = numberOfNodes;
        this.weatherDataList = weatherDataList;
    }

    public void addWeatherDataToMeasurement(WeatherData weatherData) {
        weatherDataList.add(weatherData);
    }

    public WeatherData getWeatherDataForNode(int nodeId) {
        WeatherData weatherData = null;

        for (WeatherData data : weatherDataList) {
            if (data.getNodeId() == nodeId) {
                weatherData = data;
            }
        }

        return weatherData;
    }
}
