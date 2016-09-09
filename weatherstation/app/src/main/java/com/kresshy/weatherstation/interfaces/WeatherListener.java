package com.kresshy.weatherstation.interfaces;

import com.kresshy.weatherstation.weather.WeatherData;

public interface WeatherListener {

    public void weatherDataReceived(WeatherData weatherData);
}
