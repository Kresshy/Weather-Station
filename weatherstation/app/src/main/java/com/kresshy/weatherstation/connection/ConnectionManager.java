package com.kresshy.weatherstation.connection;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.utils.ConnectionState;
import com.kresshy.weatherstation.wifi.WifiConnection;

import java.util.Set;

import timber.log.Timber;

public class ConnectionManager {

    private static final String TAG = "ConnectionManager";

    private static ConnectionManager instance = null;
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    WifiManager wifiManager;

    // wifi or bluetooth connection interface
    public Connection connection;

    private AppCompatActivity activity;
    private ArrayAdapter adapter;
    private Handler handler;

    protected ConnectionManager(AppCompatActivity activity, ArrayAdapter adapter, Handler handler) {
        this.activity = activity;
        this.adapter = adapter;
        this.connection = ConnectionFactory.getConnection(handler, activity);
        this.handler = handler;

        this.wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static ConnectionManager getInstance(AppCompatActivity activity, ArrayAdapter adapter, Handler handler) {
        if (instance == null) {
            return new ConnectionManager(activity, adapter, handler);
        } else {
            return instance;
        }
    }

    public void enableConnection() {
        String connectionType = getConnectionType(activity);

        if (connectionType.equals("bluetooth")) {
            if (bluetoothAdapter == null) {
                Timber.d( "Bluetooth is not supported, shutting down application");
                Toast.makeText(activity, "Bluetooth is not supported", Toast.LENGTH_LONG).show();
                activity.finish();
            } else {
                enableBluetooth();

                if (bluetoothAdapter.isEnabled()) {
                    if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Timber.d( "enableConnection, Missing Permissions: BLUETOOTH_CONNECT");
                        Timber.d( "Requesting Permissions!...");
                        requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 6);
                    }
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                    // If there are paired devices, add each one to the ArrayAdapter
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            adapter.add(device);
                        }
                    }
                }
            }
        } else {
            enableWifi();
        }
    }

    public static String getConnectionType(Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

        return sharedPreferences.getString("pref_connection_type", "bluetooth");
    }

    private void enableWifi() {
        wifiManager.setWifiEnabled(true);
    }

    private void disableWifi() {
        wifiManager.setWifiEnabled(false);
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Timber.d( "Enabling bluetooth adapter");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)  {
                Timber.d( "enableBluetooth, Missing Permissions: BLUETOOTH_CONNECT");
                Timber.d( "Requesting Permissions!...");
                requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 6);
            }
            activity.startActivityForResult(enableIntent, WSConstants.REQUEST_ENABLE_BT);
        }
    }

    private void disableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            Timber.d( "Disabling bluetooth adapter");
            if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)  {
                Timber.d( "disableBluetooth, Missing Permissions: BLUETOOTH_CONNECT");
                Timber.d( "Requesting Permissions!...");
                requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 6);
            }
            bluetoothAdapter.disable();
        }
    }

    public void startConnection() {
        connection.start();
    }

    public void stopConnection() {
        connection.stop();
    }

    public void connectToDevice(Parcelable device) {
        connection.connect(device);
    }

    public ConnectionState getConnectionState() {
        return connection.getState();
    }

    public SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

                    if (key.equals("pref_connection_type")) {
                        String connectionType = sharedPreferences.getString(key, "bluetooth");

                        enableConnection();

                        if (connectionType.equals("bluetooth")) {
                            connection = BluetoothConnection.getInstance(handler, activity);
                        } else {
                            connection = WifiConnection.getInstance(handler, activity);
                        }

                        stopConnection();
                        startConnection();
                    }
                }
            };
}
