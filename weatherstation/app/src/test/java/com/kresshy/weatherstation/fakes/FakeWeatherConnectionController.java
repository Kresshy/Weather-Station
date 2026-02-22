package com.kresshy.weatherstation.fakes;

import android.os.Parcelable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;
import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.util.Resource;

import java.util.ArrayList;
import java.util.List;

/** A fake implementation of {@link WeatherConnectionController} for unit and UI tests. */
public class FakeWeatherConnectionController implements WeatherConnectionController {

    private final MutableLiveData<Resource<Void>> uiState = new MutableLiveData<>();
    private final MutableLiveData<ConnectionState> connectionState =
            new MutableLiveData<>(ConnectionState.stopped);
    private final MutableLiveData<String> connectedDeviceName = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isDiscovering = new MutableLiveData<>(false);
    private final MutableLiveData<String> discoveryStatus = new MutableLiveData<>("");
    private final MutableLiveData<List<Parcelable>> discoveredDevices =
            new MutableLiveData<>(new ArrayList<>());
    private final List<Parcelable> pairedDevices = new ArrayList<>();
    private final MutableLiveData<Integer> bluetoothState = new MutableLiveData<>(0);

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
        return bluetoothState;
    }

    @Override
    public LiveData<List<Parcelable>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    @Override
    public List<Parcelable> getPairedDevices() {
        return pairedDevices;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return true;
    }

    @Override
    public void enableBluetooth() {}

    @Override
    public void disableBluetooth() {}

    @Override
    public void clearDiscoveredDevices() {}

    @Override
    public void startConnection() {}

    @Override
    public void stopConnection() {}

    @Override
    public void startDiscovery() {}

    @Override
    public void stopDiscovery() {}

    @Override
    public void connectToDevice(Parcelable device) {}

    @Override
    public void registerReceivers() {}

    @Override
    public void unregisterReceivers() {}

    @Override
    public void connectToDeviceAddress(String address) {}

    public void setUiState(Resource<Void> state) {
        uiState.postValue(state);
    }

    public void setConnectionState(ConnectionState state) {
        connectionState.postValue(state);
    }

    public void setConnectedDeviceName(String name) {
        connectedDeviceName.postValue(name);
    }

    public void setPairedDevices(List<Parcelable> devices) {
        pairedDevices.clear();
        pairedDevices.addAll(devices);
    }
}
