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
        if (rawData == null || rawData.trim().isEmpty()) {
            return null;
        }

        // 1. Clean up the raw data (strip framing and whitespace)
        String pdu = rawData.trim()
                .replace("WS_", "")
                .replace("start_", "")
                .replace("_end", "")
                .trim();

        if (pdu.isEmpty()) return null;

        try {
            Timber.d("Parsing cleaned PDU: %s", pdu);

            // 2. Try JSON parsing (Modern format)
            if (pdu.startsWith("{")) {
                try {
                    Measurement measurement = gson.fromJson(pdu, Measurement.class);
                    if (measurement != null && measurement.getWeatherDataForNode(0) != null) {
                        return measurement.getWeatherDataForNode(0);
                    }
                } catch (JsonSyntaxException e) {
                    Timber.w("Failed to parse JSON, falling back to legacy: %s", pdu);
                }
            }

            // 3. Fallback to legacy space-separated format
            return parseLegacy(pdu);

        } catch (Exception e) {
            Timber.e(e, "Error parsing message: %s", rawData);
        }

        return null;
    }

    /**
     * Parses legacy format: "{windSpeed} {temperature}" or "{windSpeed} {temperature} {nodeId}"
     */
    private WeatherData parseLegacy(String pdu) {
        try {
            // Split by any whitespace or special separators
            String[] parts = pdu.split("[\\s,;]+");
            if (parts.length >= 2) {
                double windSpeed = Double.parseDouble(parts[0]);
                double temperature = Double.parseDouble(parts[1]);
                
                int nodeId = 0;
                if (parts.length >= 3) {
                    try {
                        nodeId = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException ignored) {}
                }
                
                return new WeatherData(windSpeed, temperature, nodeId);
            }
        } catch (NumberFormatException e) {
            Timber.e("Invalid legacy format: %s", pdu);
        }
        return null;
    }
}
