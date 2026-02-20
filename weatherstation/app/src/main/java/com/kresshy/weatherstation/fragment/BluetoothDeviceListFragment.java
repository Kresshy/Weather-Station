package com.kresshy.weatherstation.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.kresshy.weatherstation.bluetooth.BluetoothDeviceItemAdapter;
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
public class BluetoothDeviceListFragment extends Fragment
        implements AbsListView.OnItemClickListener {

    private BluetoothDeviceItemAdapter adapter;
    private List<Parcelable> deviceList = new ArrayList<>();
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

        // Set the adapter for the list view
        adapter = new BluetoothDeviceItemAdapter(requireContext(), deviceList);
        binding.listviewBluetoothDevices.setAdapter(adapter);

        binding.listviewBluetoothDevices.setOnItemClickListener(this);

        // Observe both paired and discovered devices to keep the list current
        weatherViewModel
                .getPairedDevices()
                .observe(getViewLifecycleOwner(), this::updateDeviceList);
        weatherViewModel
                .getDiscoveredDevices()
                .observe(getViewLifecycleOwner(), this::updateDeviceList);

        // Auto-start discovery when screen is opened
        weatherViewModel.startDiscovery();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Consolidates paired and discovered devices into a single unique list. */
    private void updateDeviceList(List<Parcelable> devices) {
        List<Parcelable> paired = weatherViewModel.getPairedDevices().getValue();
        List<Parcelable> discovered = weatherViewModel.getDiscoveredDevices().getValue();

        java.util.LinkedHashSet<Parcelable> uniqueDevices = new java.util.LinkedHashSet<>();
        if (paired != null) uniqueDevices.addAll(paired);
        if (discovered != null) uniqueDevices.addAll(discovered);

        deviceList.clear();
        deviceList.addAll(uniqueDevices);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();
        weatherViewModel.refreshPairedDevices();
    }

    /**
     * Handles device selection. Stops discovery and attempts connection to the chosen MAC address.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Cancel discovery because it's costly and we're about to connect
        weatherViewModel.stopDiscovery();

        Parcelable device = deviceList.get(position);
        String address = "";
        if (device instanceof BluetoothDevice) {
            address = ((BluetoothDevice) device).getAddress();
        } else if (device instanceof SimulatorDevice) {
            address = ((SimulatorDevice) device).getAddress();
        }
        weatherViewModel.connectToDeviceAddress(address);
    }

    /**
     * Updates the text displayed when the list is empty.
     *
     * @param emptyText The message to show.
     */
    public void setEmptyText(CharSequence emptyText) {
        if (binding != null) {
            View emptyView = binding.listviewBluetoothDevices.getEmptyView();
            if (emptyView instanceof TextView) {
                ((TextView) emptyView).setText(emptyText);
            }
        }
    }
}
