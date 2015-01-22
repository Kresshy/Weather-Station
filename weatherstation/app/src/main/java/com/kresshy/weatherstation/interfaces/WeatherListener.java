package com.kresshy.weatherstation.interfaces;

import com.kresshy.weatherstation.weather.WeatherData;

/**
 * Created by Szabolcs on 2014.12.28..
 */
public interface WeatherListener {

    public void weatherDataReceived(WeatherData weatherData);
}
