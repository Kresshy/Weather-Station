package com.kresshy.weatherstation.di;

import com.kresshy.weatherstation.bluetooth.WeatherBluetoothManager;
import com.kresshy.weatherstation.bluetooth.WeatherBluetoothManagerImpl;
import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;
import com.kresshy.weatherstation.bluetooth.WeatherConnectionControllerImpl;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.repository.WeatherRepositoryImpl;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/** Hilt Module for binding repository and manager interfaces to their concrete implementations. */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    /** Binds the WeatherRepository implementation. */
    @Binds
    @Singleton
    public abstract WeatherRepository bindWeatherRepository(
            WeatherRepositoryImpl weatherRepositoryImpl);

    /** Binds the WeatherBluetoothManager implementation. */
    @Binds
    @Singleton
    public abstract WeatherBluetoothManager bindWeatherBluetoothManager(
            WeatherBluetoothManagerImpl weatherBluetoothManagerImpl);

    /** Binds the WeatherConnectionController implementation. */
    @Binds
    @Singleton
    public abstract WeatherConnectionController bindWeatherConnectionController(
            WeatherConnectionControllerImpl weatherConnectionControllerImpl);
}
