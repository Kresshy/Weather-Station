package com.kresshy.weatherstation.bluetooth;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S)
public class BluetoothDiscoveryTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private WeatherBluetoothManagerImpl weatherBluetoothManager;
    private ShadowApplication shadowApplication;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Ensure adapter is enabled for tests
        Shadows.shadowOf(bluetoothAdapter).setEnabled(true);

        weatherBluetoothManager = new WeatherBluetoothManagerImpl(context, bluetoothAdapter);
        shadowApplication = Shadows.shadowOf((Application) context);

        // Grant permissions for tests
        shadowApplication.grantPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT);

        weatherBluetoothManager.registerReceivers();
    }

    private void idle() {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    @Test
    public void startDiscovery_UpdatesStatusAndClearsDevices() {
        weatherBluetoothManager.startDiscovery();
        idle();

        assertEquals("Discovering...", weatherBluetoothManager.getDiscoveryStatus().getValue());
        assertTrue(weatherBluetoothManager.getDiscoveredDevices().getValue().isEmpty());
    }

    @Test
    public void discoveryStartedBroadcast_UpdatesIsDiscovering() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        context.sendBroadcast(intent);
        idle();

        assertEquals(true, weatherBluetoothManager.isDiscovering().getValue());
        assertEquals("Discovering...", weatherBluetoothManager.getDiscoveryStatus().getValue());
    }

    @Test
    public void discoveryFinishedBroadcast_UpdatesIsDiscovering() {
        // Start first
        context.sendBroadcast(new Intent(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        idle();

        Intent intent = new Intent(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.sendBroadcast(intent);
        idle();

        assertEquals(false, weatherBluetoothManager.isDiscovering().getValue());
        assertEquals("Discovery Finished", weatherBluetoothManager.getDiscoveryStatus().getValue());
    }

    @Test
    public void deviceFoundBroadcast_AddsToDiscoveredDevices() {
        BluetoothDevice realDevice = bluetoothAdapter.getRemoteDevice("00:11:22:33:44:55");

        Intent intent = new Intent(BluetoothDevice.ACTION_FOUND);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, realDevice);
        context.sendBroadcast(intent);
        idle();

        List<BluetoothDevice> discovered =
                weatherBluetoothManager.getDiscoveredDevices().getValue();
        assertNotNull(discovered);
        assertFalse("List should not be empty", discovered.isEmpty());
        assertEquals("00:11:22:33:44:55", discovered.get(0).getAddress());
    }

    @Test
    public void bleScanResult_AddsToDiscoveredDevices() {
        BluetoothDevice bleDevice = bluetoothAdapter.getRemoteDevice("AA:BB:CC:DD:EE:FF");
        ScanResult result = mock(ScanResult.class);
        when(result.getDevice()).thenReturn(bleDevice);
        when(result.getRssi()).thenReturn(-60);

        weatherBluetoothManager.bleScanCallback.onScanResult(
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
        idle();

        List<BluetoothDevice> discovered =
                weatherBluetoothManager.getDiscoveredDevices().getValue();
        assertNotNull(discovered);
        boolean found = false;
        for (BluetoothDevice device : discovered) {
            if (device.getAddress().equals("AA:BB:CC:DD:EE:FF")) {
                found = true;
                break;
            }
        }
        assertTrue("BLE device should be in the discovered list", found);
        assertEquals(-60, weatherBluetoothManager.getDeviceRssi("AA:BB:CC:DD:EE:FF"));
    }
}
