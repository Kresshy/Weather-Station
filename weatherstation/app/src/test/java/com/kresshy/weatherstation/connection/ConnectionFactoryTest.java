package com.kresshy.weatherstation.connection;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.preference.PreferenceManager;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ConnectionFactory}. Verifies that the factory produces the correct {@link
 * Connection} implementation based on the 'pref_simulator_mode' setting.
 */
public class ConnectionFactoryTest {

    private Context context;
    private RawDataCallback callback;
    private MockedStatic<PreferenceManager> mockedPreferenceManager;
    private MockedStatic<android.bluetooth.BluetoothAdapter> mockedBluetoothAdapter;
    private android.content.SharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        context = mock(Context.class);
        callback = mock(RawDataCallback.class);
        sharedPreferences = mock(android.content.SharedPreferences.class);

        mockedPreferenceManager = Mockito.mockStatic(PreferenceManager.class);
        mockedPreferenceManager
                .when(() -> PreferenceManager.getDefaultSharedPreferences(any(Context.class)))
                .thenReturn(sharedPreferences);

        mockedBluetoothAdapter = Mockito.mockStatic(android.bluetooth.BluetoothAdapter.class);
        mockedBluetoothAdapter
                .when(android.bluetooth.BluetoothAdapter::getDefaultAdapter)
                .thenReturn(mock(android.bluetooth.BluetoothAdapter.class));
    }

    @After
    public void tearDown() {
        mockedPreferenceManager.close();
        mockedBluetoothAdapter.close();
    }

    @Test
    public void getConnection_SimulatorModeOn_ReturnsSimulatorConnection() {
        when(sharedPreferences.getBoolean("pref_simulator_mode", false)).thenReturn(true);

        Connection connection = ConnectionFactory.getConnection(context, callback);

        assertTrue(connection instanceof SimulatorConnection);
    }

    @Test
    public void getConnection_SimulatorModeOff_ReturnsBluetoothConnection() {
        when(sharedPreferences.getBoolean("pref_simulator_mode", false)).thenReturn(false);

        Connection connection = ConnectionFactory.getConnection(context, callback);

        assertTrue(connection instanceof BluetoothConnection);
    }
}
