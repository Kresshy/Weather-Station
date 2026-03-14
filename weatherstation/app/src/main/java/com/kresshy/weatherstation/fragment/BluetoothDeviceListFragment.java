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
 * Fragment that displays a list of paired and discovered Bluetooth devices. This component allows
 * the user to discover nearby weather stations, initiate pairing, and select a device for
 * connection.
 */
@AndroidEntryPoint
public class BluetoothDeviceListFragment extends Fragment {

    private BluetoothDeviceRecyclerAdapter adapter;
    private final List<BluetoothDeviceRecyclerAdapter.DisplayItem> displayItems = new ArrayList<>();
    private WeatherViewModel weatherViewModel;

    private FragmentBluetoothdeviceBinding binding;

    /** Required empty public constructor for fragment instantiation by the Android framework. */
    public BluetoothDeviceListFragment() {}

    /**
     * Called when the fragment is first attached to its context. This is the earliest point to
     * initialize context-dependent resources.
     *
     * @param context The context the fragment is being attached to.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    /**
     * Initializes the fragment's internal state. This is where the shared {@link WeatherViewModel}
     * is retrieved to ensure data consistency across the application.
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
    }

    /**
     * Inflates the fragment's layout and initializes the data binding. This creates the visual
     * structure of the device selection screen.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views.
     * @param container If non-null, this is the parent view that the fragment's UI should be
     *     attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state.
     * @return The root view of the fragment's layout.
     */
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBluetoothdeviceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Sets up the RecyclerView and observes ViewModel data streams. This ensures the device list is
     * dynamically updated as new hardware is discovered or pairing states change.
     *
     * @param view The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state.
     */
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
                .isDiscovering()
                .observe(
                        getViewLifecycleOwner(),
                        isDiscovering ->
                                binding.discoveryStatusContainer.setVisibility(
                                        isDiscovering ? View.VISIBLE : View.GONE));

        weatherViewModel
                .getDiscoveryStatus()
                .observe(
                        getViewLifecycleOwner(),
                        status -> binding.textDiscoveryStatus.setText(status));

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
     * Updates the text displayed when the device list is empty. This provides feedback to the user
     * when no weather stations are currently available.
     *
     * @param emptyText The message to be displayed.
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

    /**
     * Cleans up the fragment's resources. This is essential to prevent memory leaks by nulling out
     * the view binding.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Refreshes the list of paired devices when the fragment becomes visible. This ensures the user
     * sees the most up-to-date information about known hardware.
     */
    @Override
    public void onStart() {
        super.onStart();
        weatherViewModel.refreshPairedDevices();
    }
}
