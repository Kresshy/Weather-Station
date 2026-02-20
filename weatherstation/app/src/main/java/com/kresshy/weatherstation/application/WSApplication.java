package com.kresshy.weatherstation.application;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kresshy.weatherstation.logging.FileLoggingTree;

import dagger.hilt.android.HiltAndroidApp;

import timber.log.Timber;

/**
 * Base Application class for the Weather Station app. Initializes Hilt dependency injection and
 * configures Timber logging (including file logging).
 */
@HiltAndroidApp
public class WSApplication extends Application {

    @javax.inject.Inject FileLoggingTree fileLoggingTree;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Always use standard console debugging
        Timber.plant(new Timber.DebugTree());

        // Optionally enable logging to a local file in the Downloads folder
        if (Boolean.parseBoolean(
                sharedPreferences.getString(
                        "pref_logging_enabled", Boolean.toString(Boolean.FALSE)))) {
            Timber.plant(fileLoggingTree);
        }

        Timber.d("ONCREATE");
    }
}
