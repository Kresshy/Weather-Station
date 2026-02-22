package com.kresshy.weatherstation.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.databinding.FragmentCalibrationBinding;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.WeatherViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/**
 * Fragment that allows the user to manually enter calibration offsets for sensors. These offsets
 * (wind speed delta and temperature delta) are applied to the raw data in the repository.
 */
@AndroidEntryPoint
public class CalibrationFragment extends Fragment implements View.OnClickListener {

    private WeatherViewModel weatherViewModel;
    private FragmentCalibrationBinding binding;
    @Inject SharedPreferences sharedPreferences;

    public CalibrationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalibrationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load current offsets into the edit texts
        String currentWindOffset =
                sharedPreferences.getString(WeatherRepository.KEY_WIND_DIFF, "0.0");
        String currentTempOffset =
                sharedPreferences.getString(WeatherRepository.KEY_TEMP_DIFF, "0.0");

        binding.windOffsetEdit.setText(currentWindOffset);
        binding.tempOffsetEdit.setText(currentTempOffset);

        binding.calibrateButton.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Saves the entered offsets to SharedPreferences and returns to the dashboard.
     *
     * @param v The button view that was clicked.
     */
    @Override
    public void onClick(View v) {
        String windOffset = binding.windOffsetEdit.getText().toString();
        String tempOffset = binding.tempOffsetEdit.getText().toString();

        weatherViewModel.updateCalibration(windOffset, tempOffset);

        // Navigate back to the dashboard to see the effects
        Navigation.findNavController(v).navigate(R.id.dashboardFragment);
    }
}
