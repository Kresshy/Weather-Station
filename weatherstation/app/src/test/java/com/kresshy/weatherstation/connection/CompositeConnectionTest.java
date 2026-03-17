package com.kresshy.weatherstation.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.kresshy.weatherstation.bluetooth.BleConnection;
import com.kresshy.weatherstation.bluetooth.BluetoothConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link CompositeConnection}. Verifies routing logic and identifies reconnection
 * bugs.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = android.os.Build.VERSION_CODES.S)
public class CompositeConnectionTest {

    private Context context;
    private CompositeConnection compositeConnection;
    private BluetoothConnection classicConnection;
    private BleConnection bleConnection;
    private SimulatorConnection simulatorConnection;
    private HardwareEventListener listener;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        // Grant permissions for tests
        org.robolectric.Shadows.shadowOf((android.app.Application) context)
                .grantPermissions(android.Manifest.permission.BLUETOOTH_CONNECT);

        classicConnection = mock(BluetoothConnection.class);
        bleConnection = mock(BleConnection.class);
        simulatorConnection = mock(SimulatorConnection.class);
        listener = mock(HardwareEventListener.class);

        compositeConnection =
                new CompositeConnection(
                        context, classicConnection, bleConnection, simulatorConnection);
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

    @Test
    public void connect_WhenBleDeviceReportsUnknownTypeDuringReconnect_ShouldStayOnBleDriver() {
        // Arrange
        String address = "0C:B2:B7:7A:43:00";
        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn(address);

        BluetoothDevice unknownDevice = mock(BluetoothDevice.class);
        when(unknownDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_UNKNOWN);
        when(unknownDevice.getAddress()).thenReturn(address);

        // 1. Initial successful BLE connection
        when(bleConnection.getState()).thenReturn(ConnectionState.stopped);
        compositeConnection.connect(bleDevice, listener);
        verify(bleConnection).connect(bleDevice, listener);

        // 2. Disconnect happens
        when(bleConnection.getState()).thenReturn(ConnectionState.disconnected);

        // 3. Reconnect triggers, but Android reports UNKNOWN (0) type
        compositeConnection.connect(unknownDevice, listener);

        // Assert: Should NOT switch to classicConnection. Should NOT stop() the active driver.
        // It should call connect() on the SAME bleConnection driver again.
        verify(classicConnection, times(0)).connect(unknownDevice, listener);
        verify(bleConnection, times(0)).stop(); // Should not stop if it's the same logical driver
        verify(bleConnection, times(1)).connect(unknownDevice, listener);
    }

    @Test
    public void connect_WhenConnectingWithClassicButDiscoveryFindsBle_ShouldUpgradeToBle() {
        // Arrange - Use a generic address that won't trigger the OUI heuristic
        String address = "AA:BB:CC:DD:EE:FF";
        BluetoothDevice unknownDevice = mock(BluetoothDevice.class);
        when(unknownDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_UNKNOWN);
        when(unknownDevice.getAddress()).thenReturn(address);

        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn(address);

        // 1. Initial connect with UNKNOWN -> Routes to Classic (Default)
        when(classicConnection.getState()).thenReturn(ConnectionState.stopped);
        compositeConnection.connect(unknownDevice, listener);
        verify(classicConnection).connect(unknownDevice, listener);

        // 2. State becomes 'connecting'
        when(classicConnection.getState()).thenReturn(ConnectionState.connecting);

        // 3. Discovery finds it as BLE and triggers connect again
        compositeConnection.connect(bleDevice, listener);

        // Assert: It should STOP the failing Classic connection and START the correct BLE one
        verify(classicConnection).stop();
        verify(bleConnection).connect(bleDevice, listener);
    }

    @Test
    public void connect_WhenSwitchToDifferentDeviceWithSameDriver_ShouldStopFirst() {
        // Arrange
        BluetoothDevice stationA = mock(BluetoothDevice.class);
        when(stationA.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(stationA.getAddress()).thenReturn("AA:AA:AA:AA:AA:AA");

        BluetoothDevice stationB = mock(BluetoothDevice.class);
        when(stationB.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(stationB.getAddress()).thenReturn("BB:BB:BB:BB:BB:BB");

        // 1. Connect to Station A
        when(classicConnection.getState()).thenReturn(ConnectionState.stopped);
        compositeConnection.connect(stationA, listener);
        verify(classicConnection).connect(stationA, listener);

        // 2. State becomes 'connected'
        when(classicConnection.getState()).thenReturn(ConnectionState.connected);

        // 3. Connect to Station B (same Classic driver)
        compositeConnection.connect(stationB, listener);

        // Assert: It MUST stop the driver first to clear Station A's socket
        verify(classicConnection).stop();
        verify(classicConnection).connect(stationB, listener);
    }

    @Test
    public void connect_WhenSameDeviceSameDriverButDisconnected_ShouldReconnect() {
        // Arrange
        BluetoothDevice bleDevice = mock(BluetoothDevice.class);
        when(bleDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);
        when(bleDevice.getAddress()).thenReturn("00:11:22:33:44:55");

        // 1. Initial connect
        when(bleConnection.getState()).thenReturn(ConnectionState.stopped);
        compositeConnection.connect(bleDevice, listener);
        verify(bleConnection).connect(bleDevice, listener);

        // 2. State is 'disconnected'
        when(bleConnection.getState()).thenReturn(ConnectionState.disconnected);

        // 3. Connect again (Same device, same driver)
        compositeConnection.connect(bleDevice, listener);

        // Assert: Should NOT ignore. Should trigger connect again.
        verify(bleConnection, times(2)).connect(bleDevice, listener);
    }

    @Test
    public void connect_WhenUnknownTypeButHasBleName_ShouldRouteToBle() {
        // Arrange
        BluetoothDevice unknownDevice = mock(BluetoothDevice.class);
        when(unknownDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_UNKNOWN);
        when(unknownDevice.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(unknownDevice.getName()).thenReturn("HM-10 Weather Station");

        // Act
        compositeConnection.connect(unknownDevice, listener);

        // Assert: Heuristic should match "HM-10" and route to BLE
        verify(bleConnection).connect(unknownDevice, listener);
    }

    @Test
    public void connect_WhenUnknownTypeButHasBleOui_ShouldRouteToBle() {
        // Arrange - Huamao/HM-10 OUI
        BluetoothDevice unknownDevice = mock(BluetoothDevice.class);
        when(unknownDevice.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_UNKNOWN);
        when(unknownDevice.getAddress()).thenReturn("0C:B2:B7:AA:BB:CC");

        // Act
        compositeConnection.connect(unknownDevice, listener);

        // Assert: Heuristic should match OUI and route to BLE
        verify(bleConnection).connect(unknownDevice, listener);
    }
}
