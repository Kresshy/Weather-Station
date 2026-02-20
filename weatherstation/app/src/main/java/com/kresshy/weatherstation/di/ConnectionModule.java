package com.kresshy.weatherstation.di;

import android.content.SharedPreferences;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.connection.Connection;
import com.kresshy.weatherstation.connection.SimulatorConnection;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Provider;
import javax.inject.Singleton;

/** Hilt Module for providing connection-related dependencies. */
@Module
@InstallIn(SingletonComponent.class)
public class ConnectionModule {

    /**
     * Provides the appropriate Connection implementation based on user preferences. Uses Provider
     * to allow dynamic switching if needed (though usually connection lifecycle is managed by
     * ConnectionManager).
     */
    @Provides
    @Singleton
    public Connection provideConnection(
            SharedPreferences sharedPreferences,
            Provider<BluetoothConnection> bluetoothConnectionProvider,
            Provider<SimulatorConnection> simulatorConnectionProvider) {

        boolean useSimulator = sharedPreferences.getBoolean("pref_simulator_mode", false);

        if (useSimulator) {
            return simulatorConnectionProvider.get();
        }
        return bluetoothConnectionProvider.get();
    }
}
