package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kresshy.weatherstation.R;

import java.util.List;

public class BluetoothDeviceItemAdapter extends ArrayAdapter<BluetoothDevice> {
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
