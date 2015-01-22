package com.kresshy.weatherstation.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;

public class BluetoothService extends Service {

    private static BluetoothConnection bluetoothConnection;
    private final IBinder mBinder = new BluetoothServiceBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BluetoothServiceBinder extends Binder {
        BluetoothService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothService.this;
        }
    }

    public BluetoothService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothConnection = BluetoothConnection.getInstance(bluetoothHandler, getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    Handler bluetoothHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {

            };
        }
    };
}
