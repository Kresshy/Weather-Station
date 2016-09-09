package com.kresshy.weatherstation.utils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kresshy.weatherstation.R;

/**
 * Created by Szabolcs on 2015.04.06..
 */
public class ReconnectHandler {

    private Context context;
    private String address;
    private SharedPreferences sharedPreferences;

    public ReconnectHandler(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public void saveBluetoothDevice() {

    }

    public String loadBluetoothDevice() {
        return sharedPreferences.getString(context.getString(R.string.PREFERENCE_DEVICE_ADDRESS), "00:00:00:00:00:00");
    }
}
