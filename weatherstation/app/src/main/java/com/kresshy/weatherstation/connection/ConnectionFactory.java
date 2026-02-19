package com.kresshy.weatherstation.connection;

import android.content.Context;
import android.preference.PreferenceManager;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;

/**
 * Factory class for creating {@link Connection} instances. Decides between a real {@link
 * BluetoothConnection} and a {@link SimulatorConnection} based on the user's "pref_simulator_mode"
 * preference.
 */
public class ConnectionFactory {

    /**
     * Instantiates and returns the appropriate connection type.
     *
     * @param context Application context used to read preferences.
     * @param callback Initial callback for the new connection.
     * @return A concrete Connection implementation.
     */
    public static Connection getConnection(Context context, RawDataCallback callback) {
        boolean useSimulator =
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean("pref_simulator_mode", false);

        if (useSimulator) {
            return new SimulatorConnection();
        }
        return new BluetoothConnection(context, callback);
    }
}
