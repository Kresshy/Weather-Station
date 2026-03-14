package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.connection.ConnectionManager;
import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.connection.HardwareEventListener;
import com.kresshy.weatherstation.util.PermissionHelper;
import com.kresshy.weatherstation.util.Resource;

import dagger.hilt.android.qualifiers.ApplicationContext;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of the Control Plane. Orchestrates connection lifecycles, Bluetooth hardware
 * management, and discovery.
 */
@Singleton
public class WeatherConnectionControllerImpl implements WeatherConnectionController {

    private final Context context;
    private final ConnectionManager connectionManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final WeatherBluetoothManager bluetoothManager;
    private final SharedPreferences sharedPreferences;

    private final MutableLiveData<Resource<Void>> uiState = new MutableLiveData<>();
    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>();
    private final MutableLiveData<String> connectedDeviceName = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isDiscovering = new MutableLiveData<>(false);
    private final MutableLiveData<String> discoveryStatus = new MutableLiveData<>("");
    private final MutableLiveData<List<Parcelable>> discoveredDevices =
            new MutableLiveData<>(new ArrayList<>());

    private Parcelable lastConnectedDevice;
    private boolean shouldReconnect = false;
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private long reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
    private static final long INITIAL_RECONNECT_DELAY_MS = 2000;
    private static final long MAX_RECONNECT_DELAY_MS = 32000;

    /**
     * Constructs a new WeatherConnectionControllerImpl and initializes discovery stream bridging.
     *
     * @param context The application context.
     * @param connectionManager The manager for physical and virtual connections.
     * @param bluetoothAdapter The system Bluetooth adapter.
     * @param bluetoothManager The manager for hardware state and discovery.
     * @param sharedPreferences Access to persistent settings.
     */
    @Inject
    public WeatherConnectionControllerImpl(
            @ApplicationContext Context context,
            ConnectionManager connectionManager,
            @Nullable BluetoothAdapter bluetoothAdapter,
            WeatherBluetoothManager bluetoothManager,
            SharedPreferences sharedPreferences) {
        this.context = context;
        this.connectionManager = connectionManager;
        this.bluetoothAdapter = bluetoothAdapter;
        this.bluetoothManager = bluetoothManager;
        this.sharedPreferences = sharedPreferences;

        // Set initial states
        connectionState.postValue(ConnectionState.stopped);

        // Bridge discovery streams
        bluetoothManager
                .getDiscoveredDevices()
                .observeForever(devices -> discoveredDevices.postValue(new ArrayList<>(devices)));
        bluetoothManager.getDiscoveryStatus().observeForever(discoveryStatus::postValue);
        bluetoothManager.isDiscovering().observeForever(isDiscovering::postValue);
    }

    /**
     * Sets the primary listener for hardware and connection events. This listener will receive raw
     * data packets and connection state updates.
     *
     * @param listener The listener instance.
     */
    public void setHardwareEventListener(HardwareEventListener listener) {
        connectionManager.setListener(
                new HardwareEventListener() {
                    @Override
                    public void onRawDataReceived(String data) {
                        listener.onRawDataReceived(data);
                    }

                    @Override
                    public void onConnectionStateChange(ConnectionState state) {
                        handleConnectionStateChange(state);
                        listener.onConnectionStateChange(state);
                    }

                    @Override
                    public void onConnected() {
                        handleOnConnected();
                        listener.onConnected();
                    }

                    @Override
                    public void onToastMessage(String message) {
                        listener.onToastMessage(message);
                        if (message.toLowerCase().contains("failed")
                                || message.toLowerCase().contains("missing")) {
                            uiState.postValue(Resource.error(message, null));
                        }
                    }

                    @Override
                    public void onLogMessage(String message) {
                        listener.onLogMessage(message);
                    }
                });
    }

    private void handleConnectionStateChange(ConnectionState state) {
        connectionState.postValue(state);
        switch (state) {
            case connecting:
                uiState.postValue(Resource.loading(null));
                break;
            case connected:
                // Handled in onConnected()
                break;
            case disconnected:
            case stopped:
                uiState.postValue(Resource.error("Disconnected", null));
                connectedDeviceName.postValue(null);
                if (shouldReconnect && lastConnectedDevice != null) {
                    scheduleReconnect();
                }
                break;
        }
    }

    private void handleOnConnected() {
        Timber.d("Controller: Connection established.");
        connectionState.postValue(ConnectionState.connected);
        uiState.postValue(Resource.success(null));
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;

        if (lastConnectedDevice != null) {
            if (lastConnectedDevice instanceof BluetoothDevice) {
                if (PermissionHelper.hasConnectPermission(context)) {
                    connectedDeviceName.postValue(
                            ((BluetoothDevice) lastConnectedDevice).getName());
                } else {
                    connectedDeviceName.postValue("Weather Station");
                }
            } else if (lastConnectedDevice instanceof SimulatorDevice) {
                connectedDeviceName.postValue(((SimulatorDevice) lastConnectedDevice).getName());
            }
        }
    }

    private void scheduleReconnect() {
        Timber.d("Scheduling reconnect in %d ms", reconnectDelayMs);
        reconnectExecutor.schedule(
                () -> {
                    if (shouldReconnect && lastConnectedDevice != null) {
                        connectionManager.connectToDevice(lastConnectedDevice);
                    }
                },
                reconnectDelayMs,
                TimeUnit.MILLISECONDS);
        reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
    }

    /**
     * Provides access to the high-level UI status stream.
     *
     * @return LiveData containing the current UI resource status.
     */
    @Override
    public LiveData<Resource<Void>> getUiState() {
        return uiState;
    }

