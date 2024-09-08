package com.kresshy.weatherstation.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.bluetooth.BluetoothDeviceItemAdapter;
import com.kresshy.weatherstation.bluetooth.BluetoothDiscoveryReceiver;
import com.kresshy.weatherstation.bluetooth.BluetoothStateReceiver;
import com.kresshy.weatherstation.connection.ConnectionManager;
import com.kresshy.weatherstation.fragment.BluetoothDeviceListFragment;
import com.kresshy.weatherstation.fragment.CalibrationFragment;
import com.kresshy.weatherstation.fragment.DashboardFragment;
import com.kresshy.weatherstation.fragment.GraphViewFragment;
import com.kresshy.weatherstation.fragment.NavigationDrawerFragment;
import com.kresshy.weatherstation.fragment.SettingsFragment;
import com.kresshy.weatherstation.fragment.WifiFragment;
import com.kresshy.weatherstation.utils.ConnectionState;
import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherListener;
import com.kresshy.weatherstation.wifi.WifiDevice;

import java.util.ArrayList;
import java.util.Set;

import timber.log.Timber;


public class WSActivity extends AppCompatActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        BluetoothDeviceListFragment.OnFragmentInteractionListener,
        DashboardFragment.OnFragmentInteractionListener,
        GraphViewFragment.OnFragmentInteractionListener,
        WifiFragment.OnFragmentInteractionListener,
        CalibrationFragment.OnFragmentInteractionListener {

    private static final String TAG = "WSActivity";

    private static BluetoothDeviceItemAdapter bluetoothDevicesArrayAdapter;
    private static ArrayList<BluetoothDevice> bluetoothDevices;
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
    private boolean permissionsGranted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d(TAG, "ONCREATE");

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

        // ask all runtime permissions
        String[] permissions = new String[]{
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        };

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int requestCode = 1;
            Timber.d("Requesting Permissions!...");
            requestPermissions(permissions, requestCode);
        }

        // setting up bluetooth adapter, service and broadcast receivers
        bluetoothDevices = new ArrayList<>();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevicesArrayAdapter = new BluetoothDeviceItemAdapter(this, bluetoothDevices);
        connectionManager = ConnectionManager.getInstance(this, bluetoothDevicesArrayAdapter, messageHandler);

        // setting up sharedpreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);

        registerReceiver(BluetoothStateReceiver.getInstance(connectionManager.connection, bluetoothDevicesArrayAdapter, this, sharedPreferences), new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(BluetoothDiscoveryReceiver.getInstance(bluetoothDevicesArrayAdapter, this), new IntentFilter(BluetoothDevice.ACTION_FOUND));

        if (permissionsGranted) {
            if (!requestedEnableBluetooth) {
                connectionManager.enableConnection();
                requestedEnableBluetooth = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("ONSTART");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (permissionsGranted) {
            if (!requestedEnableBluetooth) {
                connectionManager.enableConnection();
                requestedEnableBluetooth = true;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("ONRESUME");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (!bluetoothAdapter.isEnabled() && !requestedEnableBluetooth) {
            if (permissionsGranted) {
                requestedEnableBluetooth = true;
                connectionManager.enableConnection();
            }
        } else if (bluetoothAdapter.isEnabled()) {

            switch (connectionManager.getConnectionState()) {
                case connected:
                    break;
                case connecting:
                    break;
                case disconnected:
                    if (permissionsGranted) {
                        connectionManager.startConnection();
                    }
                    break;
                case stopped:
                    if (permissionsGranted) {
                        connectionManager.startConnection();
                    }
                    break;
                default:
                    if (permissionsGranted) {
                        connectionManager.startConnection();
                    }
                    break;
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Timber.d("ONSAVEDINSTANCE");

        if (connectionManager.getConnectionState() == ConnectionState.connected) {
            Timber.d("Connected to a device");
            outState.putBoolean(getString(R.string.PREFERENCE_CONNECTED), true);
        } else {
            Timber.d("Not connected to a device");
            outState.putBoolean(getString(R.string.PREFERENCE_CONNECTED), false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Timber.d("ONPAUSE");
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("ONDESTROY");

        connectionManager.stopConnection();
        BluetoothConnection.destroyInstance();

        try {
            unregisterReceiver(BluetoothStateReceiver.getInstance(connectionManager.connection, bluetoothDevicesArrayAdapter, this, sharedPreferences));
        } catch (IllegalArgumentException ie) {
            Timber.d("BluetoothReceiver was not registered");
        }

        try {
            unregisterReceiver(BluetoothDiscoveryReceiver.getInstance(bluetoothDevicesArrayAdapter, this));
        } catch (IllegalArgumentException ie) {
            Timber.d("bluetoothDiscoveryRegister was not registered");
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
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new GraphViewFragment())
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
                    Timber.d("Disabling bluetooth adapter");
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)  {
                        Timber.d("onNavigationDrawerItemSelected, Missing Permissions: BLUETOOTH_CONNECT");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Timber.d("Requesting Permissions!...");
                            requestPermissions(new String[] {Manifest.permission.BLUETOOTH_CONNECT}, 3);
                        }
                    }
                    bluetoothAdapter.disable();
                }

                finish();
                break;
            case 4:
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new WifiFragment())
                        .commit();
                break;
            default:
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new GraphViewFragment())
                        .commit();
        }

        onSectionAttached(position);
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                fragmentTitle = getString(R.string.dashboard_view);
                break;
            case 1:
                fragmentTitle = getString(R.string.bluetooth_weather_station_connect_view);
                break;
            case 2:
                fragmentTitle = getString(R.string.settings_view);
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
                Timber.d("Disabling bluetooth adapter");
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)  {
                    Timber.d("onOptionsItemSelected, Missing Permissions: BLUETOOTH_CONNECT");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Timber.d("Requesting Permissions!...");
                        requestPermissions(new String[] {Manifest.permission.BLUETOOTH_CONNECT}, 3);
                    }
                }
                bluetoothAdapter.disable();
            }

            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("HandlerLeak")
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

                    // [start_pdu_end]
                    String pdu = message.split("_")[1];
                    Timber.d("PDU of the message");
                    Timber.d(pdu);

                    double windSpeed = 0;
                    double temperature = 0;

                    WeatherData weatherData;
                    Measurement measurement;

                    try {
                        Gson gson = new Gson();
                        measurement = gson.fromJson(pdu, Measurement.class);
                        Timber.d( measurement.toString());
                        weatherData = measurement.getWeatherDataForNode(0);
                        Timber.d(weatherData.toString());
                        Timber.d("Transferring new measurement / weatherData");
                        weatherListener.weatherDataReceived(weatherData);
                        weatherListener.measurementReceived(measurement);
                        break;
                    } catch (JsonSyntaxException jse) {
                        try {
                            Timber.d("JsonSyntaxException, parsing as version 1 pdu");
                            String[] weather = pdu.split(" ");
                            windSpeed = Double.parseDouble(weather[0]);
                            temperature = Double.parseDouble(weather[1]);
                            weatherData = new WeatherData(windSpeed, temperature);
                            Timber.d(weatherData.toString());
                            measurement = new Measurement(1, 1);
                            measurement.addWeatherDataToMeasurement(weatherData);
                            Timber.d(measurement.toString());
                            Timber.d("Transferring new measurement / weatherData");
                            weatherListener.weatherDataReceived(weatherData);
                            weatherListener.measurementReceived(measurement);
                            break;
                        } catch (NumberFormatException nfe) {
                            Timber.d("Cannot parse weather data: " + pdu);
                        }
                    } catch (NumberFormatException nfe) {
                        Timber.d("Cannot parse weather data: " + pdu);
                    }
                    break;

                case WSConstants.MESSAGE_STATE:
                    ConnectionState state = (ConnectionState) msg.obj;

                    switch (state) {
                        case connecting:
                            Toast.makeText(getApplicationContext(), "Connecting to weather station", Toast.LENGTH_SHORT).show();
                            break;
                        case disconnected:
                            Toast.makeText(getApplicationContext(), "Disconnected from weather station", Toast.LENGTH_LONG).show();
                            break;
                    }

                    break;

                case WSConstants.MESSAGE_CONNECTED:
                    Toast.makeText(getApplicationContext(), "Connected to weather station", Toast.LENGTH_SHORT).show();
