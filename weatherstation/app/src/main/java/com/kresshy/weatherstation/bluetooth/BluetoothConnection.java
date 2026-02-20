package com.kresshy.weatherstation.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Parcelable;

import androidx.core.app.ActivityCompat;

import com.kresshy.weatherstation.connection.Connection;
import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.connection.RawDataCallback;

import timber.log.Timber;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

/**
 * Manages Bluetooth Classic (RFCOMM) connections to the Weather Station. Uses background threads to
 * handle accepting, connecting, and data transmission.
 */
public class BluetoothConnection implements Connection {
    private ConnectionState state;
    private final Context context;
    private RawDataCallback callback;

    private static final String NAME = "WeatherStation";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice bluetoothDevice = null;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Future<?> acceptFuture;
    private Future<?> connectFuture;
    private Future<?> connectedFuture;

    private AcceptRunnable acceptRunnable;
    private ConnectRunnable connectRunnable;
    private ConnectedRunnable connectedRunnable;

    private final BluetoothAdapter bluetoothAdapter;

    /**
     * @param context The application context.
     * @param bluetoothAdapter Injected Bluetooth adapter.
     */
    @Inject
    public BluetoothConnection(
            @dagger.hilt.android.qualifiers.ApplicationContext Context context,
            BluetoothAdapter bluetoothAdapter) {
        this.state = ConnectionState.stopped;
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    /**
     * Starts the connection service, listening for incoming connections.
     *
     * @param callback The callback to notify.
     */
    public synchronized void start(RawDataCallback callback) {
        Timber.d("START SERVICE");

        cancelTasks();

        // Start the task to listen on a BluetoothServerSocket
        acceptRunnable = new AcceptRunnable();
        acceptFuture = executorService.submit(acceptRunnable);
        Timber.d("SUBMIT AcceptRunnable");

        state = ConnectionState.disconnected;
        callback.onConnectionStateChange(ConnectionState.disconnected);
    }

    /**
     * Attempts to connect to a specific Bluetooth device.
     *
     * @param device The remote BluetoothDevice.
     * @param callback The callback to notify.
     */
    public synchronized void connect(Parcelable device, RawDataCallback callback) {
        Timber.d("connect to: " + device);

        state = ConnectionState.connecting;
        bluetoothDevice = (BluetoothDevice) device;

        cancelTasks();

        // Start the task to connect with the given device
        connectRunnable = new ConnectRunnable(bluetoothDevice);
        connectFuture = executorService.submit(connectRunnable);
        Timber.d("SUBMIT ConnectRunnable " + device);

        callback.onConnectionStateChange(ConnectionState.connecting);
    }

    /**
     * Transition to the connected state once a socket is established.
     *
     * @param socket The active BluetoothSocket.
     */
    public synchronized void connected(BluetoothSocket socket) {
        Timber.d("connected");

        // Cancel the thread that completed the connection attempt
        if (connectFuture != null) {
            connectFuture.cancel(true);
            connectFuture = null;
            connectRunnable = null; // Do NOT call cancel() as it closes the socket
        }

        // Cancel any thread currently running a connection
        if (connectedFuture != null) {
            connectedFuture.cancel(true);
            if (connectedRunnable != null) connectedRunnable.cancel();
            connectedFuture = null;
            connectedRunnable = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (acceptFuture != null) {
            acceptFuture.cancel(true);
            if (acceptRunnable != null) acceptRunnable.cancel();
            acceptFuture = null;
            acceptRunnable = null;
        }

        // Start the task to manage the connection and perform transmissions
        connectedRunnable = new ConnectedRunnable(socket);
        connectedFuture = executorService.submit(connectedRunnable);
        Timber.d("SUBMIT ConnectedRunnable");

        state = ConnectionState.connected;
        callback.onConnectionStateChange(ConnectionState.connected);
    }

    /** Stops all active threads and closes sockets. */
    public synchronized void stop() {
        Timber.d("stop");
        cancelAllTasks();
        state = ConnectionState.stopped;
        callback.onConnectionStateChange(ConnectionState.stopped);
    }

    private void cancelAllTasks() {
        if (connectFuture != null) {
            connectFuture.cancel(true);
            if (connectRunnable != null) connectRunnable.cancel();
            connectFuture = null;
            connectRunnable = null;
        }

        if (connectedFuture != null) {
            connectedFuture.cancel(true);
            if (connectedRunnable != null) connectedRunnable.cancel();
            connectedFuture = null;
            connectedRunnable = null;
        }

        if (acceptFuture != null) {
            acceptFuture.cancel(true);
            if (acceptRunnable != null) acceptRunnable.cancel();
            acceptFuture = null;
            acceptRunnable = null;
        }
    }

    private void cancelTasks() {
        cancelAllTasks();
    }

    /**
     * Sends data to the connected device.
     *
     * @param out The byte array to transmit.
     */
    public void write(byte[] out) {
        ConnectedRunnable r;
        synchronized (this) {
            if (state != ConnectionState.connected) return;
            r = connectedRunnable;
        }
        if (r != null) {
            r.write(out);
        }
    }

    /**
     * @return Current connection state.
     */
    public synchronized ConnectionState getState() {
        return state;
    }

    @Override
    public void setCallback(RawDataCallback callback) {
        this.callback = callback;
    }

    /** Thread that listens for incoming RFCOMM connections. */
    private class AcceptRunnable implements Runnable {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptRunnable() {
            BluetoothServerSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    callback.onLogMessage("AcceptRunnable, Missing Permissions: BLUETOOTH_CONNECT");
                }
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Timber.e("Accept Thread " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            while (state != ConnectionState.connected) {
                try {
                    if (mmServerSocket == null) break;
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Timber.e("AcceptRunnable: " + e.getMessage());
                    break;
                }
                if (socket != null) {
                    Timber.d("Connected");
                    connected(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (mmServerSocket != null) mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Thread that attempts to initiate an outgoing RFCOMM connection. */
    private class ConnectRunnable implements Runnable {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectRunnable(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            this.device = device;
            try {
                if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    callback.onLogMessage(
                            "ConnectRunnable Constructor, Missing Permissions: BLUETOOTH_CONNECT");
                }
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Timber.d("Cannot create RfcommSocket " + e.getMessage());
            }
            socket = tmp;
        }

        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                callback.onToastMessage("Missing Permissions: BLUETOOTH_SCAN");
            }
            bluetoothAdapter.cancelDiscovery();

            try {
                if (socket == null) return;
                socket.connect();
                Timber.d("CONNECT_OK");
                connected(socket);
            } catch (IOException connectException) {
                Timber.e("CONNECT_FAIL " + connectException.getMessage() + " ATTEMPTING FALLBACK");
                try {
                    BluetoothSocket fallbackSocket =
                            device.createRfcommSocketToServiceRecord(MY_UUID);
                    fallbackSocket.connect();
                    connected(fallbackSocket);
                } catch (IOException connectException2) {
                    Timber.e("FALLBACK CONNECT_FAIL " + connectException2.getMessage());
                    callback.onToastMessage("Failed to connect...");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Timber.e("Closing the socket");
                    }
                }
            }
        }

        public void cancel() {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                Timber.e("Failed to close the socket " + e.getMessage());
            }
        }
    }

    /** Thread that manages an established RFCOMM connection, reading and writing data. */
    private class ConnectedRunnable implements Runnable {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private volatile boolean isRunning = true;

        public ConnectedRunnable(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpInputStream = null;
            OutputStream tmpOutputStream = null;
            callback.onConnected();

            try {
                tmpInputStream = socket.getInputStream();
                tmpOutputStream = socket.getOutputStream();
                Timber.d("STREAMS_OK");
            } catch (IOException e) {
                Timber.e("STREAMS_FAIL " + e.getMessage());
            }

            inputStream = tmpInputStream;
            outputStream = tmpOutputStream;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder curMsg = new StringBuilder();
            String end = "_end";

            while (isRunning) {
                try {
                    if (!socket.isConnected()) throw new IOException();

                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        curMsg.append(new String(buffer, 0, bytes, Charset.forName("UTF-8")));
                        int endIdx = curMsg.indexOf(end);

                        while (endIdx != -1) {
                            String fullMessage = curMsg.substring(0, endIdx + end.length());
                            Timber.d("New weather data available " + fullMessage);
                            curMsg.delete(0, endIdx + end.length());
                            callback.onRawDataReceived(fullMessage);
                            endIdx = curMsg.indexOf(end);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    Timber.e("Connection lost: " + e.getMessage());
                    isRunning = false;
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                if (outputStream != null) {
                    outputStream.write(bytes);
                    Timber.d("WRITE_OK");
                }
            } catch (IOException e) {
                Timber.e("WRITE_FAIL " + e.getMessage());
            }
        }

        public void cancel() {
            isRunning = false;
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                Timber.e("Failed to close the socket " + e.getMessage());
            }
        }
    }
}
