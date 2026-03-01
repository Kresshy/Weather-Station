package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.util.PermissionHelper;

import dagger.hilt.android.qualifiers.ApplicationContext;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link WeatherBluetoothManager} that uses standard Android BroadcastReceivers
 * to monitor Bluetooth state and discover new devices.
 */
@Singleton
public class WeatherBluetoothManagerImpl implements WeatherBluetoothManager {

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final MutableLiveData<List<BluetoothDevice>> discoveredDevices =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> discoveryStatus = new MutableLiveData<>("");
    private final MutableLiveData<Integer> bluetoothState =
            new MutableLiveData<>(BluetoothAdapter.STATE_OFF);
    private final MutableLiveData<BluetoothDevice> pairingRequest = new MutableLiveData<>();
    private final java.util.Map<String, Integer> deviceRssiMap = new java.util.HashMap<>();

    private boolean isRegistered = false;

    /** Receiver for monitoring overall Bluetooth adapter state (ON/OFF). */
    private final BroadcastReceiver stateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (bluetoothAdapter != null) {
                        int state = bluetoothAdapter.getState();
                        bluetoothState.postValue(state);
                        if (state == BluetoothAdapter.STATE_ON) {
                            Timber.d("Bluetooth turned ON");
                        }
                    }
                }
            };

    /** Receiver for handling discovered devices, bond state changes and pairing requests. */
    private final BroadcastReceiver discoveryReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device;
                        int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);

                        if (android.os.Build.VERSION.SDK_INT
                                >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            device =
                                    intent.getParcelableExtra(
                                            BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        } else {
                            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        }

                        if (device != null) {
                            deviceRssiMap.put(device.getAddress(), rssi);
                            updateDeviceList(device);
                        }
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        discoveryStatus.postValue("Discovery Finished");
                    } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                        BluetoothDevice device =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            Timber.d(
                                    "Bond state changed for %s: %d",
                                    device.getAddress(), device.getBondState());
                            updateDeviceList(device);
                        }
                    } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                        BluetoothDevice device =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            Timber.d("Pairing request received for %s", device.getAddress());
                            pairingRequest.postValue(device);
                        }
                    }
                }
            };

    private void updateDeviceList(BluetoothDevice device) {
        List<BluetoothDevice> current = discoveredDevices.getValue();
        if (current == null) current = new ArrayList<>();
        int index = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getAddress().equals(device.getAddress())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            current.set(index, device);
        } else {
            current.add(device);
        }
        discoveredDevices.postValue(current);
    }

    @Inject
    public WeatherBluetoothManagerImpl(
            @ApplicationContext Context context, @Nullable BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        if (bluetoothAdapter != null) {
            bluetoothState.postValue(bluetoothAdapter.getState());
        }
    }

    @Override
    public void startDiscovery() {
        if (bluetoothAdapter != null) {
            if (PermissionHelper.hasScanPermission(context)) {
                discoveredDevices.postValue(new ArrayList<>());
                discoveryStatus.postValue("Discovering...");
                bluetoothAdapter.startDiscovery();
            }
        }
    }

    @Override
    public void stopDiscovery() {
        if (bluetoothAdapter != null) {
            if (PermissionHelper.hasScanPermission(context)) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
    }

    @Override
    public void enableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (PermissionHelper.hasConnectPermission(context)) {
                bluetoothAdapter.enable();
            }
        }
    }

    @Override
    public void disableBluetooth() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (PermissionHelper.hasConnectPermission(context)) {
                bluetoothAdapter.disable();
            }
        }
    }

    @Override
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @Override
    public void registerReceivers() {
        if (!isRegistered) {
            context.registerReceiver(
                    stateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            IntentFilter discoveryFilter = new IntentFilter();
            discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
            discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            discoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            discoveryFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            context.registerReceiver(discoveryReceiver, discoveryFilter);
            isRegistered = true;
        }
    }

    @Override
    public void unregisterReceivers() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(stateReceiver);
                context.unregisterReceiver(discoveryReceiver);
                isRegistered = false;
            } catch (IllegalArgumentException e) {
                Timber.e(e);
            }
        }
    }

    @Override
    public LiveData<List<BluetoothDevice>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    @Override
    public LiveData<String> getDiscoveryStatus() {
        return discoveryStatus;
    }

    @Override
    public LiveData<Integer> getBluetoothState() {
        return bluetoothState;
    }

    @Override
    public int getDeviceRssi(String address) {
        Integer rssi = deviceRssiMap.get(address);
        return rssi != null ? rssi : 0;
    }

    @Override
    public void pairDevice(BluetoothDevice device) {
        if (PermissionHelper.hasConnectPermission(context)) {
            device.createBond();
        }
    }

    @Override
    public void setPin(BluetoothDevice device, String pin) {
        if (PermissionHelper.hasConnectPermission(context)) {
            device.setPin(pin.getBytes());
        }
    }

    @Override
    public LiveData<BluetoothDevice> getPairingRequest() {
        return pairingRequest;
    }
}
