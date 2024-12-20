package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherListener;

import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

public class GraphViewFragment extends Fragment implements WeatherListener {

    private int measurementCount = 1;
    private int numberOfSamples = 300;
    private final int averageBucketSize = 5;

    private LineGraphView windSpeedGraph;
    private LineGraphView temperatureGraph;

    private List<GraphViewData[]> windSpeedDataList;
    private List<GraphViewData[]> temperatureDataList;

    private List<GraphViewSeries> windSpeedSeriesList;
    private List<GraphViewSeries> temperatureSeriesList;

    private OnFragmentInteractionListener mListener;

    private Measurement previousMeasurement;
    private List<Measurement> lastMeasurementsList = new ArrayList<>(averageBucketSize);

    private double correctionWind = 0.0;
    private double correctionTemp = 0.0;

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
            throw new ClassCastException(
                    activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        getActivity().getApplicationContext());

        numberOfSamples =
                Integer.parseInt(
                        sharedPreferences.getString(SettingsFragment.KEY_PREF_INTERVAL, "300"));

        correctionWind =
                Double.parseDouble(sharedPreferences.getString(WSConstants.KEY_WIND_DIFF, "0.0"));

        correctionTemp =
                Double.parseDouble(sharedPreferences.getString(WSConstants.KEY_TEMP_DIFF, "0.0"));

        Timber.d("Correction values - wind: " + correctionWind + ", temp: " + correctionTemp);

        // keep screen on
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        windSpeedDataList = new ArrayList<>();
        temperatureDataList = new ArrayList<>();
        windSpeedSeriesList = new ArrayList<>();
        temperatureSeriesList = new ArrayList<>();

        LinearLayout windSpeedContainer = (LinearLayout) view.findViewById(R.id.windSpeedContainer);
        LinearLayout temperatureContainer =
                (LinearLayout) view.findViewById(R.id.temperatureContainer);

