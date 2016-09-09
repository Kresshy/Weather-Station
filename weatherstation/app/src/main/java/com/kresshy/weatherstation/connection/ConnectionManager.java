package com.kresshy.weatherstation.connection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.interfaces.Connection;
import com.kresshy.weatherstation.utils.ConnectionState;

import java.util.Set;

public class ConnectionManager {

    private static final String TAG = "ConnectionManager";

    private static ConnectionManager instance = null;
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    WifiManager wifiManager;

    // wifi or bluetooth connection interface
    public Connection connection;

    private ActionBarActivity activity;
    private ArrayAdapter<String> bluetoothDevices;
    private boolean requestedEnableBluetooth = false;
    private Handler handler;

    protected ConnectionManager(ActionBarActivity activity, ArrayAdapter<String> bluetoothDevices, Handler handler) {
        this.activity = activity;
        this.bluetoothDevices = bluetoothDevices;
        this.connection = ConnectionFactory.getConnection(handler, activity);
        this.handler = handler;

        this.wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static ConnectionManager getInstance(ActionBarActivity activity, ArrayAdapter<String> bluetoothDevices, Handler handler) {
        if (instance == null) {
            return new ConnectionManager(activity, bluetoothDevices, handler);
        } else {
            return instance;
        }
    }

    public void enableConnection() {
        String connectionType = getConnectionType(activity);

        if (connectionType.equals("bluetooth")) {
            if (bluetoothAdapter == null) {
                Toast.makeText(activity, "Bluetooth is not supported", Toast.LENGTH_LONG).show();
                activity.finish();
            }

            enableBluetooth();

            if (bluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                // If there are paired devices, add each one to the ArrayAdapter
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        bluetoothDevices.add(device.getName() + "\n" + device.getAddress());
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
            Log.i(TAG, "Enabling bluetooth adapter");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, WSConstants.REQUEST_ENABLE_BT);
        }
    }

    private void disableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Disabling bluetooth adapter");
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