package com.kresshy.weatherstation.di;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/** Hilt Module for providing core application-level dependencies. */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    /**
     * Provides a singleton instance of Gson. This is used throughout the application for consistent
     * JSON serialization and deserialization of weather data.
     *
     * @return A singleton {@link Gson} instance.
     */
    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }

    /**
     * Provides the default SharedPreferences. This is used for persisting user settings, such as
     * the last connected device address and measurement intervals.
     *
     * @param context The application context.
     * @return The default {@link SharedPreferences} for the application.
     */
    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Provides the default BluetoothAdapter. This is the entry point for all Bluetooth-related
     * operations on the device.
     *
     * @return The default {@link BluetoothAdapter}, or null if Bluetooth is not supported.
     */
    @Provides
    @Singleton
    @androidx.annotation.Nullable public BluetoothAdapter provideBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Provides the NotificationManager system service. This is used by the background service to
     * post status updates and alerts to the user.
     *
     * @param context The application context.
     * @return The system's {@link NotificationManager}.
     */
    @Provides
    @Singleton
    public NotificationManager provideNotificationManager(@ApplicationContext Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Provides a singleton instance of Random. This is used primarily by the simulator to generate
     * unpredictable but realistic weather patterns.
     *
     * @return A singleton {@link java.util.Random} instance.
     */
    @Provides
    @Singleton
    public java.util.Random provideRandom() {
        return new java.util.Random();
    }
}
