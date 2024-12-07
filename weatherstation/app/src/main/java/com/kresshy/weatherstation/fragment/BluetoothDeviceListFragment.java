package com.kresshy.weatherstation.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.activity.WSActivity;

import java.util.ArrayList;

public class BluetoothDeviceListFragment extends Fragment
        implements AbsListView.OnItemClickListener {

    private AbsListView bluetoothDevicesListView;
    private TextView bluetootDevicesTextView;

    private OnFragmentInteractionListener mListener;

    public BluetoothDeviceListFragment() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            mListener.startBluetoothDiscovery();
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
        View view = inflater.inflate(R.layout.fragment_bluetoothdevice, container, false);

        // Set the adapter
        bluetoothDevicesListView = (AbsListView) view.findViewById(R.id.listview_bluetooth_devices);

        bluetoothDevicesListView.setAdapter(
                ((WSActivity) getActivity()).getPairedDevicesArrayAdapter());

        bluetoothDevicesListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (((WSActivity) getActivity()).getPairedDevices() != null) {
            bluetootDevicesTextView.setVisibility(View.VISIBLE);
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

            //            String info = ((TextView) view).getText().toString();
            //            String address = info.substring(info.length() - 17);
            ArrayList<BluetoothDevice> devices = ((WSActivity) getActivity()).getBluetoothDevices();
            mListener.onDeviceSelectedToConnect(devices.get(position).getAddress());
        }
    }

    public void setEmptyText(CharSequence emptyText) {
        View emptyView = bluetoothDevicesListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    public interface OnFragmentInteractionListener {

        public void onDeviceSelectedToConnect(String address);

        public void startBluetoothDiscovery();

        public void stopBluetoothDiscovery();
    }
}
