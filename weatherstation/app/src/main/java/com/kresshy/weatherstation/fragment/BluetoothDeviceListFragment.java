package com.kresshy.weatherstation.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.bluetooth.BluetoothDeviceRecyclerAdapter;
import com.kresshy.weatherstation.bluetooth.SimulatorDevice;
import com.kresshy.weatherstation.databinding.FragmentBluetoothdeviceBinding;
import com.kresshy.weatherstation.weather.WeatherViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays a list of paired and discovered Bluetooth devices. Allows the user to
 * select a device to initiate a weather station connection.
 */
@AndroidEntryPoint
public class BluetoothDeviceListFragment extends Fragment {

    private BluetoothDeviceRecyclerAdapter adapter;
    private final List<BluetoothDeviceRecyclerAdapter.DisplayItem> displayItems = new ArrayList<>();
    private WeatherViewModel weatherViewModel;

    private FragmentBluetoothdeviceBinding binding;

    public BluetoothDeviceListFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBluetoothdeviceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the adapter for the recycler view
        adapter = new BluetoothDeviceRecyclerAdapter(requireContext(), displayItems);
        adapter.setOnDeviceClickListener(this::onDeviceClick);
        adapter.setOnPairClickListener(device -> weatherViewModel.pairDevice(device));

        binding.recyclerviewBluetoothDevices.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.recyclerviewBluetoothDevices.setAdapter(adapter);

        // Observe both paired and discovered devices to keep the list current
        weatherViewModel
                .getPairedDevices()
                .observe(getViewLifecycleOwner(), devices -> updateDeviceList());
        weatherViewModel
                .getDiscoveredDevices()
                .observe(getViewLifecycleOwner(), devices -> updateDeviceList());

        weatherViewModel
                .getPairingRequest()
                .observe(getViewLifecycleOwner(), this::showPinEntryDialog);

        // Auto-start discovery when screen is opened
        weatherViewModel.startDiscovery();
    }

    private void onDeviceClick(Parcelable device) {
        // Cancel discovery because it's costly and we're about to connect
        weatherViewModel.stopDiscovery();

        String address = "";
        if (device instanceof BluetoothDevice) {
            address = ((BluetoothDevice) device).getAddress();
        } else if (device instanceof SimulatorDevice) {
            address = ((SimulatorDevice) device).getAddress();
        }
        weatherViewModel.connectToDeviceAddress(address);
    }

    /** Consolidates paired and discovered devices into a sectioned list. */
    private void updateDeviceList() {
        List<Parcelable> paired = weatherViewModel.getPairedDevices().getValue();
        List<Parcelable> discovered = weatherViewModel.getDiscoveredDevices().getValue();

        displayItems.clear();

        // 1. Paired Section
        if (paired != null && !paired.isEmpty()) {
            displayItems.add(
                    new BluetoothDeviceRecyclerAdapter.DisplayItem(
                            getString(R.string.header_paired_devices)));
            for (Parcelable d : paired) {
                displayItems.add(new BluetoothDeviceRecyclerAdapter.DisplayItem(d));
            }
        }

        // 2. Available Section (excluding already paired ones)
        if (discovered != null) {
            List<Parcelable> actuallyNew = new ArrayList<>();
            for (Parcelable d : discovered) {
                boolean alreadyPaired = false;
                if (paired != null && d instanceof BluetoothDevice) {
                    for (Parcelable p : paired) {
                        if (p instanceof BluetoothDevice
                                && ((BluetoothDevice) p)
                                        .getAddress()
                                        .equals(((BluetoothDevice) d).getAddress())) {
                            alreadyPaired = true;
                            break;
                        }
                    }
                }
                if (!alreadyPaired) {
                    actuallyNew.add(d);
                }
            }

            if (!actuallyNew.isEmpty()) {
                displayItems.add(
                        new BluetoothDeviceRecyclerAdapter.DisplayItem(
                                getString(R.string.header_available_devices)));
                for (Parcelable d : actuallyNew) {
                    displayItems.add(new BluetoothDeviceRecyclerAdapter.DisplayItem(d));
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    /**
     * Updates the text displayed when the list is empty.
     *
     * @param emptyText The message to show.
     */
    public void setEmptyText(CharSequence emptyText) {
        // Note: Empty view support for RecyclerView needs manual implementation
        // or a wrapper adapter. For now, we skip as the dashboard handles most states.
    }

    private void showPinEntryDialog(BluetoothDevice device) {
        if (device == null) return;

        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.pairing_pin_hint);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pairing_pin_title)
                .setMessage(getString(R.string.pairing_pin_message, device.getName()))
                .setView(input)
                .setPositiveButton(
                        R.string.ok,
                        (dialog, which) -> {
                            String pin = input.getText().toString();
                            weatherViewModel.setPin(device, pin);
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        weatherViewModel.refreshPairedDevices();
    }
}
