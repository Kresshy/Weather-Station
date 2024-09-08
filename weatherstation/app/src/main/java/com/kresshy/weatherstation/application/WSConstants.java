package com.kresshy.weatherstation.application;

public class WSConstants {

    // preferences
    public static final String KEY_PREF_INTERVAL = "pref_interval";
    public static final String KEY_PREF_RECONNECT = "pref_reconnect";
    public static final String KEY_PREF_CONNECTION_TYPE = "pref_connection_type";

    // requests
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_DISABLE_BT = 2;

    // messages
    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_STATE = 3;
    public static final int MESSAGE_CONNECTED = 4;

    public static final int MESSAGE_LOG = 5;

    // numbers
    public static final int NUM_SAMPLES = 300;

    // wifi server
    public static final int SERVER_PORT = 3000;
    public static final String SERVER_IP = "192.168.0.112";

    // calibration
    public static final String KEY_NODE_ID = "KEY_NODE_ID";
    public static final String KEY_WIND_DIFF = "KEY_WIND_DIFF";
    public static final String KEY_TEMP_DIFF = "KEY_TEMP_DIFF";

}
