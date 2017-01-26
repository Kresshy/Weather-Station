package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherListener;

import java.util.ArrayList;
import java.util.List;


public class DashboardFragment extends Fragment implements WeatherListener {

    private static final String TAG = "DashboardFragment";

    private GraphView windSpeedGraph;
    private GraphView temperatureGraph;

    private DataPoint[] windSpeedData;
    private DataPoint[] temperatureData;

    private LineGraphSeries<DataPoint> windSpeedSeries;
    private LineGraphSeries<DataPoint> temperatureSeries;

    private int weatherDataCount = 1;

    private OnFragmentInteractionListener mListener;

    private int NUM_SAMPLES = 300;
    private SharedPreferences sharedPreferences;

    private WeatherData previousData;

    private int slidingScreen = 5;
    private List<Double> slidingScreenItems = new ArrayList<>(slidingScreen);
    private List<DataPoint> windSpeedDataPoints;
    private List<DataPoint> temperatureDataPoints;

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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        NUM_SAMPLES = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        windSpeedDataPoints = new ArrayList<>(NUM_SAMPLES);
        temperatureDataPoints = new ArrayList<>(NUM_SAMPLES);

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

        windSpeedGraph = (GraphView) view.findViewById(R.id.windSpeedGraph);
        temperatureGraph = (GraphView) view.findViewById(R.id.temperatureGraph);

        windSpeedGraph.setTitle("Wind Speed");
        temperatureGraph.setTitle("Temperature");

        windSpeedData = new DataPoint[1];
        temperatureData = new DataPoint[1];

        windSpeedData[0] = new DataPoint(0, 0);
        temperatureData[0] = new DataPoint(0, 0);

        windSpeedSeries = new LineGraphSeries<>(windSpeedData);
        temperatureSeries = new LineGraphSeries<>(temperatureData);

        windSpeedSeries.setColor(Color.BLUE);
        temperatureSeries.setColor(Color.RED);

        windSpeedSeries.setThickness(7);
        temperatureSeries.setThickness(7);

        GridLabelRenderer windGraphLabelRender = windSpeedGraph.getGridLabelRenderer();
        GridLabelRenderer temperatureGraphLabelRender = temperatureGraph.getGridLabelRenderer();

        windGraphLabelRender.setVerticalLabelsAlign(Paint.Align.LEFT);
        windGraphLabelRender.setVerticalLabelsVAlign(GridLabelRenderer.VerticalLabelsVAlign.MID);
        windGraphLabelRender.setNumHorizontalLabels(horizontalLabels.length);
        windGraphLabelRender.setLabelVerticalWidth(80);

        temperatureGraphLabelRender.setVerticalLabelsAlign(Paint.Align.LEFT);
        temperatureGraphLabelRender.setVerticalLabelsVAlign(GridLabelRenderer.VerticalLabelsVAlign.MID);
        temperatureGraphLabelRender.setLabelVerticalWidth(80);

        windSpeedGraph.getViewport().setScrollable(true);
        windSpeedGraph.getViewport().setXAxisBoundsManual(true);
        windSpeedGraph.getViewport().setYAxisBoundsManual(true);
        windSpeedGraph.getViewport().setMinX(0);
        windSpeedGraph.getViewport().setMaxX(NUM_SAMPLES);

        temperatureGraph.getViewport().setScrollable(true);
        temperatureGraph.getViewport().setXAxisBoundsManual(true);
        temperatureGraph.getViewport().setYAxisBoundsManual(true);
        temperatureGraph.getViewport().setMinX(0);
        temperatureGraph.getViewport().setMaxX(NUM_SAMPLES);

        StaticLabelsFormatter windGraphLabelFormatter = new StaticLabelsFormatter(windSpeedGraph);
        StaticLabelsFormatter temperatureGraphLabelFormatter = new StaticLabelsFormatter(temperatureGraph);

