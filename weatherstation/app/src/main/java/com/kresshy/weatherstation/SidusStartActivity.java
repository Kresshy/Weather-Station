package com.kresshy.weatherstation;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kresshy.weatherstation.bluetooth.BluetoothService;
import com.kresshy.weatherstation.bluetooth.BluetoothService.State;

import java.util.Set;


public class SidusStartActivity extends ActionBarActivity {

	private final String TAG = "Start_Activity";

	// Member fields
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;
	private static BluetoothAdapter mBluetoothAdapter;
	private static BluetoothDevice mBluetoothDevice;
    private static Set<BluetoothDevice> pairedDevices;
	public static String mConnectedDeviceName;
	public static String DEVICE_NAME;

	private static final int REQUEST_ENABLE_BT = 1;

	public static final int MESSAGE_TOAST = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_STATE = 3;
	public static final int MESSAGE_CONNECTED = 4;

	private Button skipButton;
	private RelativeLayout rootRelativeLayout;

	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
				Log.v(TAG, "RECEIVED BLUETOOTH STATE CHANGE: STATE_TURNING_ON");
			}

			if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
				Log.v(TAG, "RECEIVED BLUETOOTH STATE CHANGE: STATE_ON");

				pairedDevices = mBluetoothAdapter.getBondedDevices();

				// If there are paired devices, add each one to the ArrayAdapter
				if (pairedDevices.size() > 0) {
					findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
					for (BluetoothDevice device : pairedDevices) {
						mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
					}
				}
			}
		}
	};

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
				setTitle("Select Device");
				if (mNewDevicesArrayAdapter.getCount() == 0) {
					// String noDevices = "None found";
					// mNewDevicesArrayAdapter.add(noDevices);
					Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	// The on-click listener for all devices in the ListViews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			// Cancel discovery because it's costly and we're about to connect
			mBluetoothAdapter.cancelDiscovery();

			// Get the device MAC address, which is the last 17 chars in the
			// View
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);

			mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
			Log.i(TAG, mBluetoothDevice.getName() + mBluetoothDevice.getAddress());

			((WeatherStationApplication) getApplication()).getConnectionService().connect(mBluetoothDevice);
		}
	};

	private OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.skip:
				Intent startProgramActivity = new Intent(getApplicationContext(), SidusProgramActivity.class);
				startActivity(startProgramActivity);
				break;

			default:
				break;
			}

		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		Log.v(TAG, "ONSTART");

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}

		if (mBluetoothAdapter.isEnabled() && ((WeatherStationApplication) getApplication()).getConnectionService() == null) {
			((WeatherStationApplication) getApplication()).setConnectionService(new BluetoothService(mHandler));
			((WeatherStationApplication) getApplication()).getConnectionService().start();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_start);
		Log.v(TAG, "ONCREATE");

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

		setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
		
		rootRelativeLayout = (RelativeLayout) findViewById(R.id.startactivity_relativelayout);
		rootRelativeLayout.setBackgroundColor(Color.argb((int) 25, 0, 0, 0));

		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

		// Find and set up the ListView for paired devices
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);
		pairedListView.setCacheColorHint(Color.TRANSPARENT);
		pairedListView.requestFocus(0);

		// Find and set up the ListView for newly discovered devices
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);
		newDevicesListView.setCacheColorHint(Color.TRANSPARENT);
		newDevicesListView.requestFocus(0);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		if (mBluetoothAdapter.isEnabled()) {
			pairedDevices = mBluetoothAdapter.getBondedDevices();

			// If there are paired devices, add each one to the ArrayAdapter
			if (pairedDevices.size() > 0) {
				findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
				for (BluetoothDevice device : pairedDevices) {
					mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				}
			}
		}

		skipButton = (Button) findViewById(R.id.skip);
		skipButton.setOnClickListener(onClickListener);

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (((WeatherStationApplication) getApplication()).getConnectionService() != null) {
			((WeatherStationApplication) getApplication()).getConnectionService().stop();
		}

		// Make sure we're not doing discovery anymore
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		this.unregisterReceiver(mReceiver);
		unregisterReceiver(bluetoothReceiver);

	}

	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

			case MESSAGE_TOAST:

				String message = (String) msg.obj;
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

				break;

			case MESSAGE_READ:

				// byte[] readBuf = (byte[]) msg.obj;
				// int paramInt = msg.arg1;

				// Toast.makeText(getApplicationContext(), "message",
				// Toast.LENGTH_SHORT).show();

				break;

			case MESSAGE_STATE:

				((WeatherStationApplication) getApplication()).setState((State) msg.obj);

				break;

			case MESSAGE_CONNECTED:

				Toast.makeText(getApplicationContext(), "Connected to timer", Toast.LENGTH_LONG).show();

				((WeatherStationApplication) getApplication()).setState((State) msg.obj);

				Intent startProgramActivity = new Intent(getApplicationContext(), SidusProgramActivity.class);
				startActivity(startProgramActivity);

				break;
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {

		case REQUEST_ENABLE_BT:

			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), "Cannot enable Bluetooth", Toast.LENGTH_LONG).show();
				break;
			}

			if (resultCode == RESULT_OK) {
				Toast.makeText(getApplicationContext(), "Bluetooth is enabled", Toast.LENGTH_LONG).show();

				if (mBluetoothAdapter.isEnabled() && ((WeatherStationApplication) getApplication()).getConnectionService() == null) {
					((WeatherStationApplication) getApplication()).setConnectionService(new BluetoothService(mHandler));
					((WeatherStationApplication) getApplication()).getConnectionService().start();
				}

				break;
			}

			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.start, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_scan:
			mBluetoothAdapter.startDiscovery();
			setSupportProgressBarIndeterminateVisibility(Boolean.TRUE);
			break;

		default:
			break;
		}

		return true;
	}

}
