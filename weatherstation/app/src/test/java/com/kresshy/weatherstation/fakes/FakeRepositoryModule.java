package com.kresshy.weatherstation.fakes;

import static org.mockito.Mockito.mock;

import com.kresshy.weatherstation.bluetooth.WeatherBluetoothManager;
import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;
import com.kresshy.weatherstation.di.RepositoryModule;
import com.kresshy.weatherstation.repository.WeatherRepository;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

import javax.inject.Singleton;

/**
 * Hilt module that replaces the production {@link RepositoryModule} with fake/mock versions during
 * test execution.
 */
@Module
@TestInstallIn(components = SingletonComponent.class, replaces = RepositoryModule.class)
public class FakeRepositoryModule {

    /** Provides a {@link FakeWeatherRepository} for manual control in tests. */
    @Provides
    @Singleton
    public WeatherRepository provideWeatherRepository() {
        return new FakeWeatherRepository();
    }

    /** Provides a Mockito-mocked version of the Bluetooth manager. */
    @Provides
    @Singleton
    public WeatherBluetoothManager provideWeatherBluetoothManager() {
        return mock(WeatherBluetoothManager.class);
    }

    /** Provides a fake connection controller for tests. */
    @Provides
    @Singleton
    public WeatherConnectionController provideWeatherConnectionController() {
        return new FakeWeatherConnectionController();
    }
}
