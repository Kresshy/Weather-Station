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
import com.kresshy.weatherstation.connection.RawDataCallback;
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
    }

    public void setDataCallback(RawDataCallback callback) {
        connectionManager.setCallback(
                new RawDataCallback() {
                    @Override
                    public void onRawDataReceived(String data) {
                        callback.onRawDataReceived(data);
                    }

                    @Override
                    public void onConnectionStateChange(ConnectionState state) {
                        handleConnectionStateChange(state);
                        callback.onConnectionStateChange(state);
                    }

                    @Override
                    public void onConnected() {
                        handleOnConnected();
                        callback.onConnected();
                    }

                    @Override
                    public void onToastMessage(String message) {
                        callback.onToastMessage(message);
                        if (message.toLowerCase().contains("failed")
                                || message.toLowerCase().contains("missing")) {
                            uiState.postValue(Resource.error(message, null));
                        }
                    }

                    @Override
                    public void onLogMessage(String message) {
                        callback.onLogMessage(message);
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

    @Override
    public LiveData<Resource<Void>> getUiState() {
        return uiState;
    }

    @Override
    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }

    @Override
    public LiveData<String> getConnectedDeviceName() {
        return connectedDeviceName;
    }

    @Override
    public LiveData<Boolean> isDiscovering() {
        return isDiscovering;
    }

    @Override
    public LiveData<String> getDiscoveryStatus() {
        return discoveryStatus;
    }

    @Override
    public LiveData<Integer> getBluetoothState() {
        return bluetoothManager.getBluetoothState();
    }

    @Override
    public LiveData<List<Parcelable>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return bluetoothManager.isBluetoothEnabled();
    }

    @Override
    public void enableBluetooth() {
        bluetoothManager.enableBluetooth();
    }

    @Override
    public void disableBluetooth() {
        bluetoothManager.disableBluetooth();
    }

    @Override
    public void clearDiscoveredDevices() {
        discoveredDevices.postValue(new ArrayList<>());
    }

    @Override
    public void startDiscovery() {
        bluetoothManager.startDiscovery();
    }

    @Override
    public void stopDiscovery() {
        bluetoothManager.stopDiscovery();
    }

    @Override
    public void registerReceivers() {
        bluetoothManager.registerReceivers();
    }

    @Override
    public void unregisterReceivers() {
        bluetoothManager.unregisterReceivers();
    }

    @Override
    public void startConnection() {
        shouldReconnect = true;
        connectionManager.startConnection();
    }

    @Override
    public void stopConnection() {
        shouldReconnect = false;
        connectionManager.stopConnection();
    }

    @Override
    public void connectToDevice(Parcelable device) {
        lastConnectedDevice = device;
        shouldReconnect = true;
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
        connectionManager.connectToDevice(device);
    }

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
}
