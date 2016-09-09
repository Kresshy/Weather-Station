package com.kresshy.weatherstation.activity;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
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
import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.connection.BluetoothConnection;
import com.kresshy.weatherstation.connection.BluetoothDiscoveryReceiver;
import com.kresshy.weatherstation.connection.BluetoothStateReceiver;
import com.kresshy.weatherstation.connection.ConnectionManager;
import com.kresshy.weatherstation.connection.WifiDevice;
import com.kresshy.weatherstation.fragment.BluetoothDeviceListFragment;
import com.kresshy.weatherstation.fragment.DashboardFragment;
import com.kresshy.weatherstation.fragment.NavigationDrawerFragment;
import com.kresshy.weatherstation.fragment.SettingsFragment;
import com.kresshy.weatherstation.fragment.WifiFragment;
import com.kresshy.weatherstation.interfaces.WeatherListener;
import com.kresshy.weatherstation.utils.ConnectionState;
import com.kresshy.weatherstation.weather.WeatherData;

import java.util.Set;


public class WSActivity extends ActionBarActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        BluetoothDeviceListFragment.OnFragmentInteractionListener,
        DashboardFragment.OnFragmentInteractionListener,
        WifiFragment.OnFragmentInteractionListener {

    private static final String TAG = "WSActivity";

    // Member fields
    private static ArrayAdapter<String> bluetoothDevicesArrayAdapter;
    private static Set<BluetoothDevice> pairedDevices;
    private static ArrayAdapter<String> wifiDevicesArrayAdapter;

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothDevice bluetoothDevice;

    private static double weatherDataCount = 0;
    private static String connectedDeviceName;
    private static SharedPreferences sharedPreferences;
    private CharSequence fragmentTitle;
    private static boolean requestedEnableBluetooth = false;
    private static int orientation;

    private WeatherListener weatherListener;
    private NavigationDrawerFragment navigationDrawerFragment;
    private ConnectionManager connectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "ONCREATE");

        // setting up view
        setContentView(R.layout.activity_main);
        orientation = getResources().getConfiguration().orientation;

        // setting up navigation drawer fragment
        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        fragmentTitle = getTitle();

        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // setting up bluetooth adapter, service and broadcast receivers
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        connectionManager = ConnectionManager.getInstance(this, bluetoothDevicesArrayAdapter, messageHandler);

        // setting up sharedpreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);

        registerReceiver(BluetoothStateReceiver.getInstance(connectionManager.connection, bluetoothDevicesArrayAdapter, this, sharedPreferences), new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(BluetoothDiscoveryReceiver.getInstance(bluetoothDevicesArrayAdapter, this), new IntentFilter(BluetoothDevice.ACTION_FOUND));

        if (!requestedEnableBluetooth) {
            connectionManager.enableConnection();
            requestedEnableBluetooth = true;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "ONSTART");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (!requestedEnableBluetooth) {
            connectionManager.enableConnection();
        }
    }

//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        Log.i(TAG, "ONRESTOREINSTANCE");
//
//        boolean isConnected = savedInstanceState.getBoolean(getString(R.string.PREFERENCE_CONNECTED));
//
//        if (isConnected && bluetoothAdapter.isEnabled()) {
//            Log.i(TAG, "We should restore the connection");
//            String address = sharedPreferences.getString(getString(R.string.PREFERENCE_DEVICE_ADDRESS), "00:00:00:00:00:00");
//
//            if (address != "00:00:00:00:00:00")
//                bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
//            else
//                return;
//
//            if (connection.getState() != ConnectionState.disconnected) {
//                connection.start();
//            }
//
//            connection.connect(bluetoothDevice);
//        } else {
//            Log.i(TAG, "We shouldn't restore the connection");
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "ONRESUME");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (!bluetoothAdapter.isEnabled() && !requestedEnableBluetooth) {
            requestedEnableBluetooth = true;
            connectionManager.enableConnection();
        } else if (bluetoothAdapter.isEnabled()) {

            switch (connectionManager.getConnectionState()) {
                case connected:
                    break;
                case connecting:
                    break;
                case disconnected:
                    connectionManager.startConnection();
                    break;
                case stopped:
                    connectionManager.startConnection();
                    break;
                default:
                    connectionManager.startConnection();
                    break;
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "ONSAVEDINSTANCE");

        if (connectionManager.getConnectionState() == ConnectionState.connected) {
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
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "ONDESTROY");

        connectionManager.stopConnection();
        BluetoothConnection.destroyInstance();

        try {
            unregisterReceiver(BluetoothStateReceiver.getInstance(connectionManager.connection, bluetoothDevicesArrayAdapter, this, sharedPreferences));
        } catch (IllegalArgumentException ie) {
            Log.i(TAG, "BluetoothReceiver was not registered");
        }

        try {
            unregisterReceiver(BluetoothDiscoveryReceiver.getInstance(bluetoothDevicesArrayAdapter, this));
        } catch (IllegalArgumentException ie) {
            Log.i(TAG, "bluetoothDiscoveryRegister was not registered");
        }

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);
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
            case 4:
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new WifiFragment())
                        .commit();
                break;
        }

        onSectionAttached(position);
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                fragmentTitle = getString(R.string.title_section1);
                break;
            case 1:
                fragmentTitle = getString(R.string.title_section2);
                break;
            case 2:
                fragmentTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(fragmentTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!navigationDrawerFragment.isDrawerOpen()) {
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

    private final Handler messageHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String message = "";

            switch (msg.what) {

                case WSConstants.MESSAGE_TOAST:

                    message = (String) msg.obj;
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    break;

                case WSConstants.MESSAGE_READ:

                    message = (String) msg.obj;

                    // [start, pdu, end]
                    String pdu = message.split("_")[1];
                    Log.i(TAG, "PDU of the message " + pdu + " message " + message);
                    String[] weather = pdu.split(" ");
                    double windSpeed = 0;
                    double temperature = 0;

                    try {
                        windSpeed = Double.parseDouble(weather[0]);
                        temperature = Double.parseDouble(weather[1]);
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot parse weather information");
                    }

                    WeatherData weatherData = new WeatherData(windSpeed, temperature);
                    Log.i(TAG, weatherData.toString());

                    weatherListener.weatherDataReceived(weatherData);
                    break;

                case WSConstants.MESSAGE_STATE:

                    ConnectionState state = (ConnectionState) msg.obj;

                    switch (state) {
                        case connecting:
                            Toast.makeText(getApplicationContext(), "Connecting to weather station", Toast.LENGTH_LONG).show();
                            break;
                        case disconnected:
                            Toast.makeText(getApplicationContext(), "Disconnected from weather station", Toast.LENGTH_LONG).show();
                            break;
                    }
                    break;

                case WSConstants.MESSAGE_CONNECTED:

                    Toast.makeText(getApplicationContext(), "Connected to weather station", Toast.LENGTH_LONG).show();
                    navigationDrawerFragment.selectItem(0);
                    break;
            }
        }
    };

    public ArrayAdapter<String> getPairedDevicesArrayAdapter() {
        return bluetoothDevicesArrayAdapter;
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }


    public static Set<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    @Override
    public void onDeviceSelectedToConnect(String address) {

        sharedPreferences.edit().putString(getString(R.string.PREFERENCE_DEVICE_ADDRESS), address).commit();

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        Log.i(TAG, bluetoothDevice.getName() + bluetoothDevice.getAddress());

        connectionManager.connectToDevice(bluetoothDevice);
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

    @Override
    public void onDeviceSelectedToConnect(WifiDevice device) {
        connectionManager.connectToDevice(device);
    }
}
