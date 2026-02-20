package com.kresshy.weatherstation.connection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Parcelable;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConnectionManager}. Verifies that the manager correctly delegates calls to
 * the underlying {@link Connection} implementation.
 */
public class ConnectionManagerTest {

    private ConnectionManager connectionManager;
    private RawDataCallback callback;
    private Connection connection;

    @Before
    public void setUp() {
        callback = mock(RawDataCallback.class);
        connection = mock(Connection.class);
        connectionManager = new ConnectionManager(connection);
        connectionManager.setCallback(callback);
    }

    @Test
    public void startConnection_CallsStartOnConnection() {
        connectionManager.startConnection();
        verify(connection).start(callback);
    }

    @Test
    public void stopConnection_CallsStopOnConnection() {
        connectionManager.stopConnection();
        verify(connection).stop();
    }

    @Test
    public void connectToDevice_CallsConnectOnConnection() {
        Parcelable device = mock(Parcelable.class);
        connectionManager.connectToDevice(device);
        verify(connection).connect(device, callback);
    }

    @Test
    public void getConnectionState_ReturnsStateFromConnection() {
        when(connection.getState()).thenReturn(ConnectionState.connected);
        assertEquals(ConnectionState.connected, connectionManager.getConnectionState());
    }
}
