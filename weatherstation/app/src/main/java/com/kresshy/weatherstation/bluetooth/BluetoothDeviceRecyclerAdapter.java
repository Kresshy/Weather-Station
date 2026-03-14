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

    /** Interface for handling click events on a specific Bluetooth device item. */
    public interface OnDeviceClickListener {
        /**
         * Called when a device item is clicked.
         *
         * @param device The clicked device (BluetoothDevice or SimulatorDevice).
         */
        void onDeviceClick(Parcelable device);
    }

    /** Interface for handling pair button clicks. */
    public interface OnPairClickListener {
        /**
         * Called when the "Pair" button for a device is clicked.
         *
         * @param device The device to pair with.
         */
        void onPairClick(BluetoothDevice device);
    }

    private final Context context;
    private final List<DisplayItem> items;
    private OnDeviceClickListener deviceClickListener;
    private OnPairClickListener pairClickListener;

    /**
     * Wrapper class for items displayed in the RecyclerView, allowing for both headers and device
     * items.
     */
    public static class DisplayItem {
        public final int type;
        public final String headerTitle;
        public final Parcelable device;

        /**
         * Constructs a header item.
         *
         * @param headerTitle The title of the header.
         */
        public DisplayItem(String headerTitle) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
            this.device = null;
        }

        /**
         * Constructs a device item.
         *
         * @param device The device to display.
         */
        public DisplayItem(Parcelable device) {
            this.type = TYPE_ITEM;
            this.device = device;
            this.headerTitle = null;
        }
    }

    /**
     * Constructs a new BluetoothDeviceRecyclerAdapter.
     *
     * @param context The application context.
     * @param items The list of items to display.
     */
    public BluetoothDeviceRecyclerAdapter(Context context, List<DisplayItem> items) {
        this.context = context;
        this.items = items;
    }

    /**
     * Sets the listener for device item click events.
     *
     * @param listener The listener instance.
     */
    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.deviceClickListener = listener;
    }

    /**
     * Sets the listener for pair button click events.
     *
     * @param listener The listener instance.
     */
    public void setOnPairClickListener(OnPairClickListener listener) {
        this.pairClickListener = listener;
    }

    /**
     * Determines the view type (header or item) for a given position.
     *
     * @param position The position in the list.
     * @return The integer constant representing the view type.
     */
    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    /**
     * Creates a new ViewHolder for the specified view type.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The type of view to create.
     * @return A new ViewHolder instance.
     */
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

    /**
     * Binds data to the specified ViewHolder.
     *
     * @param holder The ViewHolder to update.
     * @param position The position of the item in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.headerTitle);
        } else if (holder instanceof DeviceViewHolder) {
            ((DeviceViewHolder) holder).bind(item.device);
        }
    }

    /**
     * Returns the total number of items in the list.
     *
     * @return The size of the items list.
     */
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
