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
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.activity.WSActivity;
import com.kresshy.weatherstation.databinding.FragmentDashboardBinding;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherUiState;
import com.kresshy.weatherstation.weather.WeatherViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that provides a detailed graph-only view of weather trends. Unlike the Dashboard, it
 * focuses primarily on long-term data visualization using linear chart sets.
 */
@AndroidEntryPoint
public class GraphViewFragment extends Fragment {

    private int numberOfSamples = 300;
    @javax.inject.Inject SharedPreferences sharedPreferences;

    private WeatherViewModel weatherViewModel;
    private FragmentDashboardBinding binding;

    private LineDataSet windSpeedSet;
    private LineDataSet temperatureSet;
    private long totalPointsAddedWind = 0;
    private long totalPointsAddedTemp = 0;

    public GraphViewFragment() {
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

        numberOfSamples =
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
            totalPointsAddedWind = 0;
            totalPointsAddedTemp = 0;
            for (WeatherData data : history) {
                windSpeedSet.addEntry(
                        new Entry(totalPointsAddedWind++, (float) data.getWindSpeed()));
                temperatureSet.addEntry(
                        new Entry(totalPointsAddedTemp++, (float) data.getTemperature()));
            }
            binding.windSpeedChart.getData().notifyDataChanged();
            binding.windSpeedChart.notifyDataSetChanged();
            binding.windSpeedChart.invalidate();

            binding.temperatureChart.getData().notifyDataChanged();
            binding.temperatureChart.notifyDataSetChanged();
            binding.temperatureChart.invalidate();

            // Correctly set initial visible window
            binding.windSpeedChart.setVisibleXRangeMaximum(numberOfSamples);
            binding.windSpeedChart.moveViewToX(windSpeedSet.getEntryCount());
            binding.temperatureChart.setVisibleXRangeMaximum(numberOfSamples);
            binding.temperatureChart.moveViewToX(temperatureSet.getEntryCount());

            // Initialize overlay text from history
            WeatherData latest = history.get(history.size() - 1);
            binding.currentWindText.setText(
                    String.format(
                            java.util.Locale.getDefault(), "%.1f m/s", latest.getWindSpeed()));
            binding.currentTempText.setText(
                    String.format(
                            java.util.Locale.getDefault(), "%.1f°C", latest.getTemperature()));

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
        xAxis.setAxisMaximum(numberOfSamples);

        chart.getAxisLeft().setTextColor(Color.LTGRAY);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(Color.parseColor("#22FFFFFF"));
        chart.getAxisRight().setEnabled(false);
    }

    private void updateUI(WeatherUiState state) {
        WeatherData data = state.getLatestData();
        binding.currentWindText.setText(
                String.format(java.util.Locale.getDefault(), "%.1f m/s", data.getWindSpeed()));
        binding.currentTempText.setText(
                String.format(java.util.Locale.getDefault(), "%.1f°C", data.getTemperature()));

        // Use trends directly from the snapshot
        binding.windTrendText.setText(getString(R.string.wind_trend_format, state.getWindTrend()));
        binding.tempTrendText.setText(getString(R.string.temp_trend_format, state.getTempTrend()));

        addValueToSet(binding.windSpeedChart, windSpeedSet, (float) data.getWindSpeed());
        addValueToSet(binding.temperatureChart, temperatureSet, (float) data.getTemperature());
    }

    private void addValueToSet(LineChart chart, LineDataSet set, float value) {
        // Efficiency Optimization: Use increasing X coordinates and set the visible window.
        float nextX = (set == windSpeedSet) ? totalPointsAddedWind++ : totalPointsAddedTemp++;
        set.addEntry(new Entry(nextX, value));

        // Prune data if it becomes too large to keep memory usage low.
        if (set.getEntryCount() > numberOfSamples * 2) {
            set.removeEntry(0);
        }

        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();

        // Ensure the chart "scrolls" to the right and maintains fixed visible window.
        if (nextX >= numberOfSamples) {
            chart.getXAxis().resetAxisMaximum();
            chart.setVisibleXRangeMaximum(numberOfSamples);
            chart.moveViewToX(nextX);
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
