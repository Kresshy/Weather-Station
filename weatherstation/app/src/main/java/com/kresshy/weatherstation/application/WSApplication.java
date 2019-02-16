package com.kresshy.weatherstation.application;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kresshy.weatherstation.logging.FileLoggingTree;

import timber.log.Timber;

public class WSApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Timber.plant(new Timber.DebugTree());
        if (Boolean.parseBoolean(sharedPreferences.getString("pref_logging_enabled", Boolean.toString(Boolean.FALSE)))) {
            Timber.plant(new FileLoggingTree(getApplicationContext()));
        }

        Timber.d("onCreate()");
    }
}
