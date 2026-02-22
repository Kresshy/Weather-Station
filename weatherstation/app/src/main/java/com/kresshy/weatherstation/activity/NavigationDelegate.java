package com.kresshy.weatherstation.activity;

import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.databinding.ActivityMainBinding;

/**
 * Delegate responsible for orchestrating Jetpack Navigation, Toolbar synchronization, and the
 * Navigation Drawer lifecycle.
 */
public class NavigationDelegate {

    private final AppCompatActivity activity;
    private final ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    public interface QuitHandler {
        void onQuitRequested();
    }

    public NavigationDelegate(
            AppCompatActivity activity, ActivityMainBinding binding, QuitHandler quitHandler) {
        this.activity = activity;
        this.binding = binding;
        setupNavigation(quitHandler);
    }

    private void setupNavigation(QuitHandler quitHandler) {
        activity.setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment =
                (NavHostFragment)
                        activity.getSupportFragmentManager()
                                .findFragmentById(R.id.nav_host_fragment);
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

        NavigationUI.setupActionBarWithNavController(activity, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.navView.setNavigationItemSelectedListener(
                item -> {
                    boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                    if (!handled && item.getItemId() == R.id.action_quit) {
                        quitHandler.onQuitRequested();
                        handled = true;
                    }
                    binding.drawerLayout.closeDrawers();
                    return handled;
                });
    }

    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return NavigationUI.onNavDestinationSelected(item, navController);
    }

    public void setToolbarTitle(String title) {
        if (activity.getSupportActionBar() != null) {
            if (title != null) {
                activity.getSupportActionBar().setTitle(title);
            } else if (navController != null && navController.getCurrentDestination() != null) {
                activity.getSupportActionBar()
                        .setTitle(navController.getCurrentDestination().getLabel());
            }
        }
    }

    public NavController getNavController() {
        return navController;
    }
}
