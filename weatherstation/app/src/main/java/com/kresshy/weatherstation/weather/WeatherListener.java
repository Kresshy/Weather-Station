package com.kresshy.weatherstation.weather;

import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;

public interface WeatherListener {
    public void weatherDataReceived(WeatherData weatherData);

    public void measurementReceived(Measurement measurement);
}