//                    navigationDrawerFragment.selectItem(0);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.container, new CalibrationFragment())
                            .commit();
                    break;

                case WSConstants.MESSAGE_LOG:
                    message = (String) msg.obj;
                    Timber.d(message);
                    break;
            }
        }
    };

    public ArrayAdapter getPairedDevicesArrayAdapter() {
        return bluetoothDevicesArrayAdapter;
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public static ArrayList<BluetoothDevice> getBluetoothDevices() {
        return bluetoothDevices;
    }

    public static Set<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    @Override
    public void onDeviceSelectedToConnect(String address) {
        sharedPreferences.edit().putString(getString(R.string.PREFERENCE_DEVICE_ADDRESS), address).apply();

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)  {
            Timber.d("onDeviceSelectedToConnect, Missing Permissions: BLUETOOTH_CONNECT");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Timber.d("Requesting Permissions!...");
                requestPermissions(new String[] {Manifest.permission.BLUETOOTH_CONNECT}, 3);
            }
        }
        Timber.d(bluetoothDevice.getName() + bluetoothDevice.getAddress());

        connectionManager.connectToDevice(bluetoothDevice);
    }

    @Override
    public void startBluetoothDiscovery() {
        Timber.d("Starting bluetooth discovery");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Toast.makeText(getApplicationContext(), "Missing Permissions: BLUETOOTH_SCAN", Toast.LENGTH_SHORT).show();
                Timber.d("startBluetoothDiscovery, missing permissions: BLUETOOTH_SCAN");
                Timber.d("Requesting Permissions!...");
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 4);
            }
        }
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void stopBluetoothDiscovery() {
        Timber.d("Stopping bluetooth discovery");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Toast.makeText(getApplicationContext(), "Missing Permissions: BLUETOOTH_SCAN", Toast.LENGTH_SHORT).show();
                Timber.d("stopBluetoothDiscovery, missing permissions: BLUETOOTH_SCAN");
                Timber.d("Requesting Permissions!...");
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 4);
            }
        }
        bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void registerWeatherDataReceiver(WeatherListener weatherListener) {
        this.weatherListener = weatherListener;
    }

    @Override
    public void startDashboardAfterCalibration() {
        navigationDrawerFragment.selectItem(0);
    }

    @Override
    public void onDeviceSelectedToConnect(WifiDevice device) {
        connectionManager.connectToDevice(device);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Timber.d("onRequestPermissionsResult");
        switch (requestCode) {
            case 1:
                if (!requestedEnableBluetooth) {
                    Timber.d("Starting Connection");
                    connectionManager.enableConnection();
                    connectionManager.startConnection();
                    sharedPreferences.registerOnSharedPreferenceChangeListener(connectionManager.sharedPreferenceChangeListener);
                    requestedEnableBluetooth = true;
                }

                permissionsGranted = true;
                break;
            default:
                break;
        }
    }
}
