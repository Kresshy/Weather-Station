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
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.interfaces.WeatherListener;
import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;

import java.util.ArrayList;
import java.util.List;


public class GraphViewFragment extends Fragment implements WeatherListener {

    private static final String TAG = "GraphViewFragment";

    private LineGraphView windSpeedGraph;
    private LineGraphView temperatureGraph;

    private List<GraphViewData[]> windSpeedDataList;
    private List<GraphViewData[]> temperatureDataList;

    private GraphViewData[] windSpeedData;
    private GraphViewData[] temperatureData;

    private List<GraphViewSeries> windSpeedSeriesList;
    private List<GraphViewSeries> temperatureSeriesList;

    private GraphViewSeries windSpeedSeries;
    private GraphViewSeries temperatureSeries;

    private GraphViewStyle graphViewStyle;

    private int measurementCount = 1;

    private OnFragmentInteractionListener mListener;

    private int NUM_SAMPLES = 300;
    private SharedPreferences sharedPreferences;

    private WeatherData previousData;
    private Measurement previousMeasurement;

    private int slidingScreen = 5;
    private List<Double> slidingScreenItems = new ArrayList<Double>(slidingScreen);

    public GraphViewFragment() {
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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        NUM_SAMPLES = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        String[] horizontalLabels;
        switch (NUM_SAMPLES) {
            case 60:
                horizontalLabels = new String[]{"1min", "45sec", "30sec", "15sec", "0min"};
                Log.i(TAG, "Number of samples: 60");
                break;
            case 120:
                horizontalLabels = new String[]{"2min", "1min", "0min"};
                Log.i(TAG, "Number of samples: 120");
                break;
            case 300:
                horizontalLabels = new String[]{"5min", "4min", "3min", "2min", "1min", "0min"};
                Log.i(TAG, "Number of samples: 300");
                break;
            case 600:
                horizontalLabels = new String[]{"10min", "8min", "6min", "4min", "2min", "0min"};
                Log.i(TAG, "Number of samples: 600");
                break;
            case 1200:
                horizontalLabels = new String[]{"20min", "15min", "10min", "5min", "0min"};
                Log.i(TAG, "Number of samples: 1200");
                break;
            default:
                horizontalLabels = new String[]{"5min", "4min", "3min", "2min", "1min", "0min"};
                Log.i(TAG, "Number of samples: 300");
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

        windSpeedSeries = new GraphViewSeries("Wind Speed", new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, 3), windSpeedData);
        temperatureSeries = new GraphViewSeries("Temperature", new GraphViewSeries.GraphViewSeriesStyle(Color.RED, 3), temperatureData);

        windSpeedGraph.addSeries(windSpeedSeries);
        temperatureGraph.addSeries(temperatureSeries);

        windSpeedContainer.addView(windSpeedGraph);
        temperatureContainer.addView(temperatureGraph);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void weatherDataReceived(WeatherData weatherData) {
        Log.i(TAG, "measurementCount: " + measurementCount);
        if (measurementCount == 1) {
            previousData = weatherData;

            windSpeedData = new GraphViewData[1];
            temperatureData = new GraphViewData[1];

            windSpeedData[0] = new GraphViewData(0, weatherData.getWindSpeed());
            temperatureData[0] = new GraphViewData(0, weatherData.getTemperature());

            windSpeedSeries.resetData(windSpeedData);
            temperatureSeries.resetData(temperatureData);

            slidingScreenItems.add(weatherData.getWindSpeed());

            measurementCount++;
        } else {
            // prevent adding false measurements
            if (weatherData.getTemperature() == 0.0) {
                weatherData.setTemperature(previousData.getTemperature());
            } else {
                previousData = weatherData;
            }

            // the windspeed is an avarage of 3 measurements
            if (slidingScreenItems.size() == slidingScreen + 1) {
                double measurement = slidingScreenItems.remove(0);
                Log.i(TAG, slidingScreenItems.toString());
            }

            slidingScreenItems.add(weatherData.getWindSpeed());

            double sum = 0;

            for (double item : slidingScreenItems) {
                sum += item;
            }

            double avarageWindSpeed = sum / slidingScreenItems.size();
            Log.i(TAG, "windspeed: " + avarageWindSpeed);

            windSpeedSeries.appendData(new GraphViewData(measurementCount, avarageWindSpeed), true, NUM_SAMPLES);
            temperatureSeries.appendData(new GraphViewData(measurementCount, weatherData.getTemperature()), true, NUM_SAMPLES);
            measurementCount++;
        }
    }

    @Override
    public void measurementReceived(Measurement measurement) {
        
    }

    public interface OnFragmentInteractionListener {

        public void registerWeatherDataReceiver(WeatherListener weatherListener);
    }

}
