package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.application.WSConstants;
import com.kresshy.weatherstation.weather.WeatherMeasurement;
import com.kresshy.weatherstation.weather.WeatherData;
import com.kresshy.weatherstation.weather.WeatherListener;

import timber.log.Timber;


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

        windSpeedDiffView = (TextView) view.findViewById(R.id.windspeed_diff_value);
        tempDiffView = (TextView) view.findViewById(R.id.temperature_diff_value);

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
    public void measurementReceived(WeatherMeasurement weatherMeasurement) {
        WeatherData data = weatherMeasurement.getWeatherDataForNode(0);

        if (weatherMeasurement.getNumberOfNodes() > 1) {
            WeatherData data2 = weatherMeasurement.getWeatherDataForNode(1);
            windSpeedDiff = data.getWindSpeed() - data2.getWindSpeed();
            tempDiff = data.getTemperature() - data2.getTemperature();
        } else {
            Toast.makeText(
                    getActivity().getApplicationContext(),
                    getString(R.string.calibration_skip),
                    Toast.LENGTH_SHORT
            ).show();
            mListener.startDashboardAfterCalibration();
        }

        windSpeedDiffView.setText(Double.toString(windSpeedDiff));
        tempDiffView.setText(Double.toString(tempDiff) + "°C");
    }

    @Override
    public void onClick(View v) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getApplicationContext()
        );

        sharedPreferences.edit().putString(
                WSConstants.KEY_WIND_DIFF, Double.toString(0.0)
        ).commit();

        sharedPreferences.edit().putString(
                WSConstants.KEY_TEMP_DIFF, Double.toString(tempDiff)
        ).commit();

        Timber.d("Calibration values - wind: " + windSpeedDiff + ", temp: " + tempDiff);

        mListener.startDashboardAfterCalibration();
    }

    public interface OnFragmentInteractionListener {
        void registerWeatherDataReceiver(WeatherListener weatherListener);

        void startDashboardAfterCalibration();
    }
}
