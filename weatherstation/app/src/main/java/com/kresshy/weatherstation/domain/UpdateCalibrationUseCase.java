package com.kresshy.weatherstation.domain;

import android.content.SharedPreferences;

import com.kresshy.weatherstation.repository.WeatherRepository;

import timber.log.Timber;

import javax.inject.Inject;

/** UseCase for updating and persisting sensor calibration offsets. */
public class UpdateCalibrationUseCase {

    private final SharedPreferences sharedPreferences;

    @Inject
    public UpdateCalibrationUseCase(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * Persists new calibration offsets for wind and temperature.
     *
     * @param windOffset The offset to apply to wind speed readings (m/s).
     * @param tempOffset The offset to apply to temperature readings (°C).
     */
    public void execute(String windOffset, String tempOffset) {
        // Default to 0.0 if inputs are empty or invalid
        if (windOffset == null || windOffset.trim().isEmpty()) windOffset = "0.0";
        if (tempOffset == null || tempOffset.trim().isEmpty()) tempOffset = "0.0";

        sharedPreferences
                .edit()
                .putString(WeatherRepository.KEY_WIND_DIFF, windOffset)
                .putString(WeatherRepository.KEY_TEMP_DIFF, tempOffset)
                .apply();

        Timber.d("Calibration UseCase: Saved wind=%s, temp=%s", windOffset, tempOffset);
    }
}
