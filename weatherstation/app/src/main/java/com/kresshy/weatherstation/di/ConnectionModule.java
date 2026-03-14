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

    /**
     * Provides the Connection implementation. This currently provides a {@link CompositeConnection}
     * which allows the application to switch between physical Bluetooth hardware and a simulator at
     * runtime.
     *
     * @param compositeConnection The composite connection implementation.
     * @return The {@link Connection} instance to be used by the application.
     */
    @Provides
    @Singleton
    public Connection provideConnection(CompositeConnection compositeConnection) {
        return compositeConnection;
    }
}
