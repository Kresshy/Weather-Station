package com.kresshy.weatherstation.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A mock representation of a Bluetooth device. Used to display the virtual station in the device
 * list when simulator mode is enabled.
 */
public class SimulatorDevice implements Parcelable {
    /** Fixed MAC address used for the simulator. */
    public static final String SIMULATOR_ADDRESS = "00:11:22:33:44:55";

    private final String name;
    private final String address;

    /**
     * @param name Display name of the simulator.
     * @param address MAC address of the simulator.
     */
    public SimulatorDevice(String name, String address) {
        this.name = name;
        this.address = address;
    }

    protected SimulatorDevice(Parcel in) {
        name = in.readString();
        address = in.readString();
    }

    public static final Creator<SimulatorDevice> CREATOR =
            new Creator<SimulatorDevice>() {
                @Override
                public SimulatorDevice createFromParcel(Parcel in) {
                    return new SimulatorDevice(in);
                }

                @Override
                public SimulatorDevice[] newArray(int size) {
                    return new SimulatorDevice[size];
                }
            };

    /**
     * @return The device name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The device MAC address.
     */
    public String getAddress() {
        return address;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
    }
}
