package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherListener;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public class DashboardFragment extends androidx.fragment.app.Fragment implements WeatherListener {

    private static final String TAG = "DashboardFragment";

    private LineGraphView windSpeedGraph;
    private LineGraphView temperatureGraph;

    private GraphViewData[] windSpeedData;
    private GraphViewData[] temperatureData;

    private GraphViewSeries windSpeedSeries;
    private GraphViewSeries temperatureSeries;

    private GraphViewStyle graphViewStyle;

    private int weatherDataCount = 1;

    private OnFragmentInteractionListener mListener;

    private int NUM_SAMPLES = 300;
    private SharedPreferences sharedPreferences;

    private WeatherData previousData;

    private int screenSize = 5;
    private List<WeatherData> slidingScreen = new ArrayList<>(screenSize);

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            mListener.registerWeatherDataReceiver(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        NUM_SAMPLES = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        String[] horizontalLabels;
        switch (NUM_SAMPLES) {
            case 60:
                horizontalLabels = new String[]{"1min", "45sec", "30sec", "15sec", "0min"};
                Timber.d( "Number of samples: 60");
                break;
            case 120:
                horizontalLabels = new String[]{"2min", "1min", "0min"};
                Timber.d( "Number of samples: 120");
                break;
            case 300:
                horizontalLabels = new String[]{"5min", "4min", "3min", "2min", "1min", "0min"};
                Timber.d( "Number of samples: 300");
                break;
            case 600:
                horizontalLabels = new String[]{"10min", "8min", "6min", "4min", "2min", "0min"};
                Timber.d( "Number of samples: 600");
                break;
            case 1200:
                horizontalLabels = new String[]{"20min", "15min", "10min", "5min", "0min"};
                Timber.d( "Number of samples: 1200");
                break;
            default:
                horizontalLabels = new String[]{"5min", "4min", "3min", "2min", "1min", "0min"};
                Timber.d( "Number of samples: 300");
        }

        LinearLayout windSpeedContainer = (LinearLayout) view.findViewById(R.id.windSpeedContainer);
        LinearLayout temperatureContainer = (LinearLayout) view.findViewById(R.id.temperatureContainer);

        graphViewStyle = new GraphViewStyle(Color.BLACK, Color.BLACK, Color.GRAY);
        graphViewStyle.setVerticalLabelsAlign(Paint.Align.LEFT);
        graphViewStyle.setVerticalLabelsWidth(80);

        windSpeedGraph = new LineGraphView(getActivity().getApplicationContext(), "Wind Speed");
        windSpeedGraph.setScrollable(true);
        // windSpeedGraph.setScalable(true);
        windSpeedGraph.setViewPort(0, NUM_SAMPLES);
        windSpeedGraph.setGraphViewStyle(graphViewStyle);

        windSpeedGraph.setHorizontalLabels(horizontalLabels);

        temperatureGraph = new LineGraphView(getActivity().getApplicationContext(), "Temperature");
        temperatureGraph.setScrollable(true);
        // temperatureGraph.setScalable(true);
        temperatureGraph.setViewPort(0, NUM_SAMPLES);
        temperatureGraph.setGraphViewStyle(graphViewStyle);

        temperatureGraph.setHorizontalLabels(horizontalLabels);
        temperatureGraph.setShowHorizontalLabels(false);

        windSpeedData = new GraphViewData[1];
        temperatureData = new GraphViewData[1];

        windSpeedData[0] = new GraphViewData(0, 0);
        temperatureData[0] = new GraphViewData(0, 0);

        windSpeedSeries = new GraphViewSeries("Wind Speed", new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, 7), windSpeedData);
        temperatureSeries = new GraphViewSeries("Temperature", new GraphViewSeries.GraphViewSeriesStyle(Color.RED, 7), temperatureData);

        windSpeedGraph.addSeries(windSpeedSeries);
        temperatureGraph.addSeries(temperatureSeries);

        windSpeedContainer.addView(windSpeedGraph);
        temperatureContainer.addView(temperatureGraph);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mListener = null;
    }

    @Override
    public void weatherDataReceived(WeatherData weatherData) {
        Timber.d( "weatherDataCount: " + weatherDataCount);
        if (weatherDataCount == 1) {
            previousData = weatherData;

            windSpeedData = new GraphViewData[1];
            temperatureData = new GraphViewData[1];

            windSpeedData[0] = new GraphViewData(0, weatherData.windSpeed());
            temperatureData[0] = new GraphViewData(0, weatherData.temperature());

            windSpeedSeries.resetData(windSpeedData);
            temperatureSeries.resetData(temperatureData);

            slidingScreen.add(weatherData);

            weatherDataCount++;
        } else {
            // prevent adding false measurements
            if (weatherData.temperature() == 0.0 || (weatherData.temperature() - previousData.temperature() > 1.0)) {
                weatherData.toBuilder().setTemperature(previousData.temperature()).build();
            } else {
                previousData = weatherData;
            }

            // the windspeed is an avarage of 5 measurements
            if (slidingScreen.size() == screenSize + 1) {
                slidingScreen.remove(0);
            }

            slidingScreen.add(weatherData);

            double sumWindSpeed = 0;
            double sumTemperature = 0;

            for (WeatherData wData : slidingScreen) {
                sumWindSpeed += wData.windSpeed();
                sumTemperature += wData.temperature();
            }

            double avarageWindSpeed = sumWindSpeed / slidingScreen.size();
            double avarageTemperature = sumTemperature / slidingScreen.size();
            Timber.d( "windspeed: " + avarageWindSpeed);
            Timber.d( "temperature: " + avarageTemperature);

            windSpeedSeries.appendData(new GraphViewData(weatherDataCount, avarageWindSpeed), true, NUM_SAMPLES);
            temperatureSeries.appendData(new GraphViewData(weatherDataCount, avarageTemperature), true, NUM_SAMPLES);
            weatherDataCount++;
        }
    }

    @Override
    public void measurementReceived(Measurement measurement) {

    }

    public interface OnFragmentInteractionListener {

        public void registerWeatherDataReceiver(WeatherListener weatherListener);
    }

}
