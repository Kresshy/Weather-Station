package com.kresshy.weatherstation.connection;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Parcelable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ConnectionManager}. Verifies that the manager correctly delegates calls to
 * the underlying {@link Connection} implementation.
 */
public class ConnectionManagerTest {

    private ConnectionManager connectionManager;
    private Context context;
    private RawDataCallback callback;
    private Connection connection;
    private MockedStatic<ConnectionFactory> mockedFactory;

    @Before
    public void setUp() {
        context = mock(Context.class);
        callback = mock(RawDataCallback.class);
        connection = mock(Connection.class);

        mockedFactory = Mockito.mockStatic(ConnectionFactory.class);
        mockedFactory
                .when(
                        () ->
                                ConnectionFactory.getConnection(
                                        any(Context.class), any(RawDataCallback.class)))
                .thenReturn(connection);

        connectionManager = new ConnectionManager(context, callback);
    }

    @After
    public void tearDown() {
        mockedFactory.close();
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
