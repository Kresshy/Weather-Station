package com.kresshy.weatherstation.weather;

import com.google.auto.value.AutoValue;

import java.util.Date;


@AutoValue
public abstract class WeatherData {

    public static WeatherData create(double windSpeed, double temperature) {
        return create(windSpeed, temperature, 0, new Date());
    }
    public static WeatherData create(double windSpeed, double temperature, int nodeId, Date timestamp) {
        return new AutoValue_WeatherData.Builder().setWindSpeed(windSpeed).setTemperature(temperature).setNodeId(nodeId).setTimestamp(timestamp).build();
    }

    public abstract double windSpeed();
    public abstract double temperature();
    public abstract int nodeId();

    public abstract Date timestamp();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setWindSpeed(double windSpeed);

        public abstract Builder setTemperature(double temperature);

        public abstract Builder setNodeId(int nodeId);

        public abstract Builder setTimestamp(Date timestamp);

        public abstract WeatherData build();
    }
}
