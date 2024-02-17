package com.kresshy.weatherstation.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;

import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.connection.Connection;
import com.kresshy.weatherstation.utils.ConnectionState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import timber.log.Timber;


public class BluetoothConnection implements Connection {

    // Debugging
    private static final String TAG = "BluetoothConnection";

    private static BluetoothConnection instance = null;
    private ConnectionState state = ConnectionState.disconnected;
    private static Context context;

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothServerSocket bluetoothServerSocket;
    private static BluetoothSocket bluetoothSocket;
    private static BluetoothDevice bluetoothDevice;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler handler;

    private static String NAME = "SIDUS";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    protected BluetoothConnection(Handler handler, Context context) {
        this.state = ConnectionState.stopped;
        this.bluetoothDevice = null;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bluetoothServerSocket = null;
        this.bluetoothSocket = null;
        this.handler = handler;
        this.context = context;
    }

    public static synchronized BluetoothConnection getInstance(Handler handler, Context context) {
        if (instance == null) {
            instance = new BluetoothConnection(handler, context);
        } else {
            instance.setHandler(handler);
        }

        return instance;
    }

    public static synchronized void destroyInstance() {
        instance = null;
    }

    public synchronized void setHandler(Handler handler) {
        this.handler = handler;
    }

    public synchronized void start() {
        Log.d(TAG, "START SERVICE");

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
            Log.d(TAG, "CANCEL ConnectThread");
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            Log.d(TAG, "CANCEL ConnectedThread");
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
            Log.d(TAG, "START AcceptThread");
        }

        state = ConnectionState.disconnected;
        handler.obtainMessage(WSConstants.MESSAGE_STATE, -1, -1, ConnectionState.disconnected).sendToTarget();
    }

    public synchronized void connect(Parcelable device) {
        Log.d(TAG, "connect to: " + device);

        state = ConnectionState.connecting;
        bluetoothDevice = (BluetoothDevice) device;

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
            Log.d(TAG, "Cancel any thread attempting to make a connection");
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            Log.d(TAG, "Cancel any thread currently running a connection");
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();
        Log.d(TAG, "START ConnectThread " + device);

        handler.obtainMessage(WSConstants.MESSAGE_STATE, -1, -1, ConnectionState.connecting).sendToTarget();
    }

    public synchronized void connected(BluetoothSocket socket) {
        Log.d(TAG, "connected");

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            Log.d(TAG, "Cancel any thread currently connected");
        }

        // Cancel the accept thread because we only want to connect to one
        // device
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
            Log.d(TAG, "Cancel the accept thread");
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        Log.d(TAG, "START ConnectedThread");

        state = ConnectionState.connected;
        handler.obtainMessage(WSConstants.MESSAGE_STATE, -1, -1, ConnectionState.connected).sendToTarget();
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
            Log.d(TAG, "STOP ConnectThread");
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            Log.d(TAG, "STOP ConnectedThread");
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
            Log.d(TAG, "STOP AccceptThread");
        }

        state = ConnectionState.stopped;
        handler.obtainMessage(WSConstants.MESSAGE_STATE, -1, -1, ConnectionState.stopped).sendToTarget();
    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;

        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (connectedThread != null)
                ;
            r = connectedThread;
        }

        // Perform the write unsynchronized
        r.write(out);
    }

    public synchronized ConnectionState getState() {
        return state;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client
                // code
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Accept Thread " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: " + e.getMessage());
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    Timber.d( "Connected");
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

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private BluetoothSocket fallbackSocket;
        private boolean fallback = false;
        @SuppressWarnings("unused")
        private final BluetoothDevice device;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to socket,
            // because socket is final
            BluetoothSocket tmp = null;
            this.device = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server
                // code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Timber.d( "RFCOMM_OK");
            } catch (IOException e) {
                Timber.d( "Cannot create RfcommSocket " + e.getMessage());
            }
            socket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                socket.connect();
                Timber.d( "CONNECT_OK");

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    Log.e(TAG, "CONNECT_FAIL " + connectException.getMessage() + " RECONNECT");
                    fallback = true;

                    fallbackSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    fallbackSocket.connect();

                } catch (IOException connectException2) {
                    Log.e(TAG, "2ND CONNECT_FAIL " + connectException2.getMessage());

                    try {
                        handler.obtainMessage(WSConstants.MESSAGE_TOAST, -1, -1, "Failed to reconnect...").sendToTarget();
                        socket.close();
                        return;
                    } catch (IOException e) {
                        Log.e(TAG, "Closing the socket");
                    }
                }
            }

            // Do work to manage the connection (in a separate thread)
            if (fallback)
                connected(fallbackSocket);
            else
                connected(socket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close the socket " + e.getMessage());
            }
        }

    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpInputStream = null;
            OutputStream tmpOutputStream = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            handler.obtainMessage(WSConstants.MESSAGE_CONNECTED, -1, -1, state).sendToTarget();

            try {

                tmpInputStream = socket.getInputStream();
                tmpOutputStream = socket.getOutputStream();
                Timber.d( "STREAMS_OK");
            } catch (IOException e) {
                Log.e(TAG, "STREAMS_FAIL " + e.getMessage());
            }

            inputStream = tmpInputStream;
            outputStream = tmpOutputStream;
        }

        public void run() {
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes = 0; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    String end = "_end";
                    StringBuilder curMsg = new StringBuilder();

                    if (!socket.isConnected()) {
                        throw new IOException();
                    }

                    while (-1 != (bytes = inputStream.read(buffer))) {
                        curMsg.append(new String(buffer, 0, bytes, Charset.forName("UTF-8")));
                        int endIdx = curMsg.indexOf(end);

                        if (endIdx != -1) {
                            String fullMessage = curMsg.substring(0, endIdx + end.length());
                            Timber.d( "New weather data available " + fullMessage);
                            curMsg.delete(0, endIdx + end.length());
                            handler.obtainMessage(WSConstants.MESSAGE_READ, bytes, -1, fullMessage).sendToTarget();
                        }
                    }

                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                Timber.d( "WRITE_OK");
            } catch (IOException e) {
                // TODO here we should reconnect to the device if the stream is interrupted
                Log.e(TAG, "WRITE_FAIL " + e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close the socket " + e.getMessage());
            }
        }
    }
}
