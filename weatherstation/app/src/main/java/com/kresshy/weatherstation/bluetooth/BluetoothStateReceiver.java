package com.kresshy.weatherstation.bluetooth;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.connection.Connection;

import timber.log.Timber;

import java.util.Set;

public class BluetoothStateReceiver extends BroadcastReceiver {
    private static BluetoothStateReceiver instance = null;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayAdapter bluetoothDevices;
    private Set<BluetoothDevice> pairedDevices;
    private AppCompatActivity activity;
    private Connection connection;
    private SharedPreferences sharedPreferences;

    protected BluetoothStateReceiver(
            Connection connection,
            ArrayAdapter bluetoothDevices,
            AppCompatActivity activity,
            SharedPreferences sharedPreferences) {
        this.bluetoothDevices = bluetoothDevices;
        this.activity = activity;
        this.connection = connection;
        this.sharedPreferences = sharedPreferences;
    }

    public static BluetoothStateReceiver getInstance(
            Connection connection,
            ArrayAdapter bluetoothDevices,
            AppCompatActivity activity,
            SharedPreferences sharedPreferences) {
        if (instance == null) {
            return new BluetoothStateReceiver(
                    connection, bluetoothDevices, activity, sharedPreferences);
        } else {
            return instance;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
            Timber.d("RECEIVED BLUETOOTH STATE CHANGE: STATE_TURNING_ON");
        }

        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            Timber.d("RECEIVED BLUETOOTH STATE CHANGE: STATE_ON");

            connection.start();
            reconnectPreviousWeatherStation();

            if (ActivityCompat.checkSelfPermission(
                            activity.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Timber.d("onReceive, Missing Permissions: BLUETOOTH_CONNECT");
                Timber.d("Requesting Permissions!...");
                requestPermissions(
                        activity, new String[] {Manifest.permission.BLUETOOTH_CONNECT}, 5);
            }
            pairedDevices = bluetoothAdapter.getBondedDevices();

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    bluetoothDevices.add(device);
                }
            }
        }
    }

    public void reconnectPreviousWeatherStation() {
        if (sharedPreferences.getBoolean("pref_reconnect", false)) {
            Timber.d("We should restore the connection");
            final String address =
                    sharedPreferences.getString(
                            activity.getString(R.string.PREFERENCE_DEVICE_ADDRESS),
                            "00:00:00:00:00:00");

            if (!address.equals("00:00:00:00:00:00")) {

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.reconnect_message);
                builder.setPositiveButton(
                        R.string.ok,
                        (dialog, id) -> {
                            Timber.d("The device address is valid, attempting to reconnect");
                            BluetoothDevice bluetoothDevice =
                                    bluetoothAdapter.getRemoteDevice(address);
                            connection.connect(bluetoothDevice);
                        });
                builder.setNegativeButton(
                        R.string.cancel,
                        (dialog, id) -> {
                            Timber.d("We couldn't restore the connection");
                            dialog.cancel();
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Timber.d("The device address was invalid");
            }
        } else {
            Timber.d("We shouldn't restore the connection");
        }
    }
}
