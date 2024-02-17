package com.kresshy.weatherstation.wifi;

import com.google.auto.value.AutoValue;
import android.os.Parcel;
import android.os.Parcelable;

@AutoValue
public abstract class WifiDevice implements Parcelable {

    public static WifiDevice create(String ip, int port) {
        return new AutoValue_WifiDevice.Builder().setIp(ip).setPort(port).build();
    }

    private static WifiDevice createWithParcel(Parcel in) {
        return new AutoValue_WifiDevice.Builder().setIp("").setPort(0).build();
    }

    WifiDevice() {}

    protected WifiDevice(Parcel in) {}

    public static final Creator<WifiDevice> CREATOR = new Creator<WifiDevice>() {
        @Override
        public WifiDevice createFromParcel(Parcel in) {
            return createWithParcel(in);
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

    // private members & builder

    abstract String ip();

    abstract int port();

    abstract Builder toBuilder();

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder setIp(String ip);

        abstract Builder setPort(int port);

        abstract WifiDevice build();
    }
}
