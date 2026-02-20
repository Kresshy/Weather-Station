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
import com.kresshy.weatherstation.databinding.FragmentDashboardBinding;
import com.kresshy.weatherstation.weather.WeatherData;
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

    private WeatherViewModel weatherViewModel;
    private FragmentDashboardBinding binding;

    private LineDataSet windSpeedSet;
    private LineDataSet temperatureSet;

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

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireContext());
        numberOfSamples =
                Integer.parseInt(
                        sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setupCharts();

        weatherViewModel
                .getLatestWeatherData()
                .observe(
                        getViewLifecycleOwner(),
                        weatherData -> {
                            if (weatherData != null) {
                                addEntry(weatherData);
                            }
                        });
    }

    private void setupCharts() {
        windSpeedSet = createSet("Wind Speed", Color.BLUE);
        temperatureSet = createSet("Temperature", Color.RED);

        configureChart(binding.windSpeedChart, "Wind Speed (m/s)", windSpeedSet);
        configureChart(binding.temperatureChart, "Temperature (°C)", temperatureSet);
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

    private void addEntry(WeatherData data) {
        addValueToSet(binding.windSpeedChart, windSpeedSet, (float) data.getWindSpeed());
        addValueToSet(binding.temperatureChart, temperatureSet, (float) data.getTemperature());
    }

    private void addValueToSet(LineChart chart, LineDataSet set, float value) {
        List<Entry> entries = set.getValues();
        if (entries.size() >= numberOfSamples) {
            set.removeEntry(0);
            for (Entry e : entries) {
                e.setX(e.getX() - 1);
            }
        }

        set.addEntry(new Entry(set.getEntryCount(), value));

        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
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

        // Pro: Cubic smoothing
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.15f);

        // Pro: High-contrast solid fill
        set.setDrawFilled(true);
        set.setFillAlpha(110); // Higher alpha (~43% opacity)
        set.setFillColor(color);

        // Smoothing out the edges
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
