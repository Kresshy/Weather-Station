package com.kresshy.weatherstation.bluetooth;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.connection.Connection;

import java.util.Set;

import timber.log.Timber;

public class BluetoothStateReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothStateReceiver";

    private static BluetoothStateReceiver instance = null;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayAdapter bluetoothDevices;
    private Set<BluetoothDevice> pairedDevices;
    private ActionBarActivity activity;
    private Connection connection;
    private SharedPreferences sharedPreferences;

    protected BluetoothStateReceiver(
            Connection connection,
            ArrayAdapter bluetoothDevices,
            ActionBarActivity activity,
            SharedPreferences sharedPreferences
    ) {
        this.bluetoothDevices = bluetoothDevices;
        this.activity = activity;
        this.connection = connection;
        this.sharedPreferences = sharedPreferences;
    }

    public static BluetoothStateReceiver getInstance(
            Connection connection,
            ArrayAdapter bluetoothDevices,
            ActionBarActivity activity,
            SharedPreferences sharedPreferences
    ) {
        if (instance == null) {
            return new BluetoothStateReceiver(connection, bluetoothDevices, activity, sharedPreferences);
        } else {
            return instance;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
            Timber.d("Received bluetooth state change: STATE_TURNING_ON");
        }

        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            Timber.d("Received bluetooth state change: STATE_ON");

            connection.start();
            reconnectPreviousWeatherStation();

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
        if (sharedPreferences.getBoolean(WSConstants.KEY_PREF_RECONNECT, false)) {
            Timber.d("We should restore the connection");

            final String address = sharedPreferences.getString(
                    activity.getString(R.string.PREFERENCE_DEVICE_ADDRESS),
                    WSConstants.BT_NULL_ADDRESS
            );

            if (!address.equals(WSConstants.BT_NULL_ADDRESS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.reconnect_message);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        Timber.d("The device address is valid, attempting to reconnect");
                        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
                        connection.connect(bluetoothDevice);
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        Timber.d("We couldn't restore the connection");
                        dialog.cancel();
                    }
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
