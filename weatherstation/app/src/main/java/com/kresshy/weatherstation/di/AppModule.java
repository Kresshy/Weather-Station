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

    /** Provides a singleton instance of Gson for JSON parsing. */
    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }

    /** Provides the default SharedPreferences. */
    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /** Provides the default BluetoothAdapter. */
    @Provides
    @Singleton
    @androidx.annotation.Nullable
    public BluetoothAdapter provideBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    /** Provides the NotificationManager system service. */
    @Provides
    @Singleton
    public NotificationManager provideNotificationManager(@ApplicationContext Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /** Provides a singleton instance of Random. */
    @Provides
    @Singleton
    public java.util.Random provideRandom() {
        return new java.util.Random();
    }
}
