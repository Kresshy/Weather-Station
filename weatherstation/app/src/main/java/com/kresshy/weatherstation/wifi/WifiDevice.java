package com.kresshy.weatherstation.wifi;

import android.os.Parcel;
import android.os.Parcelable;


public class WifiDevice implements Parcelable {

    private String ip;
    private int port;

    public WifiDevice(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    protected WifiDevice(Parcel in) {
    }

    public static final Creator<WifiDevice> CREATOR = new Creator<WifiDevice>() {
        @Override
        public WifiDevice createFromParcel(Parcel in) {
            return new WifiDevice(in);
        }

        @Override
        public WifiDevice[] newArray(int size) {
            return new WifiDevice[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
