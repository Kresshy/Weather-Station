package com.kresshy.weatherstation.bluetooth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothAdapter;
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
public class BluetoothConnectionTest {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    @Mock private HardwareEventListener listener;
    @Mock private BluetoothDevice bluetoothDevice;

    private BluetoothConnection bluetoothConnection;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = ApplicationProvider.getApplicationContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothConnection = new BluetoothConnection(context, bluetoothAdapter);
        bluetoothConnection.setCallback(listener);
    }

    @Test
    public void start_updatesStateToDisconnected() {
        bluetoothConnection.start(listener);
        assertEquals(ConnectionState.disconnected, bluetoothConnection.getState());
        verify(listener).onConnectionStateChange(ConnectionState.disconnected);
    }

    @Test
    public void stop_updatesStateToStopped() {
        bluetoothConnection.start(listener);
        bluetoothConnection.stop();
        assertEquals(ConnectionState.stopped, bluetoothConnection.getState());
        verify(listener).onConnectionStateChange(ConnectionState.stopped);
    }

    @Test
    public void getState_returnsInitialStopped() {
        assertEquals(ConnectionState.stopped, bluetoothConnection.getState());
    }
}
