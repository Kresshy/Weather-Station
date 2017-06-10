package com.kresshy.weatherstation.wifi;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.Getter;
import lombok.Setter;


public class WifiDevice implements Parcelable {

    @Getter
    @Setter
    private String ip;

    @Getter
    @Setter
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
}
