package com.kresshy.weatherstation.application;

import android.app.Application;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.utils.ConnectionState;

public class WSApplication extends Application {

    private BluetoothConnection mConnectionService = null;
    private ConnectionState state = ConnectionState.disconnected;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public BluetoothConnection getConnectionService() {
        return mConnectionService;
    }

    public void setConnectionService(BluetoothConnection mConnectionService) {
        this.mConnectionService = mConnectionService;
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

}
