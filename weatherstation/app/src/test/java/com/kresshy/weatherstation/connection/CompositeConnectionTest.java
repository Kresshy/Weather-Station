package com.kresshy.weatherstation.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;

import com.kresshy.weatherstation.bluetooth.BleConnection;
import com.kresshy.weatherstation.bluetooth.BluetoothConnection;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CompositeConnection}. Verifies routing logic and identifies reconnection
 * bugs.
 */
public class CompositeConnectionTest {

    private CompositeConnection compositeConnection;
    private BluetoothConnection classicConnection;
    private BleConnection bleConnection;
    private SimulatorConnection simulatorConnection;
    private HardwareEventListener listener;

    @Before
    public void setUp() {
        classicConnection = mock(BluetoothConnection.class);
        bleConnection = mock(BleConnection.class);
        simulatorConnection = mock(SimulatorConnection.class);
        listener = mock(HardwareEventListener.class);

        compositeConnection =
                new CompositeConnection(classicConnection, bleConnection, simulatorConnection);
    }

    @Test
    public void connect_WhenDisconnected_ShouldAllowReconnection() {
        // Arrange
        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn("00:11:22:33:44:55");

        // 1. First connection
        when(bleConnection.getState()).thenReturn(ConnectionState.stopped);
        compositeConnection.connect(bleDevice, listener);
        verify(bleConnection, times(1)).connect(bleDevice, listener);

        // 2. Simulate disconnection (state becomes 'disconnected')
        when(bleConnection.getState()).thenReturn(ConnectionState.disconnected);

        // 3. Attempt reconnection
        compositeConnection.connect(bleDevice, listener);

        // Should call connect again on bleConnection
        verify(bleConnection, times(2)).connect(bleDevice, listener);
    }

    @Test
    public void connect_WhenConnecting_ShouldIgnoreRedundantCalls() {
        // Arrange
        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn("00:11:22:33:44:55");

        // 1. First connection
        when(bleConnection.getState()).thenReturn(ConnectionState.stopped);
        compositeConnection.connect(bleDevice, listener);

        // 2. State becomes 'connecting'
        when(bleConnection.getState()).thenReturn(ConnectionState.connecting);

        // 3. Redundant connection call (e.g. from discovery update)
        compositeConnection.connect(bleDevice, listener);

        // Should ONLY have called connect once
        verify(bleConnection, times(1)).connect(bleDevice, listener);
    }

    @Test
    public void connect_WhenConnected_ShouldIgnoreRedundantCalls() {
        // Arrange
        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn("00:11:22:33:44:55");

        // 1. First connection
        when(bleConnection.getState()).thenReturn(ConnectionState.stopped);
        compositeConnection.connect(bleDevice, listener);

        // 2. State becomes 'connected'
        when(bleConnection.getState()).thenReturn(ConnectionState.connected);

        // 3. Redundant connection call (e.g. from discovery update)
        compositeConnection.connect(bleDevice, listener);

        // Should ONLY have called connect once
        verify(bleConnection, times(1)).connect(bleDevice, listener);
    }

    @Test
    public void connect_WhenSwitchingFromClassicToBle_ShouldStopClassicAndStartBle() {
        // Arrange
        BluetoothDevice classicDevice = mock(BluetoothDevice.class);
        when(classicDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(classicDevice.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");

        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn("00:11:22:33:44:55");

        // 1. Connect to Classic first
        compositeConnection.connect(classicDevice, listener);
        verify(classicConnection).connect(classicDevice, listener);

        // 2. Switch to BLE
        compositeConnection.connect(bleDevice, listener);

        // Should stop Classic and start BLE
        verify(classicConnection).stop();
        verify(bleConnection).connect(bleDevice, listener);
    }

    @Test
    public void connect_WhenSwitchingFromBleToClassic_ShouldStopBleAndStartClassic() {
        // Arrange
        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn("00:11:22:33:44:55");

        BluetoothDevice classicDevice = mock(BluetoothDevice.class);
        when(classicDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(classicDevice.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");

        // 1. Connect to BLE first
        compositeConnection.connect(bleDevice, listener);
        verify(bleConnection).connect(bleDevice, listener);

        // 2. Switch to Classic
        compositeConnection.connect(classicDevice, listener);

        // Should stop BLE and start Classic
        verify(bleConnection).stop();
        verify(classicConnection).connect(classicDevice, listener);
    }
}
