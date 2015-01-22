package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.kresshy.weatherstation.activity.WeatherStationActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;


public class BluetoothConnection {

	// Debugging
	private static final String TAG = "BluetoothConnection";
	private static final boolean D = true;

    private static BluetoothConnection instance = null;
	private State state = State.disconnected;
    private static Context context;

	private static BluetoothAdapter mBluetoothAdapter;
	private static BluetoothServerSocket mBluetoothServerSocket;
	private static BluetoothSocket mBluetoothSocket;
	private static BluetoothDevice mBluetoothDevice;

	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private Handler mHandler;

	private static String NAME = "SIDUS";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	protected BluetoothConnection(Handler handler, Context context) {
		this.state = State.stopped;
		this.mBluetoothDevice = null;
		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.mBluetoothServerSocket = null;
		this.mBluetoothSocket = null;
		this.mHandler = handler;
        this.context = context;
	}

    public static synchronized BluetoothConnection getInstance(Handler handler, Context context) {
        if (instance == null) {
            instance = new BluetoothConnection(handler, context);
            Log.i(TAG, "Create new instance");
        } else {
            instance.setHandler(handler);
            Log.i(TAG, "Return existing instance");
        }

        return instance;
    }

    public static synchronized void destroyInstance() {
        instance = null;
    }

	public synchronized void setHandler(Handler handler) {
		mHandler = handler;
	}

	public synchronized void start() {
		if (D)
			Log.d(TAG, "START SERVICE");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
			if (D)
				Log.d(TAG, "CANCEL ConnectThread");
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
			if (D)
				Log.d(TAG, "CANCEL ConnectedThread");
		}

		// Start the thread to listen on a BluetoothServerSocket
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
			if (D)
				Log.d(TAG, "START AcceptThread");
		}

		state = State.disconnected;
		mHandler.obtainMessage(WeatherStationActivity.MESSAGE_STATE, -1, -1, State.disconnected).sendToTarget();
	}

	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connect to: " + device);

        state = State.connecting;
        mBluetoothDevice = device;

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
			if (D)
				Log.d(TAG, "Cancel any thread attempting to make a connection");
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
			if (D)
				Log.d(TAG, "Cancel any thread currently running a connection");
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		if (D)
			Log.d(TAG, "START ConnectThread " + device);

		mHandler.obtainMessage(WeatherStationActivity.MESSAGE_STATE, -1, -1, State.connecting).sendToTarget();
	}

	public synchronized void connected(BluetoothSocket socket) {
		if (D)
			Log.d(TAG, "connected");

		// mHandler.obtainMessage(WeatherStationActivity.MESSAGE_TOAST, -1, -1,
		// "Connected").sendToTarget();
		// Cancel the thread that completed the connection
		// if (mConnectThread != null) {
		// mConnectThread.cancel();
		// mConnectThread = null;
		// }

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
			if (D)
				Log.d(TAG, "Cancel any thread currently connected");
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
			if (D)
				Log.d(TAG, "Cancel the accept thread");
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		if (D)
			Log.d(TAG, "START ConnectedThread");

		state = State.connected;
		mHandler.obtainMessage(WeatherStationActivity.MESSAGE_STATE, -1, -1, State.connected).sendToTarget();
	}

	public synchronized void stop() {
		if (D)
			Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
			if (D)
				Log.d(TAG, "STOP ConnectThread");
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
			if (D)
				Log.d(TAG, "STOP ConnectedThread");
		}

		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
			if (D)
				Log.d(TAG, "STOP AccceptThread");
		}

		state = State.stopped;
		mHandler.obtainMessage(WeatherStationActivity.MESSAGE_STATE, -1, -1, State.stopped).sendToTarget();
	}

	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;

		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mConnectedThread != null)
				;
			r = mConnectedThread;
		}

		// Perform the write unsynchronized
		r.write(out);
	}

	public synchronized State getState() {
		return state;
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
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
					Log.i(TAG, "Connected");
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

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
                e.printStackTrace();
			}
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
        private BluetoothSocket fallbackSocket;
        private boolean fallback = false;
		@SuppressWarnings("unused")
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				Log.i(TAG, "RFCOMM_OK");
			} catch (IOException e) {
                Log.i(TAG, "Cannot create RfcommSocket " + e.getMessage());
			}
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
				Log.i(TAG, "CONNECT_OK");

			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					Log.e(TAG, "CONNECT_FAIL " + connectException.getMessage() + " RECONNECT");
                    fallback = true;

                    fallbackSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    fallbackSocket.connect();

				} catch (IOException connectException2) {
                    Log.e(TAG, "2ND CONNECT_FAIL " + connectException2.getMessage());

                    try {
                        mHandler.obtainMessage(WeatherStationActivity.MESSAGE_TOAST, -1, -1, "Failed to reconnect...").sendToTarget();
                        mmSocket.close();
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
			    connected(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
                Log.e(TAG, "Failed to close the socket " + e.getMessage());
			}
		}

	}

	private class ConnectedThread extends Thread {
		private BluetoothSocket mmSocket;
		private InputStream mmInStream;
		private OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			mHandler.obtainMessage(WeatherStationActivity.MESSAGE_CONNECTED, -1, -1, state).sendToTarget();

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

		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes = 0; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {

                    String end = "_end";
                    StringBuilder curMsg = new StringBuilder();

                    if (D)
                        Log.i(TAG, "Before reading weather data part");

                    if (!mmSocket.isConnected()) {
                        throw new IOException();
                    }

                    while (-1 != (bytes = mmInStream.read(buffer))) {

                        curMsg.append(new String(buffer, 0, bytes, Charset.forName("UTF-8")));
                        int endIdx = curMsg.indexOf(end);
                        if (D)
                            Log.i(TAG, "After reading weather data part");

                        if (endIdx != -1) {
                            if (D)
                                Log.i(TAG, "New weather data available");
                            String fullMessage = curMsg.substring(0, endIdx + end.length());
                            curMsg.delete(0, endIdx + end.length());
                            mHandler.obtainMessage(WeatherStationActivity.MESSAGE_READ, bytes, -1, fullMessage).sendToTarget();
                        }
                    }

                    if (D)
                        Log.i(TAG, "Sleeping the thread for 100");

                    Thread.sleep(100);

				} catch (IOException e) {
                    e.printStackTrace();
                    break;
				} catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                } finally {
                    /*
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    String address = sharedPreferences.getString("CONNECTED_DEVICE_ADDRESS", "00:00:00:00:00:00");
                    if (address != "00:00:00:00:00:00")
                        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);

                    connect(mBluetoothDevice);
                    */
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

    public static enum State {
        disconnected, connected, connecting, stopped
    }

}
