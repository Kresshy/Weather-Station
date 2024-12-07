package com.kresshy.weatherstation.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import timber.log.Timber;

public class BluetoothDiscoveryReceiver extends BroadcastReceiver {

    private static BluetoothDiscoveryReceiver instance = null;
    private ArrayAdapter bluetoothDevices;
    private AppCompatActivity activity;

    protected BluetoothDiscoveryReceiver(
            ArrayAdapter bluetoothDevices, AppCompatActivity activity) {
        this.bluetoothDevices = bluetoothDevices;
        this.activity = activity;
    }

    public static BluetoothDiscoveryReceiver getInstance(
            ArrayAdapter bluetoothDevices, AppCompatActivity activity) {
        if (instance == null) {
            return new BluetoothDiscoveryReceiver(bluetoothDevices, activity);
        } else {
            return instance;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // If it's already paired, skip it, because it's been listed
            // already
            if (ActivityCompat.checkSelfPermission(
                            activity.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Timber.d("onReceive, Missing Permissions: BLUETOOTH_CONNECT");
            }
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                bluetoothDevices.add(device);
                Timber.d("Bluetooth Device added: " + device.getName());
            }
            // When discovery is finished, change the Activity fragmentTitle
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            activity.setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
            activity.setTitle("Select Device");
            if (bluetoothDevices.getCount() == 0) {
                String noDevices = "None found";
                bluetoothDevices.add(noDevices);
                Toast.makeText(context, "No devices found", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
