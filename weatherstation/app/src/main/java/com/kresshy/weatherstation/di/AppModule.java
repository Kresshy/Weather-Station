package com.kresshy.weatherstation.di;

import com.google.gson.Gson;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/** Hilt Module for providing core application-level dependencies. */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    /**
     * Provides a singleton instance of Gson for JSON parsing.
     *
     * @return A {@link Gson} instance.
     */
    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }
}
