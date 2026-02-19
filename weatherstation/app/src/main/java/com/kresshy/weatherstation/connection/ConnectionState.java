package com.kresshy.weatherstation.connection;

/** Enum representing the possible states of a weather station connection. */
public enum ConnectionState {
    /** Hardware is disconnected but discovery might be running. */
    disconnected,

    /** Hardware is successfully connected and transmitting data. */
    connected,

    /** A connection attempt is currently in progress. */
    connecting,

    /** Connection service is stopped and not attempting to connect. */
    stopped
}
