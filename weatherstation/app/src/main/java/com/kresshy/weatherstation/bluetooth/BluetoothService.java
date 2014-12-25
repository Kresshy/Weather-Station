package com.kresshy.weatherstation.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.kresshy.weatherstation.MainActivity;
import com.kresshy.weatherstation.SidusStartActivity;


public class BluetoothService {

	// Debugging
	private static final String TAG = "ConnectionService";
	private static final boolean D = true;

	public static enum State {
		disconnected, connected, connecting
	};

	private State state = State.disconnected;

	private static BluetoothAdapter mBluetoothAdapter;
	@SuppressWarnings("unused")
	private static BluetoothServerSocket mBluetoothServerSocket;
	@SuppressWarnings("unused")
	private static BluetoothSocket mBluetoothSocket;
	@SuppressWarnings("unused")
	private static BluetoothDevice mBluetoothDevice;

	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private Handler mHandler;

	private static String NAME = "SIDUS";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	public BluetoothService(Handler handler) {
		state = State.disconnected;
		mBluetoothDevice = null;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBluetoothServerSocket = null;
		mBluetoothSocket = null;
		mHandler = handler;
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
		mHandler.obtainMessage(SidusStartActivity.MESSAGE_STATE, -1, -1, State.disconnected).sendToTarget();
	}

	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connect to: " + device);

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

		state = State.connecting;
		mHandler.obtainMessage(SidusStartActivity.MESSAGE_STATE, -1, -1, State.connecting).sendToTarget();
	}

	public synchronized void connected(BluetoothSocket socket) {
		if (D)
			Log.d(TAG, "connected");

		// mHandler.obtainMessage(MainActivity.MESSAGE_TOAST, -1, -1,
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
		mHandler.obtainMessage(SidusStartActivity.MESSAGE_STATE, -1, -1, State.connected).sendToTarget();
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

		state = State.disconnected;
		mHandler.obtainMessage(SidusStartActivity.MESSAGE_STATE, -1, -1, State.disconnected).sendToTarget();
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
			}
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
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
					Log.e(TAG, "CONNECT_FAIL " + connectException.getMessage());

					mmSocket.close();
				} catch (IOException closeException) {
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			connected(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
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
			mHandler.obtainMessage(SidusStartActivity.MESSAGE_CONNECTED, -1, -1, state).sendToTarget();

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
			byte[] buffer = new byte[2048]; // buffer store for the stream

			int bytes = 0; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {

					// Read from the InputStream
					bytes = mmInStream.read(buffer);

					// Send the obtained bytes to the UI activity
					mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();

					try {

						Thread.sleep(30);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				} catch (IOException e) {
					break;
				}

			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
				Log.i(TAG, "WRITE_OK");
			} catch (IOException e) {

				Log.e(TAG, "WRITE_FAIL " + e.getMessage());
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}
}
