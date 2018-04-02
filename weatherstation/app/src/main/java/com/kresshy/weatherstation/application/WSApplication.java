package com.kresshy.weatherstation.application;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.logging.FileLoggingTree;
import com.kresshy.weatherstation.utils.ConnectionState;
import timber.log.Timber;

public class WSApplication extends Application {

    private BluetoothConnection mConnectionService = null;
    private ConnectionState state = ConnectionState.disconnected;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Timber.plant(new Timber.DebugTree());
        if (Boolean.parseBoolean(sharedPreferences.getString("pref_logging_enabled", Boolean.toString(Boolean.FALSE)))) {
            Timber.plant(new FileLoggingTree(getApplicationContext()));
        }

        Timber.d("ONCREATE");
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
