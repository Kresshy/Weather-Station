package com.kresshy.weatherstation.bluetooth;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class BluetoothService extends Service {

    private static BluetoothConnection bluetoothConnection;
    private final IBinder mBinder = new BluetoothServiceBinder();

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
