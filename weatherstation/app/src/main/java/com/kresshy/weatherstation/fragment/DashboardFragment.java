package com.kresshy.weatherstation.fragment;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.activity.WSActivity;
import com.kresshy.weatherstation.databinding.FragmentDashboardBinding;
import com.kresshy.weatherstation.repository.WeatherRepository;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherUiState;
import com.kresshy.weatherstation.weather.WeatherViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that displays real-time weather data in charts and indicates thermal suitability. This
 * component provides a visual dashboard for monitoring wind speed, temperature, and atmospheric
 * trends, facilitating launch decisions for weather-sensitive activities.
 */
@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private int windowIntervalSeconds = 300;
    @javax.inject.Inject SharedPreferences sharedPreferences;
    private WeatherData previousData;
    private WeatherViewModel weatherViewModel;
    private FragmentDashboardBinding binding;
    private com.google.android.material.snackbar.Snackbar loadingSnackbar;

    private LineDataSet windSpeedSet;
    private LineDataSet temperatureSet;
    private Long firstTimestamp = null;
    private Double firstWindValue = null;
    private Double firstTempValue = null;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat xAxisFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    /** Required empty public constructor for fragment instantiation by the Android framework. */
    public DashboardFragment() {
        // Required empty public constructor
    }

    /**
     * Initializes the fragment's internal state. This is where the shared {@link WeatherViewModel}
     * is retrieved to access the centralized weather data and UI state.
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
    }

    /**
     * Inflates the fragment's layout and initializes the data binding. This creates the visual
     * structure of the real-time weather dashboard.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views.
     * @param container If non-null, this is the parent view that the fragment's UI should be
     *     attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state.
     * @return The root view of the fragment's layout.
     */
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Configures the charts, initializes history, and observes the unified UI state. This ensures
     * that the dashboard is correctly set up and remains synchronized with the latest sensor data
     * and connection status.
     *
     * @param view The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Keep the screen on while the dashboard is visible
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        windowIntervalSeconds =
                Integer.parseInt(
                        sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        setupCharts();
        populateChartsFromHistory();

        // Single source of truth for the UI
        weatherViewModel
                .getWeatherUiState()
                .observe(
                        getViewLifecycleOwner(),
                        state -> {
                            if (state == null) return;

                            // 1. Update sensor readings and charts
                            if (state.getLatestData() != null) {
                                updateSensorUI(state);
                            }

                            // 2. Update launch detector status
                            binding.launchStatusCard.setVisibility(
                                    state.isLaunchDetectorEnabled() ? View.VISIBLE : View.GONE);
                            binding.launchStatusCard.setCardBackgroundColor(
                                    getDecisionColor(state.getLaunchDecision()));
                            binding.launchStatusText.setText(
                                    getDecisionText(state.getLaunchDecision()));
                            binding.thermalScoreProgress.setProgress(state.getThermalScore());

                            // 3. Update device name in toolbar
                            if (getActivity() instanceof WSActivity) {
                                ((WSActivity) getActivity())
                                        .setToolbarTitle(state.getConnectedDeviceName());
                            }
                        });

        // Still observe UI status (LOADING/ERROR) separately as it drives Snackbars
        weatherViewModel
                .getUiState()
                .observe(
                        getViewLifecycleOwner(),
                        resource -> {
                            if (resource == null) return;
                            switch (resource.status) {
                                case LOADING:
                                    if (loadingSnackbar == null) {
                                        loadingSnackbar =
                                                com.google.android.material.snackbar.Snackbar.make(
                                                        binding.getRoot(),
                                                        R.string.connecting_message,
                                                        com.google.android.material.snackbar
                                                                .Snackbar.LENGTH_INDEFINITE);
                                    }
                                    loadingSnackbar.show();
                                    break;
                                case SUCCESS:
                                    if (loadingSnackbar != null) loadingSnackbar.dismiss();
                                    if (androidx.navigation.fragment.NavHostFragment
                                                            .findNavController(this)
                                                            .getCurrentDestination()
                                                    != null
                                            && androidx.navigation.fragment.NavHostFragment
                                                            .findNavController(this)
                                                            .getCurrentDestination()
                                                            .getId()
                                                    == R.id.bluetoothDeviceListFragment) {
                                        androidx.navigation.fragment.NavHostFragment
                                                .findNavController(this)
                                                .navigate(R.id.dashboardFragment);
                                    }
                                    break;
                                case ERROR:
                                    if (loadingSnackbar != null) loadingSnackbar.dismiss();
                                    com.google.android.material.snackbar.Snackbar.make(
                                                    binding.getRoot(),
                                                    getString(
                                                            R.string.error_prefix,
                                                            resource.message),
                                                    com.google.android.material.snackbar.Snackbar
                                                            .LENGTH_LONG)
                                            .setAction(
                                                    R.string.retry_action,
                                                    v -> weatherViewModel.startDiscovery()) // Use
                                            // appropriate retry
                                            .show();
                                    break;
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
            windSpeedSet.clear();
            temperatureSet.clear();

            WeatherData first = history.get(0);
            firstTimestamp = first.getTimestamp().getTime();
            firstWindValue = first.getWindSpeed();
            firstTempValue = first.getTemperature();

            // Anchor the start of the session to the bottom corner
            binding.windSpeedChart.getAxisLeft().setAxisMinimum(firstWindValue.floatValue());
            binding.temperatureChart.getAxisLeft().setAxisMinimum(firstTempValue.floatValue());

            for (WeatherData data : history) {
                float offsetSeconds = (data.getTimestamp().getTime() - firstTimestamp) / 1000f;
                windSpeedSet.addEntry(new Entry(offsetSeconds, (float) data.getWindSpeed()));
                temperatureSet.addEntry(new Entry(offsetSeconds, (float) data.getTemperature()));
            }
            binding.windSpeedChart.getData().notifyDataChanged();
            binding.windSpeedChart.notifyDataSetChanged();
            binding.windSpeedChart.invalidate();

            binding.temperatureChart.getData().notifyDataChanged();
            binding.temperatureChart.notifyDataSetChanged();
            binding.temperatureChart.invalidate();

            // Correctly set initial visible window
            float lastX =
                    (history.get(history.size() - 1).getTimestamp().getTime() - firstTimestamp)
                            / 1000f;

            binding.windSpeedChart.getXAxis().setAxisMaximum(lastX);
            binding.windSpeedChart
                    .getXAxis()
                    .setAxisMinimum(Math.max(0, lastX - windowIntervalSeconds));

            binding.temperatureChart.getXAxis().setAxisMaximum(lastX);
            binding.temperatureChart
                    .getXAxis()
                    .setAxisMinimum(Math.max(0, lastX - windowIntervalSeconds));

            // Set current values to latest in history
            WeatherData latest = history.get(history.size() - 1);
            binding.currentWindText.setText(
                    String.format(Locale.getDefault(), "%.2f m/s", latest.getWindSpeed()));
            binding.currentTempText.setText(
                    String.format(Locale.getDefault(), "%.2f°C", latest.getTemperature()));

            binding.windTrendText.setText(
                    getString(
                            R.string.wind_trend_format,
                            weatherViewModel.getWindTrend().getValue() != null
                                    ? weatherViewModel.getWindTrend().getValue()
                                    : 0.0));
            binding.tempTrendText.setText(
                    getString(
                            R.string.temp_trend_format,
                            weatherViewModel.getTempTrend().getValue() != null
                                    ? weatherViewModel.getTempTrend().getValue()
                                    : 0.0));
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
        xAxis.setAxisMaximum(windowIntervalSeconds);

        // Show real-time clock labels (HH:mm:ss)
        xAxis.setValueFormatter(
                new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (firstTimestamp == null) return "";
                        long absoluteTime = firstTimestamp + (long) (value * 1000);
                        return xAxisFormat.format(new Date(absoluteTime));
                    }
                });

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

        set.setMode(LineDataSet.Mode.LINEAR);
        return set;
    }

    /** Updates text views and appends data points to the charts. */
    private void updateSensorUI(WeatherUiState state) {
        WeatherData data = state.getLatestData();
        binding.lastUpdatedText.setText(
                getString(R.string.last_updated_format, timeFormat.format(data.getTimestamp())));
        if (data.getRssi() != 0) {
            String label = getSignalStrengthLabel(data.getRssi());
            binding.rssiText.setText(
                    getString(R.string.rssi_format_with_label, data.getRssi(), label));
        } else {
            binding.rssiText.setText(getString(R.string.rssi_connected_only));
        }

        binding.currentWindText.setText(
                String.format(Locale.getDefault(), "%.2f m/s", data.getWindSpeed()));
        binding.currentTempText.setText(
                String.format(Locale.getDefault(), "%.2f°C", data.getTemperature()));

        // Sync trends from the same state snapshot
        binding.tempTrendText.setText(getString(R.string.temp_trend_format, state.getTempTrend()));
        binding.windTrendText.setText(getString(R.string.wind_trend_format, state.getWindTrend()));

        // Initialize session anchors if this is the first data point
        if (firstTimestamp == null) {
            firstTimestamp = data.getTimestamp().getTime();
            firstWindValue = data.getWindSpeed();
            firstTempValue = data.getTemperature();

            // Anchor the start of the session to the bottom corner for both charts
            binding.windSpeedChart.getAxisLeft().setAxisMinimum(firstWindValue.floatValue());
            binding.temperatureChart.getAxisLeft().setAxisMinimum(firstTempValue.floatValue());
        }

        addEntryToChart(
                binding.windSpeedChart,
                windSpeedSet,
                (float) data.getWindSpeed(),
                data.getTimestamp().getTime());
        addEntryToChart(
                binding.temperatureChart,
                temperatureSet,
                (float) data.getTemperature(),
                data.getTimestamp().getTime());
    }

    private String getSignalStrengthLabel(int rssi) {
        if (rssi >= -60) return "Excellent";
        if (rssi >= -70) return "Good";
        if (rssi >= -80) return "Fair";
        if (rssi >= -90) return "Poor";
        return "Very Weak";
    }

    /** Appends a new value to a chart and shifts the window if capacity is reached. */
    private void addEntryToChart(LineChart chart, LineDataSet set, float value, long timestamp) {
        // Calculation of nextX remains relative to the captured firstTimestamp
        float nextX = (timestamp - firstTimestamp) / 1000f;
        set.addEntry(new Entry(nextX, value));

        // Synchronize with repository's pruning
        List<WeatherData> history = weatherViewModel.getHistoricalWeatherData();
        if (history != null && !history.isEmpty()) {
            // Remove entries from the UI set that are no longer in the repository history
            while (set.getEntryCount() > 0
                    && set.getEntryForIndex(0).getX()
                            < (history.get(0).getTimestamp().getTime() - firstTimestamp) / 1000f) {
                set.removeEntry(0);
            }
        }

        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();

        // Right-to-Left Sliding: Ensure the latest data point is always at the right edge
        // once the interval is reached. Maintain constant scale (width) from the start.
        chart.getXAxis().setAxisMaximum(Math.max(windowIntervalSeconds, nextX));
        chart.getXAxis().setAxisMinimum(Math.max(0, nextX - windowIntervalSeconds));

        // Let the Y-axis auto-scale if data goes below the initial anchor point
        if (value < chart.getAxisLeft().getAxisMinimum()) {
            chart.getAxisLeft().resetAxisMinimum();
        }

        // Avoid excessive redraws by only invalidating if it's on screen.
        chart.invalidate();
    }

    /**
     * Cleans up the fragment's resources. This is essential to prevent memory leaks by nulling out
     * the view binding.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Called when the fragment is being detached from its activity. This is used to clear flags
     * like keeping the screen on, ensuring the device can enter sleep mode when the dashboard is no
     * longer visible.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
