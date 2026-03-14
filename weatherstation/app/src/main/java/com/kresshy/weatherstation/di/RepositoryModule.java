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

    /**
     * Binds the WeatherRepository interface to its concrete implementation. This repository acts as
     * the central hub for weather data and analysis.
     *
     * @param weatherRepositoryImpl The concrete repository implementation.
     * @return The bound {@link WeatherRepository}.
     */
    @Binds
    @Singleton
    public abstract WeatherRepository bindWeatherRepository(
            WeatherRepositoryImpl weatherRepositoryImpl);

    /**
     * Binds the WeatherBluetoothManager interface to its concrete implementation. This manager
     * handles low-level Bluetooth discovery and connectivity.
     *
     * @param weatherBluetoothManagerImpl The concrete Bluetooth manager implementation.
     * @return The bound {@link WeatherBluetoothManager}.
     */
    @Binds
    @Singleton
    public abstract WeatherBluetoothManager bindWeatherBluetoothManager(
            WeatherBluetoothManagerImpl weatherBluetoothManagerImpl);

    /**
     * Binds the WeatherConnectionController interface to its concrete implementation. This
     * controller orchestrates the connection lifecycle and provides state updates to the domain
     * layer.
     *
     * @param weatherConnectionControllerImpl The concrete connection controller implementation.
     * @return The bound {@link WeatherConnectionController}.
     */
    @Binds
    @Singleton
    public abstract WeatherConnectionController bindWeatherConnectionController(
            WeatherConnectionControllerImpl weatherConnectionControllerImpl);
}