    /**
     * Provides access to the connection state stream.
     *
     * @return LiveData containing the current connection state.
     */
    @Override
    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }

    /**
     * Provides access to the name of the currently connected device.
     *
     * @return LiveData containing the device name string.
     */
    @Override
    public LiveData<String> getConnectedDeviceName() {
        return connectedDeviceName;
    }

    /**
     * Checks if discovery is currently active.
     *
     * @return LiveData indicating the discovery state.
     */
    @Override
    public LiveData<Boolean> isDiscovering() {
        return isDiscovering;
    }

    /**
     * Provides the current discovery status string.
     *
     * @return LiveData containing the status description.
     */
    @Override
    public LiveData<String> getDiscoveryStatus() {
        return discoveryStatus;
    }

    /**
     * Provides access to the Bluetooth adapter state stream.
     *
     * @return LiveData containing the adapter state constant.
     */
    @Override
    public LiveData<Integer> getBluetoothState() {
        return bluetoothManager.getBluetoothState();
    }

    /**
     * Provides access to the list of discovered devices.
     *
     * @return LiveData containing the list of discovered Parcelables.
     */
    @Override
    public LiveData<List<Parcelable>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    /**
     * Generates a list of paired hardware and virtual simulator devices.
     *
     * @return A list of paired devices and the simulator if enabled.
     */
    @Override
    public List<Parcelable> getPairedDevices() {
        boolean useSimulator = sharedPreferences.getBoolean("pref_simulator_mode", false);
        List<Parcelable> devices = new ArrayList<>();

        // 1. Add Simulator if enabled
        if (useSimulator) {
            devices.add(
                    new SimulatorDevice("Simulator Station", SimulatorDevice.SIMULATOR_ADDRESS));
        }

        // 2. Add Physical Paired Devices if Bluetooth is available and enabled
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (PermissionHelper.hasConnectPermission(context)) {
                java.util.Set<android.bluetooth.BluetoothDevice> bondedDevices =
                        bluetoothAdapter.getBondedDevices();
                if (bondedDevices != null) {
                    devices.addAll(bondedDevices);
                }
            } else {
                Timber.d("Controller: BLUETOOTH_CONNECT permission not granted");
            }
        }

        return devices;
    }

    /**
     * Checks if Bluetooth is currently active.
     *
     * @return true if Bluetooth is enabled.
     */
    @Override
    public boolean isBluetoothEnabled() {
        return bluetoothManager.isBluetoothEnabled();
    }

    /** Requests the Bluetooth adapter to be enabled. */
    @Override
    public void enableBluetooth() {
        bluetoothManager.enableBluetooth();
    }

    /** Disables the Bluetooth adapter. */
    @Override
    public void disableBluetooth() {
        bluetoothManager.disableBluetooth();
    }

    /** Resets the discovery result list. */
    @Override
    public void clearDiscoveredDevices() {
        discoveredDevices.postValue(new ArrayList<>());
    }

    /** Begins searching for nearby devices. */
    @Override
    public void startDiscovery() {
        bluetoothManager.startDiscovery();
    }

    /** Cancels any active device search. */
    @Override
    public void stopDiscovery() {
        bluetoothManager.stopDiscovery();
    }

    /** Configures the system broadcast receivers for Bluetooth events. */
    @Override
    public void registerReceivers() {
        bluetoothManager.registerReceivers();
    }

    /** Removes the system broadcast receivers. */
    @Override
    public void unregisterReceivers() {
        bluetoothManager.unregisterReceivers();
    }

    /** Activates background connection management and enables reconnection logic. */
    @Override
    public void startConnection() {
        shouldReconnect = true;
        connectionManager.startConnection();
    }

    /** Shuts down connection management and disables reconnection. */
    @Override
    public void stopConnection() {
        shouldReconnect = false;
        connectionManager.stopConnection();
    }

    /**
     * Attempts to connect to the specified device and tracks it for potential reconnection.
     *
     * @param device The target device.
     */
    @Override
    public void connectToDevice(Parcelable device) {
        lastConnectedDevice = device;
        shouldReconnect = true;
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
        connectionManager.connectToDevice(device);
    }

    /**
     * Attempts to connect to a device identified by its MAC address.
     *
     * @param address The MAC address of the device.
     */
    @Override
    public void connectToDeviceAddress(String address) {
        if (address.equals(SimulatorDevice.SIMULATOR_ADDRESS)
                && sharedPreferences.getBoolean("pref_simulator_mode", false)) {
            connectToDevice(new SimulatorDevice("Simulator Station", address));
        } else if (bluetoothAdapter != null) {
            try {
                connectToDevice(bluetoothAdapter.getRemoteDevice(address));
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Invalid address: %s", address);
            }
        }
    }

    /**
     * Initiates pairing with the specified Bluetooth device.
     *
     * @param device The device to pair with.
     */
    @Override
    public void pairDevice(BluetoothDevice device) {
        bluetoothManager.pairDevice(device);
    }

    /**
     * Supplies a PIN for an active pairing request.
     *
     * @param device The device requesting the PIN.
     * @param pin The PIN code string.
     */
    @Override
    public void setPin(BluetoothDevice device, String pin) {
        bluetoothManager.setPin(device, pin);
    }

    /**
     * Provides access to incoming pairing request events.
     *
     * @return LiveData emitting the device that requested pairing.
     */
    @Override
    public LiveData<BluetoothDevice> getPairingRequest() {
        return bluetoothManager.getPairingRequest();
    }
}
