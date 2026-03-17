package com.kresshy.weatherstation.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.databinding.ActivityMainBinding;
import com.kresshy.weatherstation.weather.WeatherViewModel;

/**
 * Delegate responsible for managing complex UI events like reconnection dialogs and persistent
 * state notifications (Snackbars).
 */
public class UIEventDelegate {

    private final AppCompatActivity activity;
    private final ActivityMainBinding binding;
    private final SharedPreferences sharedPreferences;
    private final WeatherViewModel viewModel;

    private boolean isReconnectDialogShowing = false;
    private Snackbar loadingSnackbar;

    /**
     * Constructs a new UIEventDelegate.
     *
     * @param activity The host activity.
     * @param binding The activity's view binding.
     * @param sharedPreferences Access to application settings.
     * @param viewModel The view model for processing business logic.
     */
    public UIEventDelegate(
            AppCompatActivity activity,
            ActivityMainBinding binding,
            SharedPreferences sharedPreferences,
            WeatherViewModel viewModel) {
        this.activity = activity;
        this.binding = binding;
        this.sharedPreferences = sharedPreferences;
        this.viewModel = viewModel;
    }

    /**
     * Evaluates whether a reconnection attempt should be offered to the user based on persistent
     * settings and previous connection history. Displays a modal dialog if appropriate.
     */
    public void showReconnectDialogIfNeeded() {
        if (isReconnectDialogShowing) return;

        if (sharedPreferences.getBoolean("pref_reconnect", false)) {
            final String address =
                    sharedPreferences.getString(
                            activity.getString(R.string.PREFERENCE_DEVICE_ADDRESS),
                            "00:00:00:00:00:00");

            if (!address.equals("00:00:00:00:00:00")) {
                isReconnectDialogShowing = true;
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.reconnect_message)
                        .setPositiveButton(
                                R.string.ok,
                                (dialog, id) -> {
                                    viewModel.connectToDeviceAddress(address);
                                    isReconnectDialogShowing = false;
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                (dialog, id) -> {
                                    dialog.cancel();
                                    isReconnectDialogShowing = false;
                                })
                        .setOnCancelListener(dialog -> isReconnectDialogShowing = false)
                        .show();
            }
        }
    }

    /**
     * Displays an indefinite Snackbar to inform the user that a hardware connection is currently
     * being established.
     */
    public void showLoadingSnackbar() {
        if (loadingSnackbar == null) {
            loadingSnackbar =
                    Snackbar.make(
                            binding.getRoot(),
                            R.string.connecting_message,
                            Snackbar.LENGTH_INDEFINITE);
        }
        loadingSnackbar.show();
    }

    /** Removes the active connection progress indicator from the screen. */
    public void dismissLoadingSnackbar() {
        if (loadingSnackbar != null) loadingSnackbar.dismiss();
    }

    /**
     * Displays a temporary error message to the user with an option to retry the failed operation.
     *
     * @param message The error description to display.
     * @param retryAction The action to execute if the user taps "Retry".
     */
    public void showErrorSnackbar(String message, Runnable retryAction) {
        dismissLoadingSnackbar();
        Snackbar.make(
                        binding.getRoot(),
                        activity.getString(R.string.error_prefix, message),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.retry_action, v -> retryAction.run())
                .show();
    }

    /**
     * Displays a modal Dialog alerting the user that system-level Location Services are required
     * for Bluetooth discovery, providing a direct link to the system settings.
     */
    public void showLocationServicesDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Location Services Required")
                .setMessage(
                        "System-level Location Services must be enabled to discover nearby weather"
                                + " stations.")
                .setPositiveButton(
                        "GO TO SETTINGS",
                        (dialog, id) -> {
                            activity.startActivity(
                                    new Intent(
                                            android.provider.Settings
                                                    .ACTION_LOCATION_SOURCE_SETTINGS));
                        })
                .setNegativeButton("CANCEL", (dialog, id) -> dialog.cancel())
                .show();
    }
}
