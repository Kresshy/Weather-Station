package com.kresshy.weatherstation.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

/**
 * Helper class to manage Bluetooth and Location permissions across different Android versions.
 * Corrects issues where newer Bluetooth permissions (API 31+) were being checked on older devices.
 */
public class PermissionHelper {

    /**
     * Checks if the application has the necessary permissions to perform Bluetooth scanning. This
     * is required for discovering nearby weather stations. The check handles changes in permission
     * requirements introduced in Android S (API 31).
     *
     * @param context The application context.
     * @return true if scanning permissions are granted.
     */
    public static boolean hasScanPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean scanGranted =
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                            == PackageManager.PERMISSION_GRANTED;
            // Note: If the manifest does not use 'neverForLocation', we also need location.
            // For simplicity and safety, we check both.
            boolean locationGranted =
                    ActivityCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
            return scanGranted && locationGranted;
        } else {
            // On older versions, Location permission is required for Bluetooth scanning
            return ActivityCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Checks if the application has the necessary permissions to connect to Bluetooth devices. This
     * is required to establish a data link with the weather station. For devices running Android S
     * (API 31) and above, this requires the BLUETOOTH_CONNECT runtime permission.
     *
     * @param context The application context.
     * @return true if connection permissions are granted.
     */
    public static boolean hasConnectPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // On older versions, BLUETOOTH and BLUETOOTH_ADMIN are granted at install time
            // and don't require runtime checks, but they are declared in the manifest.
            return true;
        }
    }
}
