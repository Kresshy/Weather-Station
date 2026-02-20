package com.kresshy.weatherstation.fragment;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.activity.WSActivity;
import com.kresshy.weatherstation.databinding.FragmentDashboardBinding;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that displays real-time weather data in charts and indicates thermal suitability.
 * Features live-scrolling charts for wind speed and temperature, plus a 0-100 score indicator.
 */
@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private int numSamples = 300;
    @javax.inject.Inject SharedPreferences sharedPreferences;
    private WeatherData previousData;
    private WeatherViewModel weatherViewModel;
    private FragmentDashboardBinding binding;

    private LineDataSet windSpeedSet;
    private LineDataSet temperatureSet;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public DashboardFragment() {
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
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Keep the screen on while the dashboard is visible
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        numSamples =
                Integer.parseInt(
                        sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        setupCharts();
        populateChartsFromHistory();

        // Data observation and UI updates
        weatherViewModel
                .getLatestWeatherData()
                .observe(
                        getViewLifecycleOwner(),
                        weatherData -> {
                            if (weatherData != null) {
                                // Simple outlier filter
                                if (previousData != null
                                        && Math.abs(
                                                        previousData.getTemperature()
                                                                - weatherData.getTemperature())
                                                > 10) {
                                    return;
                                }
                                previousData = weatherData;
                                addEntry(weatherData);
                            }
                        });

        weatherViewModel
                .getLaunchDecision()
                .observe(
                        getViewLifecycleOwner(),
                        decision -> {
                            binding.launchStatusCard.setCardBackgroundColor(
                                    getDecisionColor(decision));
                            binding.launchStatusText.setText(getDecisionText(decision));
                        });

        weatherViewModel
                .getTempTrend()
                .observe(
                        getViewLifecycleOwner(),
                        trend -> {
                            binding.tempTrendText.setText(
                                    getString(R.string.temp_trend_format, trend));
                        });

        weatherViewModel
                .getWindTrend()
                .observe(
                        getViewLifecycleOwner(),
                        trend -> {
                            binding.windTrendText.setText(
                                    getString(R.string.wind_trend_format, trend));
                        });

        weatherViewModel
                .getThermalScore()
                .observe(
                        getViewLifecycleOwner(),
                        score -> {
                            binding.thermalScoreProgress.setProgress(score);
                        });

        weatherViewModel
                .isLaunchDetectorEnabled()
                .observe(
                        getViewLifecycleOwner(),
                        enabled -> {
                            binding.launchStatusCard.setVisibility(enabled ? View.VISIBLE : View.GONE);
                        });

        weatherViewModel
                .getConnectedDeviceName()
                .observe(
                        getViewLifecycleOwner(),
                        name -> {
                            if (getActivity() instanceof WSActivity) {
                                ((WSActivity) getActivity()).setToolbarTitle(name);
                            }
                        });
    }

    private int getDecisionColor(WeatherRepository.LaunchDecision decision) {
        switch (decision) {
            case LAUNCH:
                return Color.parseColor("#4CAF50"); // Material Green
            case POTENTIAL:
                return Color.parseColor("#FFC107"); // Material Amber
            case POOR:
                return Color.parseColor("#F44336"); // Material Red
            default:
                return Color.GRAY;
        }
    }

    private String getDecisionText(WeatherRepository.LaunchDecision decision) {
        switch (decision) {
            case LAUNCH:
                return getString(R.string.launch_status_launch);
            case POTENTIAL:
                return getString(R.string.launch_status_potential);
            case POOR:
                return getString(R.string.launch_status_poor);
            default:
                return getString(R.string.launch_status_analyzing);
        }
    }

    /** Initializes the charts with empty data sets and styling. */
    private void setupCharts() {
        windSpeedSet = createSet("Wind Speed", Color.BLUE);
        temperatureSet = createSet("Temperature", Color.RED);

        configureChart(binding.windSpeedChart, windSpeedSet, "Wind Speed (m/s)");
        configureChart(binding.temperatureChart, temperatureSet, "Temperature (°C)");
    }

    /** Loads stored historical data into the charts for persistence. */
    private void populateChartsFromHistory() {
        List<WeatherData> history = weatherViewModel.getHistoricalWeatherData();
        if (history != null && !history.isEmpty()) {
            for (WeatherData data : history) {
                windSpeedSet.addEntry(new Entry(windSpeedSet.getEntryCount(), (float) data.getWindSpeed()));
                temperatureSet.addEntry(new Entry(temperatureSet.getEntryCount(), (float) data.getTemperature()));
            }
            binding.windSpeedChart.getData().notifyDataChanged();
            binding.windSpeedChart.notifyDataSetChanged();
            binding.windSpeedChart.invalidate();

            binding.temperatureChart.getData().notifyDataChanged();
            binding.temperatureChart.notifyDataSetChanged();
            binding.temperatureChart.invalidate();
            
            // Set current values to latest in history
            WeatherData latest = history.get(history.size() - 1);
            binding.currentWindText.setText(
                    String.format(Locale.getDefault(), "%.1f m/s", latest.getWindSpeed()));
            binding.currentTempText.setText(
                    String.format(Locale.getDefault(), "%.1f°C", latest.getTemperature()));
        }
    }

    /** Configures general styling and axes for a LineChart. */
    private void configureChart(LineChart chart, LineDataSet set, String description) {
        chart.getDescription().setText(description);
        chart.getDescription().setTextColor(Color.WHITE);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.TRANSPARENT);

        LineData data = new LineData(set);
        data.setValueTextColor(Color.WHITE);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.GRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(numSamples);

        chart.getAxisLeft().setTextColor(Color.GRAY);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(Color.GRAY);
    }

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(new ArrayList<>(), label);
        set.setColor(color);
        set.setLineWidth(5f);
        set.setCircleColor(color);
        set.setCircleRadius(3.5f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return set;
    }

    /** Updates text views and appends data points to the charts. */
    private void addEntry(WeatherData data) {
        binding.lastUpdatedText.setText(
                getString(R.string.last_updated_format, timeFormat.format(data.getTimestamp())));
        if (data.getRssi() != 0) {
            String label = getSignalStrengthLabel(data.getRssi());
            binding.rssiText.setText(getString(R.string.rssi_format_with_label, data.getRssi(), label));
        } else {
            binding.rssiText.setText(getString(R.string.rssi_connected_only));
        }

        binding.currentWindText.setText(
                String.format(Locale.getDefault(), "%.1f m/s", data.getWindSpeed()));
        binding.currentTempText.setText(
                String.format(Locale.getDefault(), "%.1f°C", data.getTemperature()));

        addEntryToChart(binding.windSpeedChart, windSpeedSet, (float) data.getWindSpeed());
        addEntryToChart(binding.temperatureChart, temperatureSet, (float) data.getTemperature());
    }

    private String getSignalStrengthLabel(int rssi) {
        if (rssi >= -60) return "Excellent";
        if (rssi >= -70) return "Good";
        if (rssi >= -80) return "Fair";
        if (rssi >= -90) return "Poor";
        return "Very Weak";
    }

    /** Appends a new value to a chart and shifts the window if capacity is reached. */
    private void addEntryToChart(LineChart chart, LineDataSet set, float value) {
        // Shift existing entries to the left
        List<Entry> entries = set.getValues();
        if (entries.size() >= numSamples) {
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                e.setX(e.getX() - 1);
            }
            // Remove the point that shifted out of range
            set.removeEntry(0);
        }

        // Always add the new point at the right-most index
        set.addEntry(new Entry(set.getEntryCount(), value));

        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
