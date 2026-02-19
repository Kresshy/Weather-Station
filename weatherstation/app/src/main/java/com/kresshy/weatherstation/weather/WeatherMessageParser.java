package com.kresshy.weatherstation.weather;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import timber.log.Timber;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Parses raw string messages from the weather station into WeatherData objects. Supports both
 * modern JSON format and legacy space-separated formats.
 */
@Singleton
public class WeatherMessageParser {

    private final Gson gson;

    @Inject
    public WeatherMessageParser(Gson gson) {
        this.gson = gson;
    }

    /**
     * Parses a raw weather message. Expected format: "WS_{DATA}_end" where {DATA} can be JSON or
     * legacy string.
     *
     * @param rawData The raw message string from the sensor.
     * @return A parsed WeatherData object, or null if parsing fails.
     */
    public WeatherData parse(String rawData) {
        if (rawData == null || !rawData.startsWith("WS_")) {
            return null;
        }

        try {
            String[] parts = rawData.split("_");
            if (parts.length < 2) return null;

            String pdu = parts[1];
            Timber.d("Parsing PDU: %s", pdu);

            // Attempt JSON parsing (Modern format)
            try {
                Measurement measurement = gson.fromJson(pdu, Measurement.class);
                if (measurement != null && measurement.getWeatherDataForNode(0) != null) {
                    return measurement.getWeatherDataForNode(0);
                }
            } catch (JsonSyntaxException e) {
                // Fallback to legacy format
                return parseLegacy(pdu);
            }
        } catch (Exception e) {
            Timber.e(e, "Error parsing message: %s", rawData);
        }

        return null;
    }

    /**
     * Parses the legacy space-separated format: "{windSpeed} {temperature}"
     *
     * @param pdu The data payload part of the message.
     * @return WeatherData object or null if format is invalid.
     */
    private WeatherData parseLegacy(String pdu) {
        try {
            String[] weather = pdu.split(" ");
            if (weather.length >= 2) {
                double windSpeed = Double.parseDouble(weather[0]);
                double temperature = Double.parseDouble(weather[1]);
                return new WeatherData(windSpeed, temperature);
            }
        } catch (NumberFormatException e) {
            Timber.e("Invalid legacy PDU format: %s", pdu);
        }
        return null;
    }
}
