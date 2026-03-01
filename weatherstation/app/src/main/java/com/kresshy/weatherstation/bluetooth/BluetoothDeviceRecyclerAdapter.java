package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.databinding.BluetoothDeviceListHeaderBinding;
import com.kresshy.weatherstation.databinding.BluetoothDeviceListItemBinding;
import com.kresshy.weatherstation.util.PermissionHelper;

import java.util.List;

/**
 * RecyclerView adapter for displaying a sectioned list of Bluetooth devices. Supports headers for
 * "Paired" and "Available" sections.
 */
public class BluetoothDeviceRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnDeviceClickListener {
        void onDeviceClick(Parcelable device);
    }

    public interface OnPairClickListener {
        void onPairClick(BluetoothDevice device);
    }

    private final Context context;
    private final List<DisplayItem> items;
    private OnDeviceClickListener deviceClickListener;
    private OnPairClickListener pairClickListener;

    public static class DisplayItem {
        public final int type;
        public final String headerTitle;
        public final Parcelable device;

        public DisplayItem(String headerTitle) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
            this.device = null;
        }

        public DisplayItem(Parcelable device) {
            this.type = TYPE_ITEM;
            this.device = device;
            this.headerTitle = null;
        }
    }

    public BluetoothDeviceRecyclerAdapter(Context context, List<DisplayItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.deviceClickListener = listener;
    }

    public void setOnPairClickListener(OnPairClickListener listener) {
        this.pairClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            BluetoothDeviceListHeaderBinding binding =
                    BluetoothDeviceListHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(binding);
        } else {
            BluetoothDeviceListItemBinding binding =
                    BluetoothDeviceListItemBinding.inflate(inflater, parent, false);
            return new DeviceViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.headerTitle);
        } else if (holder instanceof DeviceViewHolder) {
            ((DeviceViewHolder) holder).bind(item.device);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final BluetoothDeviceListHeaderBinding binding;

        HeaderViewHolder(BluetoothDeviceListHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String title) {
            binding.headerTitle.setText(title);
        }
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final BluetoothDeviceListItemBinding binding;

        DeviceViewHolder(BluetoothDeviceListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Parcelable device) {
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

                    int type = btDevice.getType();
                    boolean isBle =
                            type == BluetoothDevice.DEVICE_TYPE_LE
                                    || type == BluetoothDevice.DEVICE_TYPE_DUAL;
                    binding.badgeBle.setVisibility(isBle ? View.VISIBLE : View.GONE);
                } else if (device instanceof SimulatorDevice) {
                    SimulatorDevice simDevice = (SimulatorDevice) device;
                    name = simDevice.getName();
                    address = simDevice.getAddress();
                    bondState = BluetoothDevice.BOND_BONDED;
                    binding.badgeBle.setVisibility(View.GONE);
                }

                if (name == null || name.isEmpty()) {
                    name = context.getString(R.string.device_unknown);
                }

                binding.bluetoothDeviceName.setText(name);
                binding.bluetoothDeviceAddress.setText(address);

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
                                if (pairClickListener != null
                                        && device instanceof BluetoothDevice) {
                                    pairClickListener.onPairClick((BluetoothDevice) device);
                                }
                            });
                }

                binding.getRoot()
                        .setOnClickListener(
                                v -> {
                                    if (deviceClickListener != null) {
                                        deviceClickListener.onDeviceClick(device);
                                    }
                                });
            }
        }
    }
}
