package com.kresshy.weatherstation.utils;

import android.content.Context;

public class Utils {

    private Context context;

    public Utils(Context context) {
        this.context = context;
    }

    public boolean isBluetoothEnabled() {
        return true;
    }

    public boolean isBluetoothStateConnected() {
        return true;
    }

    public boolean isBluetoothStateDisconnected() {
        return true;
    }

    public boolean isWiFiEnabled() {
        return true;
    }

    public String getConnectionTypePreference() {
        return "";
    }
}
