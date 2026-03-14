package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;

import com.kresshy.weatherstation.repository.WeatherRepository;

import org.junit.Test;

public class ProcessedWeatherDataTest {

    @Test
    public void constructorAndGetters_workCorrectly() {
        WeatherData weatherData = new WeatherData(5.5, 22.2);
        WeatherRepository.LaunchDecision decision = WeatherRepository.LaunchDecision.LAUNCH;
        double tempTrend = 0.5;
        double windTrend = -0.2;
        int thermalScore = 85;

        ProcessedWeatherData processed =
                new ProcessedWeatherData(weatherData, decision, tempTrend, windTrend, thermalScore);

        assertEquals(weatherData, processed.getWeatherData());
        assertEquals(decision, processed.getLaunchDecision());
        assertEquals(tempTrend, processed.getTempTrend(), 0.001);
        assertEquals(windTrend, processed.getWindTrend(), 0.001);
        assertEquals(thermalScore, processed.getThermalScore());
    }
}
