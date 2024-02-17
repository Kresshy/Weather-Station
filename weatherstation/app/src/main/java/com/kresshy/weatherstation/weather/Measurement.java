package com.kresshy.weatherstation.weather;


import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class Measurement {
    public abstract int version();

    public abstract int numberOfNodes();

    public abstract List<WeatherData> measurements();

    public static Measurement create(int version, int numberOfNodes) {
        return create(version, numberOfNodes, Collections.emptyList());
    }

    public static Measurement create(int version, int numberOfNodes, List<WeatherData> measurements) {
        return new AutoValue_Measurement.Builder().setVersion(version).setNumberOfNodes(numberOfNodes).setMeasurements(measurements).build();
    }

    public void addWeatherDataToMeasurement(WeatherData weatherData) {
        measurements().add(weatherData);
    }

    public WeatherData getWeatherDataForNode(int nodeId) {
        WeatherData weatherData = null;

        for (WeatherData data : measurements()) {
            if (data.nodeId() == nodeId) {
                weatherData = data;
            }
        }

        return weatherData;
    }

    public boolean hasNodeId(int nodeId) {
        for (WeatherData data : measurements()) {
            if (data.nodeId() == nodeId) {
                return true;
            }
        }

        return false;
    }

    @AutoValue.Builder
    static abstract class Builder {
        public abstract Builder setVersion(int version);

        public abstract Builder setNumberOfNodes(int numberOfNodes);

        public abstract Builder setMeasurements(List<WeatherData> measurements);

        public abstract Measurement build();
    }
}
