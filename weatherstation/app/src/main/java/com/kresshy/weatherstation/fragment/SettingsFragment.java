package com.kresshy.weatherstation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from the XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        // Force a white background for visibility
        view.setBackgroundColor(getResources().getColor(android.R.color.white));

        return view;
    }
}
