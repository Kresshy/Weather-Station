package com.kresshy.weatherstation.bluetooth;

import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WeatherBluetoothManagerTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Context context;
    @Mock private BluetoothAdapter bluetoothAdapter;
    @Mock private BluetoothDevice bluetoothDevice;

    private WeatherBluetoothManagerImpl weatherBluetoothManager;

    @Before
    public void setUp() {
        weatherBluetoothManager = new WeatherBluetoothManagerImpl(context, bluetoothAdapter);
    }

    @Test
    public void pairDevice_shouldCallCreateBond() {
        weatherBluetoothManager.pairDevice(bluetoothDevice);
        verify(bluetoothDevice).createBond();
    }

    @Test
    public void setPin_shouldCallSetPin() {
        String pin = "1234";
        weatherBluetoothManager.setPin(bluetoothDevice, pin);
        verify(bluetoothDevice).setPin(pin.getBytes());
    }
}
