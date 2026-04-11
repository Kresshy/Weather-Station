package com.kresshy.weatherstation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.kresshy.weatherstation.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment that displays and manages application settings using the Android Preference library.
 * Allows users to toggle simulator mode, change sampling intervals, and enable file logging.
 */
@AndroidEntryPoint
public class SettingsFragment extends PreferenceFragmentCompat {

    /** Key for the measurement display interval preference. */
    public static final String KEY_PREF_INTERVAL = "pref_interval";

    /** Required empty public constructor for fragment instantiation. */
    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Called during {@link #onCreate(Bundle)} to supply the preferences for this fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     *     this is the state.
     * @param rootKey If non-null, this preference fragment should be rooted at the {@link
     *     androidx.preference.PreferenceScreen} with this key.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from the XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference manageLogsPref = findPreference("pref_manage_logs");
        if (manageLogsPref != null) {
            manageLogsPref.setOnPreferenceClickListener(
                    preference -> {
                        NavHostFragment.findNavController(this).navigate(R.id.logManagerFragment);
                        return true;
                    });
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view. Ensures the background is
     * explicitly white for consistent visibility.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the
     *     fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be
     *     attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        // Force a white background for visibility
        view.setBackgroundColor(getResources().getColor(android.R.color.white));

        return view;
    }
}
