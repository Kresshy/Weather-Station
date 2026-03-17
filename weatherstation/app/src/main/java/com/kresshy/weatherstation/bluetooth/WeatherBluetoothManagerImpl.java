package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
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
 * and BluetoothLeScanner to monitor Bluetooth state and discover new devices (Classic + BLE).
 */
@Singleton
public class WeatherBluetoothManagerImpl implements WeatherBluetoothManager {

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final MutableLiveData<List<BluetoothDevice>> discoveredDevices =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> discoveryStatus = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isDiscovering = new MutableLiveData<>(false);
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
                        bluetoothState.setValue(state);
                        if (state == BluetoothAdapter.STATE_ON) {
                            Timber.d("Bluetooth turned ON");
                        } else if (state == BluetoothAdapter.STATE_OFF) {
                            Timber.d("Bluetooth turned OFF");
                            isDiscovering.setValue(false);
                            discoveryStatus.setValue("Bluetooth Off");
                        }
                    }
                }
            };

    /**
     * Receiver for handling discovered classic devices, bond state changes and pairing requests.
     */
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
                            if (PermissionHelper.hasConnectPermission(context)) {
                                Timber.d(
                                        "Classic Device found: %s (%s) [RSSI: %d]",
                                        device.getName(), device.getAddress(), rssi);
                            } else {
                                Timber.d(
                                        "Classic Device found: %s [RSSI: %d]",
                                        device.getAddress(), rssi);
                            }
                            updateDeviceList(device);
                        }
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                        Timber.d("Classic Discovery Started");
                        isDiscovering.setValue(true);
                        discoveryStatus.setValue("Discovering...");
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        Timber.d("Classic Discovery Finished");
                        isDiscovering.setValue(false);
                        discoveryStatus.setValue("Discovery Finished");
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
                            pairingRequest.setValue(device);
                        }
                    }
                }
            };

    /** Callback for handling BLE scan results. Package-private for testing. */
    final ScanCallback bleScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice device = result.getDevice();
                    int rssi = result.getRssi();
                    deviceRssiMap.put(device.getAddress(), rssi);

                    if (PermissionHelper.hasConnectPermission(context)) {
                        Timber.v(
                                "BLE Device found: %s (%s) [RSSI: %d]",
                                device.getName(), device.getAddress(), rssi);
                    } else {
                        Timber.v("BLE Device found: %s [RSSI: %d]", device.getAddress(), rssi);
                    }
                    updateDeviceList(device);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult result : results) {
                        onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Timber.e("BLE Scan Failed with code: %d", errorCode);
                }
            };

    private void updateDeviceList(BluetoothDevice device) {
        List<BluetoothDevice> current = discoveredDevices.getValue();
        List<BluetoothDevice> newList = new ArrayList<>();
        if (current != null) {
            newList.addAll(current);
        }

        int index = -1;
        for (int i = 0; i < newList.size(); i++) {
            if (newList.get(i).getAddress().equals(device.getAddress())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            BluetoothDevice existing = newList.get(index);
            // If the existing device has a known type but the new one is UNKNOWN, keep the existing
            // one (but maybe update the name/RSSI via other paths if needed, but for now we
            // prioritize type)
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_UNKNOWN
                    && existing.getType() != BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                // Keep existing to preserve type information
                return;
            }
            newList.set(index, device);
        } else {
            newList.add(device);
        }
        discoveredDevices.setValue(newList);
    }

    /**
     * Constructs a new WeatherBluetoothManagerImpl.
     *
     * @param context The application context.
     * @param bluetoothAdapter The system Bluetooth adapter.
     */
    @Inject
    public WeatherBluetoothManagerImpl(
            @ApplicationContext Context context, @Nullable BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        if (bluetoothAdapter != null) {
            bluetoothState.postValue(bluetoothAdapter.getState());
        }
    }

    /**
     * Initiates a search for nearby Bluetooth devices. This starts both classic Bluetooth discovery
     * and BLE scanning to find all compatible weather station hardware.
     */
    @Override
    public void startDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (PermissionHelper.hasScanPermission(context)) {
                discoveredDevices.postValue(new ArrayList<>());
                discoveryStatus.postValue("Discovering...");
                isDiscovering.postValue(true);

                // 1. Start Classic Bluetooth Discovery
                bluetoothAdapter.startDiscovery();

                // 2. Start BLE Scanning
                BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
                if (scanner != null) {
                    ScanSettings settings =
                            new ScanSettings.Builder()
                                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                    .build();
                    scanner.startScan(null, settings, bleScanCallback);
                    Timber.d("BLE Scanning started");
                } else {
                    Timber.w("BluetoothLeScanner not available");
                }
            } else {
                Timber.e("Missing scan permission for discovery");
                discoveryStatus.postValue("Missing Permissions");
            }
        }
    }

    /**
     * Terminates any active Bluetooth discovery or BLE scan. This should be called when discovery
     * is no longer needed to conserve battery power.
     */
    @Override
    public void stopDiscovery() {
        if (bluetoothAdapter != null) {
            if (PermissionHelper.hasScanPermission(context)) {
                // 1. Stop Classic Bluetooth Discovery
                bluetoothAdapter.cancelDiscovery();

                // 2. Stop BLE Scanning
                BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
                if (scanner != null) {
                    scanner.stopScan(bleScanCallback);
                    Timber.d("BLE Scanning stopped");
                }
            }
            isDiscovering.postValue(false);
            discoveryStatus.postValue("Discovery Finished");
        }
    }

    /**
     * Programmatically enables the Bluetooth adapter. This is used to ensure Bluetooth is active
     * before attempting discovery or connection.
     */
    @Override
    public void enableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (PermissionHelper.hasConnectPermission(context)) {
                bluetoothAdapter.enable();
            }
        }
    }

    /**
     * Programmatically disables the Bluetooth adapter. This can be used as part of an application
     * exit sequence to restore hardware state.
     */
    @Override
    public void disableBluetooth() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (PermissionHelper.hasConnectPermission(context)) {
                bluetoothAdapter.disable();
            }
        }
    }

    /**
     * Checks the current enablement state of the Bluetooth hardware.
     *
     * @return true if Bluetooth is enabled and ready for use.
     */
    @Override
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Provides an observable stream for the current discovery state.
     *
     * @return A LiveData object indicating if a scan is currently in progress.
     */
    @Override
    public LiveData<Boolean> isDiscovering() {
        return isDiscovering;
    }

    /**
     * Sets up the necessary system-level broadcast receivers to monitor Bluetooth state and
     * discovery results.
     */
    @Override
    public void registerReceivers() {
        if (!isRegistered) {
            IntentFilter stateFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            IntentFilter discoveryFilter = new IntentFilter();
            discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
            discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            discoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            discoveryFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(stateReceiver, stateFilter, Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(
                        discoveryReceiver, discoveryFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(stateReceiver, stateFilter);
                context.registerReceiver(discoveryReceiver, discoveryFilter);
            }
            isRegistered = true;
        }
    }

    /**
     * Tears down the system-level broadcast receivers. This must be called when the manager is no
     * longer active to prevent memory leaks.
     */
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

    /**
     * Provides an observable list of all Bluetooth devices discovered during the current scan
     * session.
     *
     * @return A LiveData list of discovered devices.
     */
    @Override
    public LiveData<List<BluetoothDevice>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    /**
     * Provides an observable string describing the current progress of the discovery operation.
     *
     * @return A LiveData string representing the discovery status.
     */
    @Override
    public LiveData<String> getDiscoveryStatus() {
        return discoveryStatus;
    }

    /**
     * Provides an observable stream for the Bluetooth adapter's state (ON, OFF, etc.).
     *
     * @return A LiveData integer representing the adapter state.
     */
    @Override
    public LiveData<Integer> getBluetoothState() {
        return bluetoothState;
    }

    /**
     * Retrieves the last recorded signal strength (RSSI) for a specific device.
     *
     * @param address The MAC address of the device.
     * @return The signal strength in dBm, or 0 if unknown.
     */
    @Override
    public int getDeviceRssi(String address) {
        Integer rssi = deviceRssiMap.get(address);
        return rssi != null ? rssi : 0;
    }

    /**
     * Attempts to initiate the standard Bluetooth pairing process with the specified device.
     *
     * @param device The Bluetooth device to pair with.
     */
    @Override
    public void pairDevice(BluetoothDevice device) {
        if (PermissionHelper.hasConnectPermission(context)) {
            device.createBond();
        }
    }

    /**
     * Supplies a PIN code to the system for an ongoing pairing request.
     *
     * @param device The device requesting the PIN.
     * @param pin The PIN code string.
     */
    @Override
    public void setPin(BluetoothDevice device, String pin) {
        if (PermissionHelper.hasConnectPermission(context)) {
            device.setPin(pin.getBytes());
        }
    }

    /**
     * Provides an observable event that triggers when a Bluetooth pairing request is received from
     * a remote device.
     *
     * @return A LiveData object emitting the device that requested pairing.
     */
    @Override
    public LiveData<BluetoothDevice> getPairingRequest() {
        return pairingRequest;
    }
}
