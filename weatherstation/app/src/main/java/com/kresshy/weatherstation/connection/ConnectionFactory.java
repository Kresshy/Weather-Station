package com.kresshy.weatherstation.connection;

import android.app.Activity;
import android.os.Handler;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;

public class ConnectionFactory {

    public static Connection getConnection(Handler handler, Activity activity) {
        return BluetoothConnection.getInstance(handler, activity);
    }
}
