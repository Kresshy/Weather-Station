package com.kresshy.weatherstation.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.kresshy.weatherstation.R;

import java.util.List;

import timber.log.Timber;

public class BluetoothDeviceItemAdapter extends ArrayAdapter<BluetoothDevice> {

    private static final String TAG = "BluetoothDeviceItemAdapter";
    List<BluetoothDevice> bluetoothDevices;
    Context context;

    public BluetoothDeviceItemAdapter(Context context, List<BluetoothDevice> bluetoothDevices) {
        super(context, 0, bluetoothDevices);

        this.context = context;
        this.bluetoothDevices = bluetoothDevices;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.bluetooth_device_list_item, null);
        }

        BluetoothDevice bluetoothDevice = bluetoothDevices.get(position);

        if (bluetoothDevice != null) {
            ImageView icon = (ImageView) v.findViewById(R.id.bluetooth_device_icon);
            TextView name = (TextView) v.findViewById(R.id.bluetooth_device_name);
            TextView address = (TextView) v.findViewById(R.id.bluetooth_device_address);
            TextView status = (TextView) v.findViewById(R.id.bluetooth_device_status);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)  {
                Timber.d( "getView, Missing Permissions: BLUETOOTH_CONNECT");
            }
            if (bluetoothDevice.getName() != null) {
                if (bluetoothDevice.getName().startsWith("WS")) {
                    icon.setBackgroundDrawable(this.context.getResources().getDrawable(R.drawable.weather_station));
                }

                if (name != null) {
                    name.setText(bluetoothDevice.getName());
                }
            } else {
                name.setText("Unknown device");
            }

            if (address != null) {
                address.setText(bluetoothDevice.getAddress());
            }

            if (status != null) {
                if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                    status.setText("paired");
            }
        }
        return v;
    }
}
