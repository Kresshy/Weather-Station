package com.kresshy.weatherstation.activity;

import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.fragment.BluetoothDeviceListFragment;
import com.kresshy.weatherstation.fragment.DashboardFragment;
import com.kresshy.weatherstation.fragment.NavigationDrawerFragment;
import com.kresshy.weatherstation.fragment.SettingsFragment;
import com.kresshy.weatherstation.interfaces.WeatherListener;
import com.kresshy.weatherstation.weather.WeatherData;

import java.util.Set;


public class WeatherStationActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, BluetoothDeviceListFragment.OnFragmentInteractionListener,
        DashboardFragment.OnFragmentInteractionListener {

    private static final String TAG = "WeatherStationActivity";
    private static final boolean D = true;

    // Member fields
    private static ArrayAdapter<String> pairedDevicesArrayAdapter;
    private static ArrayAdapter<String> newDevicesArrayAdapter;
    private static Set<BluetoothDevice> pairedDevices;

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothDevice bluetoothDevice;
    private static BluetoothConnection bluetoothConnection;

    private static double weatherDataCount = 0;
    private static String connectedDeviceName;
    private static SharedPreferences sharedPreferences;
    private static String DEVICE_NAME;
    private CharSequence mTitle;
    private static boolean requestedEnableBluetooth = false;
    private static int orientation;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISABLE_BT = 2;

    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_STATE = 3;
    public static final int MESSAGE_CONNECTED = 4;

    private WeatherListener weatherListener;
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "ONCREATE");

        // setting up view
        setContentView(R.layout.activity_main);
        orientation = getResources().getConfiguration().orientation;

        // setting up navigation drawer fragment
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // setting up bluetooth adapter, service and broadcast receivers
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_LONG).show();
            finish();
        }

        bluetoothConnection = BluetoothConnection.getInstance(mHandler, getApplicationContext());

        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(bluetoothDiscoveryReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        newDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        if (bluetoothAdapter.isEnabled()) {
            pairedDevices = bluetoothAdapter.getBondedDevices();

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }

        // setting up sharedpreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "ONSTART");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (!bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Enabling bluetooth adapter");
            requestedEnableBluetooth = true;
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if (bluetoothAdapter.isEnabled() && bluetoothConnection.getState() != BluetoothConnection.State.connected) {
            bluetoothConnection.start();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "ONRESTOREINSTANCE");

        boolean isConnected = savedInstanceState.getBoolean(getString(R.string.PREFERENCE_CONNECTED));

        if (isConnected && bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "We should restore the connection");
            String address = sharedPreferences.getString(getString(R.string.PREFERENCE_DEVICE_ADDRESS), "00:00:00:00:00:00");

            if (address != "00:00:00:00:00:00")
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            else
                return;

            if (bluetoothConnection.getState() != BluetoothConnection.State.disconnected) {
                bluetoothConnection.start();
            }

            bluetoothConnection.connect(bluetoothDevice);
        } else {
            Log.i(TAG, "We shouldn't restore the connection");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "ONRESUME");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (!bluetoothAdapter.isEnabled() && !requestedEnableBluetooth) {
            Log.i(TAG, "Enabling bluetooth adapter");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (bluetoothAdapter.isEnabled()) {

            switch (bluetoothConnection.getState()) {
                case connected:
                    break;
                case connecting:
                    break;
                case disconnected:
                    bluetoothConnection.start();
                    break;
                case stopped:
                    bluetoothConnection.start();
                    break;
                default:
                    bluetoothConnection.start();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "ONSAVEDINSTANCE");

        if (bluetoothConnection.getState() == BluetoothConnection.State.connected) {
            Log.i(TAG, "Connected to a device");
            outState.putBoolean(getString(R.string.PREFERENCE_CONNECTED), true);
        } else {
            Log.i(TAG, "Not connected to a device");
            outState.putBoolean(getString(R.string.PREFERENCE_CONNECTED), false);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "ONPAUSE");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "ONDESTROY");

        bluetoothConnection.stop();
        BluetoothConnection.destroyInstance();

        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException ie) {
            Log.i(TAG, "BluetoothReceiver was not registered");
        }

        try {
            unregisterReceiver(bluetoothDiscoveryReceiver);
        } catch (IllegalArgumentException ie) {
            Log.i(TAG, "bluetoothDiscoveryRegister was not registered");
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();

        switch (position) {
            case 0:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new DashboardFragment())
                        .commit();

                break;
            case 1:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new BluetoothDeviceListFragment())
                        .commit();

                break;
            case 2:
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new SettingsFragment())
                        .commit();

                break;
            case 3:
                if (bluetoothAdapter.isEnabled()) {
                    Log.i(TAG, "Disabling bluetooth adapter");
                    bluetoothAdapter.disable();
                }

                finish();
                break;
        }

        onSectionAttached(position);
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.title_section1);
                break;
            case 1:
                mTitle = getString(R.string.title_section2);
                break;
            case 2:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment())
                    .commit();

            return true;
        } else if (id == R.id.action_quit) {
            if (bluetoothAdapter.isEnabled()) {
                Log.i(TAG, "Disabling bluetooth adapter");
                bluetoothAdapter.disable();
            }

            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String message = "";

            switch (msg.what) {

                case MESSAGE_TOAST:

                    message = (String) msg.obj;
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    break;

                case MESSAGE_READ:

                    message = (String) msg.obj;

                    // [start, pdu, end]
                    String pdu = message.split("_")[1];
                    String[] weather = pdu.split(" ");
                    double windSpeed = Double.parseDouble(weather[0]);
                    double temperature = Double.parseDouble(weather[1]);
                    WeatherData weatherData = new WeatherData(windSpeed, temperature);
                    Log.i(TAG, weatherData.toString());

                    weatherListener.weatherDataReceived(weatherData);
                    break;

                case MESSAGE_STATE:

                    BluetoothConnection.State state = (BluetoothConnection.State) msg.obj;

                    switch (state) {
                        case connecting:
                            Toast.makeText(getApplicationContext(), "Connecting to weather station", Toast.LENGTH_LONG).show();
                            break;
                        case disconnected:
                            Toast.makeText(getApplicationContext(), "Disconnected from weather station", Toast.LENGTH_LONG).show();
                            break;
                    }


                    break;

                case MESSAGE_CONNECTED:

                    Toast.makeText(getApplicationContext(), "Connected to weather station", Toast.LENGTH_LONG).show();
                    mNavigationDrawerFragment.selectItem(0);

                    break;
            }
        }
    };

    public ArrayAdapter<String> getPairedDevicesArrayAdapter() {
        return pairedDevicesArrayAdapter;
    }

    public ArrayAdapter<String> getNewDevicesArrayAdapter() {
        return newDevicesArrayAdapter;
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }


    public static Set<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    @Override
    public void onDeviceSelectedToConnect(String address) {

        sharedPreferences.edit().putString("CONNECTED_DEVICE_ADDRESS", address).commit();

        BluetoothDevice mBluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        Log.i(TAG, mBluetoothDevice.getName() + mBluetoothDevice.getAddress());

        bluetoothConnection.connect(mBluetoothDevice);
    }

    @Override
    public void startBluetoothDiscovery() {
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void stopBluetoothDiscovery() {
        bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void registerWeatherDataReceiver(WeatherListener weatherListener) {
        this.weatherListener = weatherListener;
    }


    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                Log.v(TAG, "RECEIVED BLUETOOTH STATE CHANGE: STATE_TURNING_ON");
            }

            if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                Log.v(TAG, "RECEIVED BLUETOOTH STATE CHANGE: STATE_ON");

                bluetoothConnection.start();

                pairedDevices = bluetoothAdapter.getBondedDevices();

                // If there are paired devices, add each one to the ArrayAdapter
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
            }
        }
    };
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver bluetoothDiscoveryReceiver = new BroadcastReceiver() {
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
                    newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
                setTitle("Select Device");
                if (newDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "None found";
                    newDevicesArrayAdapter.add(noDevices);
                    Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


}
