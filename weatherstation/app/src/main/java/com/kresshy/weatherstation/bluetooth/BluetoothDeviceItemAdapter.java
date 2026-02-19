package com.kresshy.weatherstation.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.databinding.BluetoothDeviceListItemBinding;

import java.util.List;

/**
 * Custom adapter for displaying a list of Bluetooth devices or Simulators. Handles the display of
 * device name, MAC address, and pairing status.
 */
public class BluetoothDeviceItemAdapter extends ArrayAdapter<Parcelable> {

    List<Parcelable> devices;
    Context context;

    /**
     * @param context Application context.
     * @param devices List of {@link BluetoothDevice} or {@link SimulatorDevice}.
     */
    public BluetoothDeviceItemAdapter(Context context, List<Parcelable> devices) {
        super(context, 0, devices);
        this.context = context;
        this.devices = devices;
    }

    @NonNull @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        BluetoothDeviceListItemBinding binding;
        if (convertView == null) {
            binding =
                    BluetoothDeviceListItemBinding.inflate(
                            LayoutInflater.from(getContext()), parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (BluetoothDeviceListItemBinding) convertView.getTag();
        }

        Parcelable device = devices.get(position);

        if (device != null) {
            String name = "";
            String address = "";
            boolean isPaired = false;

            if (device instanceof BluetoothDevice) {
                BluetoothDevice btDevice = (BluetoothDevice) device;
                if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    name = btDevice.getName();
                }
                address = btDevice.getAddress();
                isPaired = btDevice.getBondState() == BluetoothDevice.BOND_BONDED;
            } else if (device instanceof SimulatorDevice) {
                SimulatorDevice simDevice = (SimulatorDevice) device;
                name = simDevice.getName();
                address = simDevice.getAddress();
                isPaired = true; // Simulator is always displayed as paired
            }

            if (name == null || name.isEmpty()) {
                name = context.getString(R.string.device_unknown);
            }

            binding.bluetoothDeviceName.setText(name);
            binding.bluetoothDeviceAddress.setText(address);

            // Change icon based on device type/name
            if (name.startsWith("WS") || name.contains("Simulator")) {
                binding.bluetoothDeviceIcon.setBackground(
                        ActivityCompat.getDrawable(context, R.drawable.weather_station));
            } else {
                binding.bluetoothDeviceIcon.setBackground(
                        ActivityCompat.getDrawable(context, R.drawable.mobile_device));
            }

            if (isPaired) {
                binding.bluetoothDeviceStatus.setText(
                        context.getString(R.string.device_status_paired));
            } else {
                binding.bluetoothDeviceStatus.setText(
                        context.getString(R.string.device_status_unpaired));
            }
        }
        return convertView;
    }
}
