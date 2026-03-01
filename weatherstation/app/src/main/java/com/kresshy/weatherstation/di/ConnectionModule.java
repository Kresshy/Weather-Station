package com.kresshy.weatherstation.di;

import com.kresshy.weatherstation.connection.CompositeConnection;
import com.kresshy.weatherstation.connection.Connection;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/** Hilt Module for providing connection-related dependencies. */
@Module
@InstallIn(SingletonComponent.class)
public class ConnectionModule {

    /** Provides the CompositeConnection that delegates to specific implementations. */
    @Provides
    @Singleton
    public Connection provideConnection(CompositeConnection compositeConnection) {
        return compositeConnection;
    }
}
