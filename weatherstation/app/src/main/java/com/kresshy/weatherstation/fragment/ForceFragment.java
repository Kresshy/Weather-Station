package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.force.ForceData;
import com.kresshy.weatherstation.force.ForceListener;
import com.kresshy.weatherstation.force.ForceMeasurement;

import timber.log.Timber;


public class ForceFragment extends Fragment implements ForceListener {
    private static final String TAG = "ForceFragment";

    private int measurementCount = 1;
    private int numberOfSamples = 300;
    private boolean maximumForce = false;
    private int lastCount = 0;

    private LineGraphView forceGraph;
    private GraphViewData[] forceData;
    private GraphViewSeries forceSeries;

    private TextView pullForceTextView;

    private OnFragmentInteractionListener mListener;

    public ForceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            mListener.registerForceDataReceiver(this);
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

        View view = inflater.inflate(R.layout.fragment_force, container, false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getApplicationContext()
        );

        // keep screen on
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout forceGraphContainer = (LinearLayout) view.findViewById(R.id.forceGraphContainer);
        pullForceTextView = (TextView) view.findViewById(R.id.pullForceText);

        createViewForForceGraph(forceGraphContainer);

        return view;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        // release flag to keep screen on
        getActivity().getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mListener = null;
    }

    @Override
    public void measurementReceived(ForceMeasurement forceMeasurement) {
        handleIncomingMeasurement(forceMeasurement);
    }

    private void handleIncomingMeasurement(ForceMeasurement forceMeasurement) {
        if (!maximumForce) {
            for (ForceData data : forceMeasurement.getMeasurements()) {
                forceSeries.appendData(new GraphViewData(
                        data.getCount(),
                        data.getForce()
                ), true, numberOfSamples);
                lastCount = data.getCount();

                if (data.getForce() >= 4000) {
                    maximumForce = true;
                }
            }
        } else {
            for (ForceData data : forceMeasurement.getMeasurements()) {
                lastCount -= data.getCount();
                forceSeries.appendData(new GraphViewData(
                        lastCount,
                        data.getForce()
                ), true, numberOfSamples);
            }
        }


        pullForceTextView.setText(
                forceMeasurement.getMeasurements().get(
                        forceMeasurement.getMeasurements().size() - 1)
                        .getForce() + " gramms"
        );

        measurementCount++;
    }

    private void createViewForForceGraph(LinearLayout container) {
        Timber.d("Creating GraphView For WindSpeed");
        forceGraph = new LineGraphView(getActivity().getApplicationContext(), "Wind Speed");
        forceGraph.setScrollable(true);
        forceGraph.setViewPort(0, numberOfSamples);
        forceGraph.setGraphViewStyle(getGraphViewStyle());

        forceGraph.setHorizontalLabels(getHorizontalLabelsForGraph(numberOfSamples));

        GraphViewData[] forceData = new GraphViewData[1];
        forceData[0] = new GraphViewData(0, 0);
        this.forceData = forceData;

        GraphViewSeries forceSeries = new GraphViewSeries(
                "Pull force",
                new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, 9),
                forceData
        );

        this.forceSeries = forceSeries;
        forceGraph.addSeries(forceSeries);

        Timber.d("Adding GraphView For Pull Force to LayoutContainer");
        container.addView(forceGraph);
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
        void registerForceDataReceiver(ForceFragment forceFragment);
    }
}
