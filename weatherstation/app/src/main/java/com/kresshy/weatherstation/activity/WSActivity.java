package com.kresshy.weatherstation.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.bluetooth.WeatherBluetoothManager;
import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.databinding.ActivityMainBinding;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.service.WeatherService;
import com.kresshy.weatherstation.weather.WeatherViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

/**
 * The main activity of the Weather Station application. Manages the navigation drawer, runtime
 * permissions for Bluetooth, and the foreground {@link WeatherService} lifecycle.
 */
@AndroidEntryPoint
public class WSActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private boolean requestedEnableBluetooth = false;

    private boolean permissionsGranted;

    @Inject public WeatherRepository weatherRepository;
    @Inject public WeatherBluetoothManager weatherBluetoothManager;

    private WeatherViewModel weatherViewModel;

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private Snackbar loadingSnackbar;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::handlePermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("ONCREATE");

        // setting up view with ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // Set up NavController
        NavHostFragment navHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        appBarConfiguration =
                new AppBarConfiguration.Builder(
                                R.id.dashboardFragment,
                                R.id.graphViewFragment,
                                R.id.bluetoothDeviceListFragment,
                                R.id.settingsFragment,
                                R.id.calibrationFragment)
                        .setOpenableLayout(binding.drawerLayout)
                        .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.navView.setNavigationItemSelectedListener(
                item -> {
                    boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                    if (!handled) {
                        if (item.getItemId() == R.id.action_quit) {
                            quitApp();
                            handled = true;
                        }
                    }
                    binding.drawerLayout.closeDrawers();
                    return handled;
                });

        // ask all runtime permissions
        ArrayList<String> permissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            permissionList.add(Manifest.permission.BLUETOOTH);
            permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
            // Location only needed for Bluetooth scanning on older Android
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        Timber.d("Requesting Permissions!...");
        requestPermissionLauncher.launch(permissionList.toArray(new String[0]));

        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        // setting up sharedpreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        weatherBluetoothManager.registerReceivers();

        weatherViewModel
                .getToastMessage()
                .observe(
                        this,
                        message -> {
                            // Redundant, UI state handled by getUiState() Snackbars
                        });

        weatherViewModel
                .getUiState()
                .observe(
                        this,
                        resource -> {
                            if (resource == null) return;
                            switch (resource.status) {
                                case LOADING:
                                    if (loadingSnackbar == null) {
                                        loadingSnackbar =
                                                Snackbar.make(
                                                        binding.getRoot(),
                                                        R.string.connecting_message,
                                                        Snackbar.LENGTH_INDEFINITE);
                                    }
                                    loadingSnackbar.show();
                                    break;
                                case SUCCESS:
                                    if (loadingSnackbar != null) loadingSnackbar.dismiss();
                                    if (navController != null
                                            && navController.getCurrentDestination() != null
                                            && navController.getCurrentDestination().getId()
                                                    == R.id.bluetoothDeviceListFragment) {
                                        navController.navigate(R.id.dashboardFragment);
                                    }
                                    break;
                                case ERROR:
                                    if (loadingSnackbar != null) loadingSnackbar.dismiss();
                                    Snackbar.make(
                                                    binding.getRoot(),
                                                    getString(
                                                            R.string.error_prefix,
                                                            resource.message),
                                                    Snackbar.LENGTH_LONG)
                                            .setAction(
                                                    R.string.retry_action,
                                                    v -> startWeatherService())
                                            .show();
                                    break;
                            }
                        });

        weatherViewModel
                .isDiscovering()
                .observe(
                        this,
                        isDiscovering -> {
                            // TODO: replace deprecated method if possible
                            // setSupportProgressBarIndeterminateVisibility(isDiscovering);
                        });

        weatherViewModel
                .getDiscoveryStatus()
                .observe(
                        this,
                        status -> {
                            setTitle(status);
                        });
    }

    private void handlePermissionResult(Map<String, Boolean> result) {
        Timber.d("handlePermissionResult");
        boolean mandatoryGranted = true;

        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            String permission = entry.getKey();
            boolean granted = entry.getValue();
            Timber.d("Permission: %s was %s!", permission, granted ? "granted" : "not granted");

            // Define which permissions are absolute deal-breakers
            if (!granted) {
                if (permission.equals(Manifest.permission.BLUETOOTH_CONNECT)
                        || permission.equals(Manifest.permission.BLUETOOTH_SCAN)
                        || permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)
                        || permission.equals(Manifest.permission.BLUETOOTH)
                        || permission.equals(Manifest.permission.BLUETOOTH_ADMIN)) {
                    mandatoryGranted = false;
                }
            }
        }

        if (mandatoryGranted) {
            permissionsGranted = true;
            weatherRepository.refreshPairedDevices();
            if (!requestedEnableBluetooth) {
                Timber.d("Starting Weather Service");
                startWeatherService();
                requestedEnableBluetooth = true;
            }
        } else {
            Snackbar.make(
                            binding.getRoot(),
                            R.string.permissions_required,
                            Snackbar.LENGTH_INDEFINITE)
                    .setAction("ALLOW", v -> relaunchPermissions())
                    .show();
        }
    }

    private void relaunchPermissions() {
        ArrayList<String> permissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            permissionList.add(Manifest.permission.BLUETOOTH);
            permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        requestPermissionLauncher.launch(permissionList.toArray(new String[0]));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("ONSTART");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (permissionsGranted) {
            if (!requestedEnableBluetooth) {
                // connectionManager.enableConnection(); // Removed
                requestedEnableBluetooth = true;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("ONRESUME");

        // checking bluetoothadapter and bluetoothservice and make sure it is started
        if (!weatherBluetoothManager.isBluetoothEnabled() && !requestedEnableBluetooth) {
            if (permissionsGranted) {
                requestedEnableBluetooth = true;
                // connectionManager.enableConnection(); // Removed
            }
        } else if (weatherBluetoothManager.isBluetoothEnabled()) {
            reconnectPreviousWeatherStation();
            ConnectionState currentState = weatherViewModel.getConnectionState().getValue();
            if (currentState == null) {
                // Handle null case, e.g., default to disconnected
                currentState = ConnectionState.disconnected;
            }
            switch (currentState) { // Using new LiveData
                case connected:
                case connecting:
                    break;
                case disconnected:
                case stopped:
                default:
                    if (permissionsGranted) {
                        startWeatherService();
                    }
                    break;
            }
        }
    }

    /** Checks if the user previously connected to a device and offers to reconnect. */
    public void reconnectPreviousWeatherStation() {
        if (sharedPreferences.getBoolean("pref_reconnect", false)) {
            Timber.d("We should restore the connection");
            final String address =
                    sharedPreferences.getString(
                            getString(R.string.PREFERENCE_DEVICE_ADDRESS), "00:00:00:00:00:00");

            if (!address.equals("00:00:00:00:00:00")) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.reconnect_message);
                builder.setPositiveButton(
                        R.string.ok,
                        (dialog, id) -> {
                            Timber.d("The device address is valid, attempting to reconnect");
                            weatherRepository.connectToDeviceAddress(address);
                        });

                builder.setNegativeButton(
                        R.string.cancel,
                        (dialog, id) -> {
                            Timber.d("We couldn't restore the connection");
                            dialog.cancel();
                        });

                builder.create().show();
            } else {
                Timber.d("The device address was invalid");
            }
        } else {
            Timber.d("We shouldn't restore the connection");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Timber.d("ONSAVEDINSTANCE");

        if (weatherViewModel.getConnectionState().getValue()
                == ConnectionState.connected) { // Using new LiveData
            Timber.d("Connected to a device");
            outState.putBoolean(getString(R.string.PREFERENCE_CONNECTED), true);
        } else {
            Timber.d("Not connected to a device");
            outState.putBoolean(getString(R.string.PREFERENCE_CONNECTED), false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Timber.d("ONPAUSE");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("ONDESTROY");

        stopWeatherService();
        weatherBluetoothManager.unregisterReceivers();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (binding.drawerLayout != null
                && !binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle 'home' button (hamburger or up arrow) via NavController
        if (item.getItemId() == android.R.id.home) {
            return super.onOptionsItemSelected(item);
        }

        if (NavigationUI.onNavDestinationSelected(item, navController)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_quit) {
            quitApp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Safely shuts down the app, stopping services and disabling Bluetooth if required. */
    private void quitApp() {
        stopWeatherService();
        if (weatherBluetoothManager.isBluetoothEnabled()) {
            Timber.d("Disabling bluetooth adapter");
            weatherBluetoothManager.disableBluetooth();
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
}
