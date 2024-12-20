package com.kresshy.weatherstation.connection;

import android.os.Handler;
import android.os.Parcelable;

public interface Connection {
    public void setHandler(Handler handler);

    public void start();

    public void connect(Parcelable device);

    public void stop();

    public void write(byte[] out);

    public ConnectionState getState();
}
