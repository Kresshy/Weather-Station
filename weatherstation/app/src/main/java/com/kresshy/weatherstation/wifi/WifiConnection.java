package com.kresshy.weatherstation.wifi;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.connection.Connection;
import com.kresshy.weatherstation.utils.ConnectionState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;


public class WifiConnection implements Connection {

    private static final String TAG = "WiFiConnection";

    private static WifiConnection instance = null;
    private static ConnectionState state = ConnectionState.disconnected;
    private Context context;

    private static SharedPreferences sharedPreferences;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler handler;

    private static WifiManager wifiManager;
    private WifiDevice wifiDevice;
    private Socket socket;

    int network;

    protected WifiConnection(Handler handler, Context context) {
        this.context = context;
        this.handler = handler;
        wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized WifiConnection getInstance(Handler handler, Context context) {
        if (instance == null) {
            return new WifiConnection(handler, context);
        } else {
            return instance;
        }
    }

    public static synchronized void destroyInstance() {
        instance = null;
    }

    public synchronized void setHandler(Handler handler) {
        this.handler = handler;
    }

    public synchronized void start() {
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

        state = ConnectionState.disconnected;
        handler.obtainMessage(WSConstants.MESSAGE_STATE, -1, -1, ConnectionState.disconnected).sendToTarget();
    }

    public synchronized void connect(Parcelable device) {
        Log.d(TAG, "connect to: " + device);

        wifiDevice = (WifiDevice) device;
        state = ConnectionState.connecting;

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
        connectThread = new ConnectThread(wifiDevice);
        connectThread.start();
        Log.d(TAG, "START ConnectThread " + device);

        handler.obtainMessage(WSConstants.MESSAGE_STATE, -1, -1, ConnectionState.connecting).sendToTarget();
    }

    public synchronized void connected(Socket socket) {
        Log.d(TAG, "connected");

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            Log.d(TAG, "Cancel any thread currently connected");
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

    private class ConnectThread extends Thread {
        private final WifiDevice mmDevice;
        private Socket mmSocket;

        public ConnectThread(WifiDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            mmDevice = device;
        }

        public void run() {

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket = new Socket(mmDevice.getIp(), mmDevice.getPort());
                Log.i(TAG, "CONNECT_OK");

            } catch (IOException connectException) {
                connectException.printStackTrace();
            }

            connected(mmSocket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close the socket " + e.getMessage());
            }
        }

    }

    private class ConnectedThread extends Thread {
        private Socket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectedThread(Socket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            handler.obtainMessage(WSConstants.MESSAGE_CONNECTED, -1, -1, state).sendToTarget();

            try {

                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.i(TAG, "STREAMS_OK");
            } catch (IOException e) {
                Log.e(TAG, "STREAMS_FAIL " + e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes = 0; // bytes returned from read()

            Log.i(TAG, "Runing Connected Thread");

            while (true) {
                try {

                    String end = "_end";
                    StringBuilder curMsg = new StringBuilder();

                    if (!mmSocket.isConnected()) {
                        throw new IOException();
                    }

                    while (-1 != (bytes = mmInStream.read(buffer))) {
                        Log.i(TAG, "Read from inputstream");

                        curMsg.append(new String(buffer, 0, bytes, Charset.forName("UTF-8")));
                        Log.i(TAG, "Append to current message" + curMsg);

                        int endIdx = curMsg.indexOf(end);
                        if (endIdx != -1) {
                            Log.i(TAG, "Found endIdx");
                            String fullMessage = curMsg.substring(0, endIdx + end.length());
                            Log.i(TAG, "New weather data available " + fullMessage);
                            curMsg.delete(0, endIdx + end.length());
                            handler.obtainMessage(WSConstants.MESSAGE_READ, bytes, -1, fullMessage).sendToTarget();
                        } else {
                            Log.i(TAG, "NOT Found endIdx");
                        }
                    }

                    Thread.sleep(100);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Log.i(TAG, "WRITE_OK");
            } catch (IOException e) {
                // TODO here we should reconnect to the device if the stream is interrupted
                Log.e(TAG, "WRITE_FAIL " + e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close the socket " + e.getMessage());
            }
        }
    }
}
