package com.kresshy.weatherstation.bluetooth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.connection.ConnectionManager;
import com.kresshy.weatherstation.connection.ConnectionState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class WeatherConnectionControllerImplTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Context context;
    @Mock private ConnectionManager connectionManager;
    @Mock private BluetoothAdapter bluetoothAdapter;
    @Mock private WeatherBluetoothManager bluetoothManager;
    @Mock private SharedPreferences sharedPreferences;

    private WeatherConnectionControllerImpl controller;

    @Before
    public void setUp() {
        // Mock the LiveData returned by WeatherBluetoothManager
        when(bluetoothManager.getDiscoveredDevices())
                .thenReturn(new MutableLiveData<>(new ArrayList<>()));
        when(bluetoothManager.getDiscoveryStatus()).thenReturn(new MutableLiveData<>(""));
        when(bluetoothManager.isDiscovering()).thenReturn(new MutableLiveData<>(false));

        controller =
                new WeatherConnectionControllerImpl(
                        context,
                        connectionManager,
                        bluetoothAdapter,
                        bluetoothManager,
                        sharedPreferences);
    }

    @Test
    public void startConnection_WhenStopped_ShouldCallManagerStart() {
        // Initial state is stopped
        assertEquals(ConnectionState.stopped, controller.getConnectionState().getValue());

        controller.startConnection();

        verify(connectionManager, times(1)).startConnection();
    }

    @Test
    public void startConnection_WhenNotStopped_ShouldIgnoreRequest() {
        // 1. Capture the internal listener that the controller sets on the manager
        com.kresshy.weatherstation.connection.HardwareEventListener capturedListener =
                mock(com.kresshy.weatherstation.connection.HardwareEventListener.class);
        controller.setHardwareEventListener(capturedListener);

        org.mockito.ArgumentCaptor<com.kresshy.weatherstation.connection.HardwareEventListener>
                listenerCaptor =
                        org.mockito.ArgumentCaptor.forClass(
                                com.kresshy.weatherstation.connection.HardwareEventListener.class);
        verify(connectionManager).setListener(listenerCaptor.capture());
        com.kresshy.weatherstation.connection.HardwareEventListener internalListener =
                listenerCaptor.getValue();

        // 2. Transition to 'connecting'
        internalListener.onConnectionStateChange(ConnectionState.connecting);
        assertEquals(ConnectionState.connecting, controller.getConnectionState().getValue());

        // 3. Call startConnection() - should be ignored because state is NOT stopped
        controller.startConnection();

        // Should still only have 0 calls to startConnection() because it was already connecting
        verify(connectionManager, times(0)).startConnection();
    }

    @Test
    public void stopConnection_ShouldCallManagerStop() {
        controller.stopConnection();
        verify(connectionManager).stopConnection();
    }
}
