package com.kresshy.weatherstation.activity;

import android.app.AlertDialog;
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

    /** Shows the reconnection dialog if a previous device is known and preferred. */
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

    public void dismissLoadingSnackbar() {
        if (loadingSnackbar != null) loadingSnackbar.dismiss();
    }

    public void showErrorSnackbar(String message, Runnable retryAction) {
        dismissLoadingSnackbar();
        Snackbar.make(
                        binding.getRoot(),
                        activity.getString(R.string.error_prefix, message),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.retry_action, v -> retryAction.run())
                .show();
    }
}
