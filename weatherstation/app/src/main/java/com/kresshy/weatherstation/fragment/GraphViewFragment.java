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
 * A fragment that provides a detailed graph-only view of weather trends. Unlike the Dashboard, it
 * focuses primarily on long-term data visualization using linear chart sets.
 */
@AndroidEntryPoint
public class GraphViewFragment extends Fragment {

    private int windowIntervalSeconds = 300;
    @javax.inject.Inject SharedPreferences sharedPreferences;

    private WeatherViewModel weatherViewModel;
    private FragmentDashboardBinding binding;

    private LineDataSet windSpeedSet;
    private LineDataSet temperatureSet;
    private Long firstTimestamp = null;
    private Double firstWindValue = null;
    private Double firstTempValue = null;

    private final SimpleDateFormat xAxisFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    /** Required empty public constructor for fragment instantiation. */
    public GraphViewFragment() {
        // Required empty public constructor
    }

    /**
     * Called when the fragment is first created. Initializes the WeatherViewModel.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     *     this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
    }

    /**
     * Called to have the fragment instantiate its user interface view.
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
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after {@link #onCreateView} has returned. Sets up charts, populates data
     * from history, and begins observing UI state updates.
     *
     * @param view The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *     saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        windowIntervalSeconds =
                Integer.parseInt(
                        sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setupCharts();
        populateChartsFromHistory();

        weatherViewModel
                .getWeatherUiState()
                .observe(
                        getViewLifecycleOwner(),
                        state -> {
                            if (state == null) return;

                            if (getActivity() instanceof WSActivity) {
                                ((WSActivity) getActivity())
                                        .setToolbarTitle(state.getConnectedDeviceName());
                            }

                            if (state.getLatestData() != null) {
                                updateUI(state);
                            }
                        });
    }

    private void setupCharts() {
        windSpeedSet = createSet("Wind Speed", Color.BLUE);
        temperatureSet = createSet("Temperature", Color.RED);

        configureChart(binding.windSpeedChart, "Wind Speed (m/s)", windSpeedSet);
        configureChart(binding.temperatureChart, "Temperature (°C)", temperatureSet);
    }

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
            binding.windSpeedChart.getXAxis().setAxisMinimum(lastX - windowIntervalSeconds);

            binding.temperatureChart.getXAxis().setAxisMaximum(lastX);
            binding.temperatureChart.getXAxis().setAxisMinimum(lastX - windowIntervalSeconds);

            // Initialize overlay text from history
            WeatherData latest = history.get(history.size() - 1);
            binding.currentWindText.setText(
                    String.format(
                            java.util.Locale.getDefault(), "%.2f m/s", latest.getWindSpeed()));
            binding.currentTempText.setText(
                    String.format(
                            java.util.Locale.getDefault(), "%.2f°C", latest.getTemperature()));

            // Initialize trends from history/viewmodel
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

    private void configureChart(LineChart chart, String description, LineDataSet set) {
        chart.getDescription().setText(description);
        chart.getDescription().setTextColor(Color.WHITE);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        // Dark background to make fills pop
        chart.setBackgroundColor(Color.parseColor("#121212"));
        chart.setDrawGridBackground(false);

        // Styling for the legend
        chart.getLegend().setTextColor(Color.WHITE);
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.LINE);

        LineData data = new LineData(set);
        data.setValueTextColor(Color.WHITE);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.LTGRAY);
        xAxis.setDrawGridLines(false);
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

        chart.getAxisLeft().setTextColor(Color.LTGRAY);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(Color.parseColor("#22FFFFFF"));
        chart.getAxisRight().setEnabled(false);
    }

    private void updateUI(WeatherUiState state) {
        WeatherData data = state.getLatestData();
        binding.currentWindText.setText(
                String.format(java.util.Locale.getDefault(), "%.2f m/s", data.getWindSpeed()));
        binding.currentTempText.setText(
                String.format(java.util.Locale.getDefault(), "%.2f°C", data.getTemperature()));

        // Use trends directly from the snapshot
        binding.windTrendText.setText(getString(R.string.wind_trend_format, state.getWindTrend()));
        binding.tempTrendText.setText(getString(R.string.temp_trend_format, state.getTempTrend()));

        // Initialize session anchors if this is the first data point
        if (firstTimestamp == null) {
            firstTimestamp = data.getTimestamp().getTime();
            firstWindValue = data.getWindSpeed();
            firstTempValue = data.getTemperature();

            // Anchor the start of the session to the bottom corner for both charts
            binding.windSpeedChart.getAxisLeft().setAxisMinimum(firstWindValue.floatValue());
            binding.temperatureChart.getAxisLeft().setAxisMinimum(firstTempValue.floatValue());
        }

        addValueToSet(
                binding.windSpeedChart,
                windSpeedSet,
                (float) data.getWindSpeed(),
                data.getTimestamp().getTime());
        addValueToSet(
                binding.temperatureChart,
                temperatureSet,
                (float) data.getTemperature(),
                data.getTimestamp().getTime());
    }

    private void addValueToSet(LineChart chart, LineDataSet set, float value, long timestamp) {
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

        // Right-to-Left Filling: Ensure the latest data point is always at the right edge.
        // The window is always 'windowIntervalSeconds' wide, starting from the latest point.
        chart.getXAxis().setAxisMaximum(nextX);
        chart.getXAxis().setAxisMinimum(nextX - windowIntervalSeconds);

        // Let the Y-axis auto-scale if data goes below the initial anchor point
        if (value < chart.getAxisLeft().getAxisMinimum()) {
            chart.getAxisLeft().resetAxisMinimum();
        }

        chart.invalidate();
    }

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(new ArrayList<>(), label);
        set.setColor(color);
        set.setLineWidth(5f);
        set.setCircleColor(color);
        set.setCircleRadius(3.5f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);

        // Performance: Use LINEAR mode
        set.setMode(LineDataSet.Mode.LINEAR);

        // Solid fill
        set.setDrawFilled(true);
        set.setFillAlpha(110);
        set.setFillColor(color);
        set.setHighLightColor(Color.WHITE);
        set.setDrawHorizontalHighlightIndicator(false);

        return set;
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed. Cleans up the
     * binding to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Called when the fragment is no longer attached to its activity. Ensures the screen-on flag is
     * cleared to conserve battery.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