        createViewForWindSpeedGraph(windSpeedContainer);
        createViewForTemperatureGraph(temperatureContainer);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // release flag to keep screen on
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mListener = null;
    }

    @Override
    public void weatherDataReceived(WeatherData weatherData) {
        // TODO unnecessary function remove from interface
    }

    @Override
    public void measurementReceived(Measurement measurement) {
        Timber.d("measurementCount: " + measurementCount);
        if (measurementCount == 1) {
            handleFirstIncomingMeasurement(measurement);
        } else {
            handleIncomingMeasurement(measurement);
        }
    }

    private void handleFirstIncomingMeasurement(Measurement measurement) {
        Timber.d("Handling the (1st) first incoming message");
        previousMeasurement = measurement;
        Timber.d("Cleaning the graphViews");
        windSpeedGraph.removeAllSeries();
        temperatureGraph.removeAllSeries();
        windSpeedDataList = new ArrayList<>();
        temperatureDataList = new ArrayList<>();
        windSpeedSeriesList = new ArrayList<>();
        temperatureSeriesList = new ArrayList<>();

        for (int i = 0; i < measurement.getNumberOfNodes(); i++) {
            GraphViewData[] windSpeedData = new GraphViewData[1];
            GraphViewData[] temperatureData = new GraphViewData[1];

            if (i > 0) {
                windSpeedData[0] =
                        new GraphViewData(
                                0,
                                measurement.getWeatherDataForNode(i).getWindSpeed()
                                        + correctionWind);
                temperatureData[0] =
                        new GraphViewData(
                                0,
                                measurement.getWeatherDataForNode(i).getTemperature()
                                        + correctionTemp);
            } else {
                windSpeedData[0] =
                        new GraphViewData(0, measurement.getWeatherDataForNode(i).getWindSpeed());
                temperatureData[0] =
                        new GraphViewData(0, measurement.getWeatherDataForNode(i).getTemperature());
            }

            try {
                windSpeedData = windSpeedDataList.get(i);
            } catch (IndexOutOfBoundsException e) {
                Timber.d(
                        "Cannot find windSpeedData for nodeId: "
                                + i
                                + " creating new GraphViewDataArray");
                windSpeedDataList.add(i, windSpeedData);
            }

            try {
                temperatureData = temperatureDataList.get(i);
            } catch (IndexOutOfBoundsException e) {
                Timber.d(
                        "Cannot find temperatureData for nodeId: "
                                + i
                                + " creating new GraphViewDataArray");
                temperatureDataList.add(i, temperatureData);
            }

            GraphViewSeries windSpeedSeries;
            GraphViewSeries temperatureSeries;

            windSpeedSeries =
                    new GraphViewSeries(
                            "Wind Speed",
                            new GraphViewSeries.GraphViewSeriesStyle(
                                    getColorForWindSpeedByNode(i), getLineWidthByNode(i)),
                            windSpeedDataList.get(i));

            temperatureSeries =
                    new GraphViewSeries(
                            "Temperature",
                            new GraphViewSeries.GraphViewSeriesStyle(
                                    getColorForTemperatureByNode(i), getLineWidthByNode(i)),
                            temperatureDataList.get(i));

            try {
                windSpeedSeries = windSpeedSeriesList.get(i);
            } catch (IndexOutOfBoundsException e) {
                Timber.d(
                        "Cannot find windSpeedSeries for nodeId: "
                                + i
                                + " creating new GraphViewSeries");
                windSpeedSeriesList.add(i, windSpeedSeries);
            }

            try {
                temperatureSeries = temperatureSeriesList.get(i);
            } catch (IndexOutOfBoundsException e) {
                Timber.d(
                        "Cannot find temperatureSeries for nodeId: "
                                + i
                                + " creating new GraphViewSeries");
                temperatureSeriesList.add(i, temperatureSeries);
            }

            Timber.d("Reset data in new Series for nodeId: " + i);
            windSpeedSeriesList.get(i).resetData(windSpeedDataList.get(i));
            temperatureSeriesList.get(i).resetData(temperatureDataList.get(i));

            Timber.d("Adding Series for GraphView for nodeId: " + i);
            windSpeedGraph.addSeries(windSpeedSeriesList.get(i));
            temperatureGraph.addSeries(temperatureSeriesList.get(i));
        }

        lastMeasurementsList.add(measurement);
        measurementCount++;
    }

    private void handleIncomingMeasurement(Measurement measurement) {
        // prevent adding false zero temperature measurements
        Timber.d("Filtering wrong measurements and load previous values");
        for (int j = 0; j < measurement.getNumberOfNodes(); j++) {
            if (previousMeasurement.hasNodeId(j) && measurement.hasNodeId(j)) {
                if (measurement.getWeatherDataForNode(j).getTemperature() == 0.0
                        || (measurement.getWeatherDataForNode(j).getTemperature()
                                        - previousMeasurement
                                                .getWeatherDataForNode(j)
                                                .getTemperature()
                                > 3.0)) {
                    measurement
                            .getWeatherDataForNode(j)
                            .setTemperature(
                                    previousMeasurement.getWeatherDataForNode(j).getTemperature());
                }
            }
        }

        previousMeasurement = measurement;

        // maintaining bucket for average calculation
        if (lastMeasurementsList.size() == averageBucketSize + 1) {
            lastMeasurementsList.remove(0);
        }

        lastMeasurementsList.add(measurement);

        for (int i = 0; i < measurement.getNumberOfNodes(); i++) {
            double sumWindSpeed = 0;
            double sumTemperature = 0;

            int missingMeasurements = 0;

            for (Measurement m : lastMeasurementsList) {
                if (!(m.getNumberOfNodes() < i)) { // if measurement has missing data of nodes
                    if (m.hasNodeId(i)) { // ensuring node exists
                        if (i > 0) {
                            sumWindSpeed +=
                                    (m.getWeatherDataForNode(i).getWindSpeed() + correctionWind);
                            sumTemperature +=
                                    (m.getWeatherDataForNode(i).getTemperature() + correctionTemp);
                        } else {
                            sumWindSpeed += m.getWeatherDataForNode(i).getWindSpeed();
                            sumTemperature += m.getWeatherDataForNode(i).getTemperature();
                        }
                    } else { // else doesn't count in the average calc
                        missingMeasurements++;
                    }
                }
            }

            double averageWindSpeed =
                    sumWindSpeed / (lastMeasurementsList.size() - missingMeasurements);
            double averageTemperature =
                    sumTemperature / (lastMeasurementsList.size() - missingMeasurements);

            Timber.d("SumWindSpeed: " + sumWindSpeed + ", sumTemp: " + sumTemperature);
            Timber.d("Missing measurements: " + missingMeasurements);
            Timber.d("LastMeasurementList: " + lastMeasurementsList.toString());
            Timber.d(
                    "AverageWindSpeed: "
                            + averageWindSpeed
                            + ", AverageTemp: "
                            + averageTemperature);
            Timber.d("Adding data in Series for nodeId: " + i);
            Timber.d("Number of samples: " + numberOfSamples);

            try {
                windSpeedSeriesList
                        .get(i)
                        .appendData(
                                new GraphViewData(measurementCount, averageWindSpeed),
                                true,
                                numberOfSamples);
            } catch (IndexOutOfBoundsException e) {
                Timber.d(
                        "Cannot find windSpeedSeries for nodeId: "
                                + i
                                + " creating new GraphViewSeries");
                GraphViewData[] windSpeedData = new GraphViewData[1];
                windSpeedData[0] =
                        new GraphViewData(
                                measurementCount,
                                measurement.getWeatherDataForNode(i).getWindSpeed());

                windSpeedDataList.add(i, windSpeedData);
                GraphViewSeries windSpeedSeries =
                        new GraphViewSeries(
                                "Wind Speed",
                                new GraphViewSeries.GraphViewSeriesStyle(
                                        getColorForWindSpeedByNode(i), getLineWidthByNode(i)),
                                windSpeedDataList.get(i));

                windSpeedSeriesList.add(i, windSpeedSeries);
                windSpeedSeriesList.get(i).resetData(windSpeedDataList.get(i));
                windSpeedGraph.addSeries(windSpeedSeriesList.get(i));
            }

            try {
                temperatureSeriesList
                        .get(i)
                        .appendData(
                                new GraphViewData(measurementCount, averageTemperature),
                                true,
                                numberOfSamples);
            } catch (IndexOutOfBoundsException e) {
                Timber.d(
                        "Cannot find windSpeedSeries for nodeId: "
                                + i
                                + " creating new GraphViewSeries");
                GraphViewData[] temperatureData = new GraphViewData[1];
                temperatureData[0] =
                        new GraphViewData(
                                measurementCount,
                                measurement.getWeatherDataForNode(i).getTemperature());

                temperatureDataList.add(i, temperatureData);
                GraphViewSeries temperatureSeries =
                        new GraphViewSeries(
                                "Temperature",
                                new GraphViewSeries.GraphViewSeriesStyle(
                                        getColorForTemperatureByNode(i), getLineWidthByNode(i)),
                                temperatureDataList.get(i));

                temperatureSeriesList.add(i, temperatureSeries);
                temperatureSeriesList.get(i).resetData(temperatureDataList.get(i));
                temperatureGraph.addSeries(temperatureSeriesList.get(i));
            }
        }

        measurementCount++;
    }

    private int getColorForWindSpeedByNode(int i) {
        switch (i) {
            case 0:
                return Color.RED;
            case 1:
                return Color.BLUE;
            case 2:
                return Color.DKGRAY;
            default:
                return Color.BLACK;
        }
    }

    private int getColorForTemperatureByNode(int i) {
        switch (i) {
            case 0:
                return Color.RED;
            case 1:
                return Color.BLUE;
            case 2:
                return Color.DKGRAY;
            default:
                return Color.BLACK;
        }
    }

    private int getLineWidthByNode(int i) {
        switch (i) {
            case 0:
                return 9;
            case 1:
                return 6;
            case 2:
                return 3;
            default:
                return 6;
        }
    }

    private void createViewForWindSpeedGraph(LinearLayout container) {
        Timber.d("Creating GraphView For WindSpeed");
        windSpeedGraph = new LineGraphView(getActivity().getApplicationContext(), "Wind Speed");
        windSpeedGraph.setScrollable(true);
        // windSpeedGraph.setScalable(true);
        windSpeedGraph.setViewPort(0, numberOfSamples);
        windSpeedGraph.setGraphViewStyle(getGraphViewStyle());

        windSpeedGraph.setHorizontalLabels(getHorizontalLabelsForGraph(numberOfSamples));

        GraphViewData[] windSpeedData = new GraphViewData[1];
        windSpeedData[0] = new GraphViewData(0, 0);
        windSpeedDataList.add(windSpeedData);

        GraphViewSeries windSpeedSeries =
                new GraphViewSeries(
                        "Wind Speed",
                        new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, getLineWidthByNode(0)),
                        windSpeedData);

        windSpeedSeriesList.add(windSpeedSeries);
        windSpeedGraph.addSeries(windSpeedSeries);

        Timber.d("Adding GraphView For WindSpeed to LayoutContainer");
        container.addView(windSpeedGraph);
    }

    private void createViewForTemperatureGraph(LinearLayout container) {
        Timber.d("Creating GraphView For Temperature");
        temperatureGraph = new LineGraphView(getActivity().getApplicationContext(), "Temperature");
        temperatureGraph.setScrollable(true);
        // temperatureGraph.setScalable(true);
        temperatureGraph.setViewPort(0, numberOfSamples);
        temperatureGraph.setGraphViewStyle(getGraphViewStyle());

        temperatureGraph.setHorizontalLabels(getHorizontalLabelsForGraph(numberOfSamples));
        temperatureGraph.setShowHorizontalLabels(false);

        GraphViewData[] temperatureData = new GraphViewData[1];
        temperatureData[0] = new GraphViewData(0, 0);
        temperatureDataList.add(temperatureData);

        GraphViewSeries temperatureSeries =
                new GraphViewSeries(
                        "Temperature",
                        new GraphViewSeries.GraphViewSeriesStyle(Color.RED, getLineWidthByNode(0)),
                        temperatureData);

        temperatureSeriesList.add(temperatureSeries);
        temperatureGraph.addSeries(temperatureSeries);

        Timber.d("Adding GraphView For Temperature to LayoutContainer");
        container.addView(temperatureGraph);
    }

    private String[] getHorizontalLabelsForGraph(int numberOfSamples) {
        final String[] horizontalLabels1min =
                new String[] {"1min", "45sec", "30sec", "15sec", "0min"};
        final String[] horizontalLabels2min = new String[] {"2min", "1min", "0min"};
        final String[] horizontalLabels5min =
                new String[] {"5min", "4min", "3min", "2min", "1min", "0min"};
        final String[] horizontalLabels10min =
                new String[] {"10min", "8min", "6min", "4min", "2min", "0min"};
        final String[] horizontalLabels20min =
                new String[] {"20min", "15min", "10min", "5min", "0min"};

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
