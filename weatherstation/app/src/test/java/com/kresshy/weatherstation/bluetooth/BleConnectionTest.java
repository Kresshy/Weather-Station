package com.kresshy.weatherstation.bluetooth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.connection.HardwareEventListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S)
public class BleConnectionTest {

    private Context context;
    @Mock private HardwareEventListener listener;
    @Mock private BluetoothDevice bluetoothDevice;

    private BleConnection bleConnection;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = ApplicationProvider.getApplicationContext();
        bleConnection = new BleConnection(context);
    }

    @Test
    public void start_updatesStateToDisconnected() {
        bleConnection.start(listener);
        assertEquals(ConnectionState.disconnected, bleConnection.getState());
        verify(listener).onConnectionStateChange(ConnectionState.disconnected);
    }

    @Test
    public void stop_updatesStateToStopped() {
        bleConnection.start(listener);
        bleConnection.stop();
        assertEquals(ConnectionState.stopped, bleConnection.getState());
        verify(listener).onConnectionStateChange(ConnectionState.stopped);
    }
}
