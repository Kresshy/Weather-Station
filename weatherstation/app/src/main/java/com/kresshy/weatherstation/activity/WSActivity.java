package com.kresshy.weatherstation.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.databinding.ActivityMainBinding;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.service.WeatherService;
import com.kresshy.weatherstation.weather.WeatherViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import timber.log.Timber;

import javax.inject.Inject;

/**
 * The main activity of the Weather Station application. Manages the navigation drawer, runtime
 * permissions for Bluetooth, and the foreground {@link WeatherService} lifecycle.
 */
@AndroidEntryPoint
public class WSActivity extends AppCompatActivity {
    @Inject SharedPreferences sharedPreferences;
    @Inject public WeatherRepository weatherRepository;

    @Inject
    public com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController;

    private WeatherViewModel weatherViewModel;
    private ActivityMainBinding binding;

    private PermissionDelegate permissionDelegate;
    private NavigationDelegate navigationDelegate;
    private UIEventDelegate uiEventDelegate;

    private boolean permissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("ONCREATE");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        // Initialize Delegates
        permissionDelegate =
                new PermissionDelegate(
                        this,
                        new PermissionDelegate.Callback() {
                            @Override
                            public void onPermissionsGranted() {
                                handlePermissionsGranted();
                            }

                            @Override
                            public void onPermissionsDenied() {
                                Snackbar.make(
                                                binding.getRoot(),
                                                R.string.permissions_required,
                                                Snackbar.LENGTH_INDEFINITE)
                                        .setAction(
                                                "ALLOW",
                                                v -> permissionDelegate.requestPermissions())
                                        .show();
                            }
                        });

        navigationDelegate = new NavigationDelegate(this, binding, this::quitApp);
        uiEventDelegate = new UIEventDelegate(this, binding, sharedPreferences, weatherViewModel);

        setupObservers();

        permissionDelegate.requestPermissions();
        connectionController.registerReceivers();
    }

    private void setupObservers() {
        navigationDelegate
                .getNavController()
                .addOnDestinationChangedListener(
                        (controller, destination, arguments) -> {
                            String deviceName =
                                    weatherViewModel.getConnectedDeviceName().getValue();
                            if (deviceName != null
                                    && (destination.getId() == R.id.dashboardFragment
                                            || destination.getId() == R.id.graphViewFragment)) {
                                navigationDelegate.setToolbarTitle(deviceName);
                            }
                        });

        weatherViewModel
                .getUiState()
                .observe(
                        this,
                        resource -> {
                            if (resource == null) return;
                            switch (resource.status) {
                                case LOADING:
                                    uiEventDelegate.showLoadingSnackbar();
                                    break;
                                case SUCCESS:
                                    uiEventDelegate.dismissLoadingSnackbar();
                                    if (navigationDelegate
                                                            .getNavController()
                                                            .getCurrentDestination()
                                                    != null
                                            && navigationDelegate
                                                            .getNavController()
                                                            .getCurrentDestination()
                                                            .getId()
                                                    == R.id.bluetoothDeviceListFragment) {
                                        navigationDelegate
                                                .getNavController()
                                                .navigate(R.id.dashboardFragment);
                                    }
                                    break;
                                case ERROR:
                                    uiEventDelegate.showErrorSnackbar(
                                            resource.message, this::startWeatherService);
                                    break;
                            }
                        });

        weatherViewModel.getDiscoveryStatus().observe(this, navigationDelegate::setToolbarTitle);

        weatherViewModel
                .getBluetoothState()
                .observe(
                        this,
                        state -> {
                            if (state == android.bluetooth.BluetoothAdapter.STATE_ON
                                    && permissionsGranted) {
                                uiEventDelegate.showReconnectDialogIfNeeded();
                            }
                        });
    }

    private void handlePermissionsGranted() {
        permissionsGranted = true;
        weatherViewModel.refreshPairedDevices();
        if (!connectionController.isBluetoothEnabled()) {
            connectionController.enableBluetooth();
        } else {
            uiEventDelegate.showReconnectDialogIfNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("ONRESUME");

        if (!connectionController.isBluetoothEnabled()) {
            if (permissionsGranted) {
                connectionController.enableBluetooth();
            }
        } else {
            uiEventDelegate.showReconnectDialogIfNeeded();
        }

        ConnectionState currentState = weatherViewModel.getConnectionState().getValue();
        if (currentState == null
                || currentState == ConnectionState.disconnected
                || currentState == ConnectionState.stopped) {
            if (permissionsGranted && connectionController.isBluetoothEnabled()) {
                startWeatherService();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navigationDelegate.onSupportNavigateUp() || super.onSupportNavigateUp();
    }

    /**
     * Updates the toolbar title. If title is null, defaults to the app name or current destination.
     *
     * @param title The title to display.
     */
    public void setToolbarTitle(String title) {
        navigationDelegate.setToolbarTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            return super.onOptionsItemSelected(item);
        }
        return navigationDelegate.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /** Safely shuts down the app, stopping services and disabling Bluetooth if required. */
    private void quitApp() {
        stopWeatherService();
        boolean disableBluetoothOnQuit =
                sharedPreferences.getBoolean("pref_disable_bluetooth_on_quit", false);
        if (disableBluetoothOnQuit && connectionController.isBluetoothEnabled()) {
            Timber.d("Disabling bluetooth adapter as per user preference");
            connectionController.disableBluetooth();
        }
        finish();
    }

    private void startWeatherService() {
        Intent serviceIntent = new Intent(this, WeatherService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopWeatherService() {
        Intent serviceIntent = new Intent(this, WeatherService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("ONDESTROY");
        stopWeatherService();
        connectionController.unregisterReceivers();
    }
}
