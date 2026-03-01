package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.databinding.BluetoothDeviceListItemBinding;
import com.kresshy.weatherstation.util.PermissionHelper;

import java.util.List;

/**
 * Custom adapter for displaying a list of Bluetooth devices or Simulators. Handles the display of
 * device name, MAC address, and pairing status.
 */
public class BluetoothDeviceItemAdapter extends ArrayAdapter<Parcelable> {

    List<Parcelable> devices;
    Context context;

    public interface OnPairClickListener {
        void onPairClick(BluetoothDevice device);
    }

    private OnPairClickListener pairClickListener;

    /**
     * @param context Application context.
     * @param devices List of {@link BluetoothDevice} or {@link SimulatorDevice}.
     */
    public BluetoothDeviceItemAdapter(Context context, List<Parcelable> devices) {
        super(context, 0, devices);
        this.context = context;
        this.devices = devices;
    }

    public void setOnPairClickListener(OnPairClickListener listener) {
        this.pairClickListener = listener;
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
            int bondState = BluetoothDevice.BOND_NONE;

            if (device instanceof BluetoothDevice) {
                BluetoothDevice btDevice = (BluetoothDevice) device;
                if (PermissionHelper.hasConnectPermission(context)) {
                    name = btDevice.getName();
                }
                address = btDevice.getAddress();
                bondState = btDevice.getBondState();
            } else if (device instanceof SimulatorDevice) {
                SimulatorDevice simDevice = (SimulatorDevice) device;
                name = simDevice.getName();
                address = simDevice.getAddress();
                bondState = BluetoothDevice.BOND_BONDED;
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

            if (bondState == BluetoothDevice.BOND_BONDED) {
                binding.bluetoothDeviceStatus.setText(
                        context.getString(R.string.device_status_paired));
                binding.btnPairDevice.setVisibility(View.GONE);
            } else if (bondState == BluetoothDevice.BOND_BONDING) {
                binding.bluetoothDeviceStatus.setText(
                        context.getString(R.string.device_status_pairing));
                binding.btnPairDevice.setVisibility(View.GONE);
            } else {
                binding.bluetoothDeviceStatus.setText(
                        context.getString(R.string.device_status_unpaired));
                binding.btnPairDevice.setVisibility(View.VISIBLE);
                binding.btnPairDevice.setOnClickListener(
                        v -> {
                            if (pairClickListener != null && device instanceof BluetoothDevice) {
                                pairClickListener.onPairClick((BluetoothDevice) device);
                            }
                        });
            }
        }
        return convertView;
    }
}
