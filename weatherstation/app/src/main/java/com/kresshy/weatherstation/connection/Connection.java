package com.kresshy.weatherstation.connection;

import android.os.Handler;
import android.os.Parcelable;

import com.kresshy.weatherstation.utils.ConnectionState;


public interface Connection {
    void setHandler(Handler handler);

    void start();

    void connect(Parcelable device);

    void stop();

    void write(byte[] out);

    ConnectionState getState();
}