        windGraphLabelFormatter.setHorizontalLabels(horizontalLabels);
        temperatureGraphLabelFormatter.setHorizontalLabels(horizontalLabels);

        windGraphLabelRender.setLabelFormatter(windGraphLabelFormatter);
        windGraphLabelRender.setHorizontalLabelsVisible(false);

        temperatureGraphLabelRender.setLabelFormatter(temperatureGraphLabelFormatter);
        temperatureGraphLabelRender.setHorizontalLabelsVisible(false);

        GridLabelRenderer.Styles styles = windGraphLabelRender.getStyles();
        styles.highlightZeroLines = true;

        windSpeedGraph.addSeries(windSpeedSeries);
        temperatureGraph.addSeries(temperatureSeries);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void weatherDataReceived(WeatherData weatherData) {
        Log.i(TAG, "weatherDataCount: " + weatherDataCount);
        if (weatherDataCount == 1) {
            previousData = weatherData;

            windSpeedData = new DataPoint[1];
            temperatureData = new DataPoint[1];

            windSpeedData[0] = new DataPoint(0, weatherData.getWindSpeed());
            temperatureData[0] = new DataPoint(0, weatherData.getTemperature());

            windSpeedSeries.resetData(windSpeedData);
            temperatureSeries.resetData(temperatureData);

            slidingScreenItems.add(weatherData.getWindSpeed());

            windSpeedDataPoints.add(windSpeedData[0]);
            temperatureDataPoints.add(temperatureData[0]);

            weatherDataCount++;
        } else {
            // prevent adding false measurements
            if (weatherData.getTemperature() == 0.0) {
                weatherData.setTemperature(previousData.getTemperature());
            } else {
                previousData = weatherData;
            }

            // the windspeed is an avarage of 3 measurements
            if (slidingScreenItems.size() == slidingScreen) {
                slidingScreenItems.remove(0);
            }

            slidingScreenItems.add(weatherData.getWindSpeed());

            double sum = 0;

            for (double item : slidingScreenItems) {
                sum += item;
            }

            double avarageWindSpeed = sum / slidingScreenItems.size();
            Log.i(TAG, "windspeed: " + avarageWindSpeed);

            DataPoint windSpeedData = new DataPoint(weatherDataCount, avarageWindSpeed);
            DataPoint temperatureData = new DataPoint(weatherDataCount, weatherData.getTemperature());

            windSpeedSeries.appendData(windSpeedData, true, NUM_SAMPLES);
            temperatureSeries.appendData(temperatureData, true, NUM_SAMPLES);

            weatherDataCount++;

            if (windSpeedDataPoints.size() == NUM_SAMPLES) {
                windSpeedDataPoints.remove(0);
                temperatureDataPoints.remove(0);
            }

            windSpeedDataPoints.add(windSpeedData);
            temperatureDataPoints.add(temperatureData);

            windSpeedGraph.getViewport().setMaxY(maxValue(windSpeedDataPoints) + 0.25);
            windSpeedGraph.getViewport().setMinY(minValue(windSpeedDataPoints) - 0.25);

            temperatureGraph.getViewport().setMaxY(maxValue(temperatureDataPoints) + 0.25);
            temperatureGraph.getViewport().setMinY(minValue(temperatureDataPoints) - 0.25);
        }
    }

    public double minValue(List<DataPoint> dataPoints) {
        double minValue = dataPoints.get(0).getY();

        for(DataPoint data: dataPoints) {
            if(data.getY() < minValue)
                minValue = data.getY();
        }

        return minValue;
    }

    public double maxValue(List<DataPoint> dataPoints) {
        double maxValue = dataPoints.get(0).getY();

        for(DataPoint data: dataPoints) {
            if(data.getY() > maxValue)
                maxValue = data.getY();
        }

        return maxValue;
    }

    public interface OnFragmentInteractionListener {
        public void registerWeatherDataReceiver(WeatherListener weatherListener);
    }

}
