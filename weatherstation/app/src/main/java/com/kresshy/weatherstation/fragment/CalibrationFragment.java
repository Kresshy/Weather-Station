package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.weather.Measurement;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherListener;


public class CalibrationFragment extends Fragment implements WeatherListener, View.OnClickListener {

    private String TAG = "CalibrationFragment";
    private OnFragmentInteractionListener mListener;
    private TextView windSpeedDiffView;
    private TextView tempDiffView;
    private double windSpeedDiff = 0.0;
    private double tempDiff = 0.0;

    public CalibrationFragment() {
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
        View view = inflater.inflate(R.layout.fragment_calibration, container, false);

        windSpeedDiffView = (TextView) view.findViewById(R.id.windspeed_diff);
        tempDiffView = (TextView) view.findViewById(R.id.temperature_diff);

        Button calibrateButton = (Button) view.findViewById(R.id.calibrate_button);
        calibrateButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void weatherDataReceived(WeatherData weatherData) {

    }

    @Override
    public void measurementReceived(Measurement measurement) {
        WeatherData data = measurement.getWeatherDataForNode(0);

        if (measurement.getNumberOfNodes() > 1) {
            WeatherData data2 = measurement.getWeatherDataForNode(1);
            windSpeedDiff = data.getWindSpeed() - data2.getWindSpeed();
            tempDiff = data.getTemperature() - data2.getTemperature();
        } else {
            mListener.startDashboardAfterCalibration();
        }

        windSpeedDiffView.setText(Double.toString(windSpeedDiff));
        tempDiffView.setText(Double.toString(tempDiff));
    }

    @Override
    public void onClick(View v) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getApplicationContext()
        );

        sharedPreferences.edit().putString(
                WSConstants.KEY_WIND_DIFF, Double.toString(windSpeedDiff)
        ).commit();

        sharedPreferences.edit().putString(
                WSConstants.KEY_TEMP_DIFF, Double.toString(tempDiff)
        ).commit();

        Log.i(TAG, "Calibration values - wind: " + windSpeedDiff + ", temp: " + tempDiff);

        mListener.startDashboardAfterCalibration();
    }

    public interface OnFragmentInteractionListener {
        void registerWeatherDataReceiver(WeatherListener weatherListener);

        void startDashboardAfterCalibration();
    }
}
