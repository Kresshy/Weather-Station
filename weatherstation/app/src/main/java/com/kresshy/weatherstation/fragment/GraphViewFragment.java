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

    private int measurementCount = 1;
    private int avarageBucketSize = 5;
    private int NUM_SAMPLES = 300;

    private LineGraphView windSpeedGraph;
    private LineGraphView temperatureGraph;

    private List<GraphViewData[]> windSpeedDataList;
    private List<GraphViewData[]> temperatureDataList;

    private List<GraphViewSeries> windSpeedSeriesList;
    private List<GraphViewSeries> temperatureSeriesList;

    private OnFragmentInteractionListener mListener;

    private Measurement previousMeasurement;
    private List<Measurement> lastMeasurementsList = new ArrayList<>(avarageBucketSize);

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

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        NUM_SAMPLES = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        windSpeedDataList = new ArrayList<>();
        temperatureDataList = new ArrayList<>();
        windSpeedSeriesList = new ArrayList<>();
        temperatureSeriesList = new ArrayList<>();

        LinearLayout windSpeedContainer = (LinearLayout) view.findViewById(R.id.windSpeedContainer);
        LinearLayout temperatureContainer = (LinearLayout) view.findViewById(R.id.temperatureContainer);

        createViewForWindSpeedGraph(windSpeedContainer);
        createViewForTemperatureGraph(temperatureContainer);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void weatherDataReceived(WeatherData weatherData) {
        // TODO unnecessary function remove from interface
    }

    @Override
    public void measurementReceived(Measurement measurement) {
        Log.i(TAG, "measurementCount: " + measurementCount);
        if (measurementCount == 1) {
            handleFirstIncomingMeasurement(measurement);
        } else {
            handleIncomingMeasurement(measurement);
        }
    }

    private void handleFirstIncomingMeasurement(Measurement measurement) {
        previousMeasurement = measurement;
        windSpeedGraph.removeAllSeries();
        temperatureGraph.removeAllSeries();

        for (int i = 0; i < measurement.getNumberOfNodes(); i++) {
            GraphViewData[] windSpeedData = new GraphViewData[1];
            GraphViewData[] temperatureData = new GraphViewData[1];

            try {
                windSpeedData = windSpeedDataList.get(i);
            } catch (IndexOutOfBoundsException e) {
                windSpeedDataList.add(i, windSpeedData);
            }

            try {
                temperatureData = temperatureDataList.get(i);
            } catch (IndexOutOfBoundsException e) {
                temperatureDataList.add(i, temperatureData);
            }

            windSpeedData[0] = new GraphViewData(0, measurement.getWeatherDataForNode(i).getWindSpeed());
            temperatureData[0] = new GraphViewData(0, measurement.getWeatherDataForNode(i).getTemperature());

            GraphViewSeries windSpeedSeries;
            GraphViewSeries temperatureSeries;

            windSpeedSeries = new GraphViewSeries("Wind Speed", new GraphViewSeries.GraphViewSeriesStyle(getColorForWindSpeedByNode(i), 3), windSpeedDataList.get(i));
            temperatureSeries = new GraphViewSeries("Temperature", new GraphViewSeries.GraphViewSeriesStyle(getColorForTemperatureByNode(i), 3), temperatureDataList.get(i));

            try {
                windSpeedSeries = windSpeedSeriesList.get(i);
            } catch (IndexOutOfBoundsException e) {
                windSpeedSeriesList.add(i, windSpeedSeries);
            }

            try {
                temperatureSeries = windSpeedSeriesList.get(i);
            } catch (IndexOutOfBoundsException e) {
                temperatureSeriesList.add(i, temperatureSeries);
            }

            windSpeedSeriesList.get(i).resetData(windSpeedDataList.get(i));
            temperatureSeriesList.get(i).resetData(temperatureDataList.get(i));

            windSpeedGraph.addSeries(windSpeedSeriesList.get(i));
            temperatureGraph.addSeries(temperatureSeriesList.get(i));
        }

        lastMeasurementsList.add(measurement);
        measurementCount++;
    }

    private void handleIncomingMeasurement(Measurement measurement) {
        // prevent adding false zero temperature measurements
        for (int j = 0; j < measurement.getNumberOfNodes(); j++) {
            if (measurement.getWeatherDataForNode(j).getTemperature() == 0.0) {
                measurement.getWeatherDataForNode(j).setTemperature(previousMeasurement.getWeatherDataForNode(j).getTemperature());
            }
        }

        // saving corrected measurement as previous
        previousMeasurement = measurement;

        // maintaining bucket for avarage calculation
        if (lastMeasurementsList.size() == avarageBucketSize + 1) {
            lastMeasurementsList.remove(0);
        }

        lastMeasurementsList.add(measurement);

        for (int i = 0; i < measurement.getNumberOfNodes(); i++) {
            double sum = 0;
            for (Measurement m : lastMeasurementsList) {
                sum += m.getWeatherDataForNode(i).getWindSpeed();
            }
            double avarageWindSpeed = sum / lastMeasurementsList.size();

            windSpeedSeriesList.get(i).appendData(new GraphViewData(measurementCount, avarageWindSpeed), true, NUM_SAMPLES);
            temperatureSeriesList.get(i).appendData(new GraphViewData(measurementCount, measurement.getWeatherDataForNode(i).getTemperature()), true, NUM_SAMPLES);
        }

        measurementCount++;
    }

    private int getColorForWindSpeedByNode(int i) {
        switch (i) {
            case 1:
                return Color.BLUE;
            case 2:
                return Color.GREEN;
            case 3:
                return Color.CYAN;
            default:
                return Color.BLACK;
        }
    }

    private int getColorForTemperatureByNode(int i) {
        switch (i) {
            case 1:
                return Color.RED;
            case 2:
                return Color.YELLOW;
            case 3:
                return Color.MAGENTA;
            default:
                return Color.BLACK;
        }
    }

    private void createViewForWindSpeedGraph(LinearLayout container) {
        windSpeedGraph = new LineGraphView(getActivity().getApplicationContext(), "Wind Speed");
        windSpeedGraph.setScrollable(true);
        // windSpeedGraph.setScalable(true);
        windSpeedGraph.setViewPort(0, NUM_SAMPLES);
        windSpeedGraph.setGraphViewStyle(getGraphViewStyle());

        windSpeedGraph.setHorizontalLabels(getHorizontalLabelsForGraph(NUM_SAMPLES));

        GraphViewData[] windSpeedData = new GraphViewData[1];
        windSpeedData[0] = new GraphViewData(0, 0);
        windSpeedDataList.add(windSpeedData);

        GraphViewSeries windSpeedSeries = new GraphViewSeries("Wind Speed", new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, 3), windSpeedData);
        windSpeedSeriesList.add(windSpeedSeries);
        windSpeedGraph.addSeries(windSpeedSeries);

        container.addView(windSpeedGraph);
    }

    private void createViewForTemperatureGraph(LinearLayout container) {
        temperatureGraph = new LineGraphView(getActivity().getApplicationContext(), "Temperature");
        temperatureGraph.setScrollable(true);
        // temperatureGraph.setScalable(true);
        temperatureGraph.setViewPort(0, NUM_SAMPLES);
        temperatureGraph.setGraphViewStyle(getGraphViewStyle());

        temperatureGraph.setHorizontalLabels(getHorizontalLabelsForGraph(NUM_SAMPLES));
        temperatureGraph.setShowHorizontalLabels(false);

        GraphViewData[] temperatureData = new GraphViewData[1];
        temperatureData[0] = new GraphViewData(0, 0);
        temperatureDataList.add(temperatureData);

        GraphViewSeries temperatureSeries = new GraphViewSeries("Temperature", new GraphViewSeries.GraphViewSeriesStyle(Color.RED, 3), temperatureData);
        temperatureSeriesList.add(temperatureSeries);
        temperatureGraph.addSeries(temperatureSeries);

        container.addView(temperatureGraph);
    }

    private String[] getHorizontalLabelsForGraph(int numberOfSamples) {
        final String[] horizontalLabels1min = new String[]{"1min", "45sec", "30sec", "15sec", "0min"};
        final String[] horizontalLabels2min = new String[]{"2min", "1min", "0min"};
        final String[] horizontalLabels5min = new String[]{"5min", "4min", "3min", "2min", "1min", "0min"};
        final String[] horizontalLabels10min = new String[]{"10min", "8min", "6min", "4min", "2min", "0min"};
        final String[] horizontalLabels20min = new String[]{"20min", "15min", "10min", "5min", "0min"};

        switch (numberOfSamples) {
            case 60:
                return horizontalLabels1min;
            case 120:
                return horizontalLabels2min;
            case 300:
                return horizontalLabels5min;
            case 600:
                return horizontalLabels10min;
            case 1200:
                return horizontalLabels20min;
            default:
                return horizontalLabels5min;
        }
    }

    private GraphViewStyle getGraphViewStyle() {
        GraphViewStyle graphViewStyle = new GraphViewStyle(Color.BLACK, Color.BLACK, Color.GRAY);
        graphViewStyle.setVerticalLabelsAlign(Paint.Align.LEFT);
        graphViewStyle.setVerticalLabelsWidth(80);
        return graphViewStyle;
    }

    public interface OnFragmentInteractionListener {
        void registerWeatherDataReceiver(WeatherListener weatherListener);
    }
}
