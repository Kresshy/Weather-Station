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

    /** Interface for handling application exit requests from the navigation system. */
    public interface QuitHandler {
        /**
         * Called when the user explicitly requests to quit the application via the navigation menu.
         */
        void onQuitRequested();
    }

    /**
     * Constructs a new NavigationDelegate.
     *
     * @param activity The host activity.
     * @param binding The view binding for the activity.
     * @param quitHandler The handler for application exit events.
     */
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
                                R.id.settingsFragment)
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

    /**
     * Handles the "up" navigation event (back arrow in toolbar).
     *
     * @return true if the navigation was handled by the NavController.
     */
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration);
    }

    /**
     * Delegates menu item selection to the navigation controller.
     *
     * @param item The selected menu item.
     * @return true if the item was handled and navigation occurred.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        return NavigationUI.onNavDestinationSelected(item, navController);
    }

    /**
     * Updates the text displayed in the activity's support action bar.
     *
     * @param title The title to set. If null, defaults to the current destination's label.
     */
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

    /**
     * Provides access to the Jetpack Navigation Controller.
     *
     * @return The active NavController instance.
     */
    public NavController getNavController() {
        return navController;
    }
}
