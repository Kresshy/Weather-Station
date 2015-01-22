package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.activity.WeatherStationActivity;


public class BluetoothDeviceListFragment extends Fragment implements AbsListView.OnItemClickListener {

    public final String TAG = "BluetoothDeviceListFragment";

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mNewDeviceListView;
    private AbsListView mPairedDeviceListView;
    private TextView pairedDevices;
    private TextView newDevices;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private OnFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BluetoothDeviceListFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            mListener.startBluetoothDiscovery();
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
        View view = inflater.inflate(R.layout.fragment_bluetoothdevice, container, false);

        // Set the adapter
        mNewDeviceListView = (AbsListView) view.findViewById(R.id.listview_new_devices);
        mPairedDeviceListView = (AbsListView) view.findViewById(R.id.listview_paired_devices);
        pairedDevices = (TextView) view.findViewById(R.id.text_paired_devices);
        newDevices = (TextView) view.findViewById(R.id.text_new_devices);

        mNewDeviceListView.setAdapter(((WeatherStationActivity) getActivity()).getNewDevicesArrayAdapter());
        mPairedDeviceListView.setAdapter(((WeatherStationActivity) getActivity()).getPairedDevicesArrayAdapter());

        // Set OnItemClickListener so we can be notified on item clicks
        mNewDeviceListView.setOnItemClickListener(this);
        mPairedDeviceListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(((WeatherStationActivity) getActivity()).getPairedDevices() != null) {
            pairedDevices.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Cancel discovery because it's costly and we're about to connect
            mListener.stopBluetoothDiscovery();

            // Get the device MAC address, which is the last 17 chars in the
            // View
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            mListener.onDeviceSelectedToConnect(address);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mNewDeviceListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        public void onDeviceSelectedToConnect(String address);
        public void startBluetoothDiscovery();
        public void stopBluetoothDiscovery();
    }
}
