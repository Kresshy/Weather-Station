package com.kresshy.weatherstation.weather;

public interface WeatherListener {
    void weatherDataReceived(WeatherData weatherData);

    void measurementReceived(Measurement measurement);
}
