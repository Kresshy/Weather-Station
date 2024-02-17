package com.kresshy.weatherstation.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.wifi.WifiConnection;
import com.kresshy.weatherstation.wifi.WifiDevice;

import java.util.List;

import timber.log.Timber;


public class WifiFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final int SERVERPORT = 3000;
    private static final String SERVER_IP = "192.168.100.155";
    private AbsListView wifiDeviceListView;
    private ArrayAdapter<String> wifiDeviceArrayAdapter;


    WifiManager wifiManager;
    WifiReceiver wifiReceiver;

    StringBuilder sb = new StringBuilder();

    private final Handler handler = new Handler();

    private static final String TAG = "WIFIFragment";
    private WifiConnection wifiConnection;

    OnFragmentInteractionListener mListener;

    public WifiFragment() {
        // Required empty public constructor
    }

    public static WifiFragment newInstance() {
        WifiFragment fragment = new WifiFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiReceiver = new WifiReceiver();
        getActivity().registerReceiver(wifiReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (wifiManager.isWifiEnabled() == false) {
            wifiManager.setWifiEnabled(true);
        }

        doInback();
    }

    public void doInback() {
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                wifiManager.startScan();
                doInback();
            }
        }, 1000);

    }

    class WifiReceiver extends BroadcastReceiver {
        @SuppressLint("MissingPermission")
        public void onReceive(Context c, Intent intent) {
            sb = new StringBuilder();

            List<ScanResult> wifiList;

            wifiList = wifiManager.getScanResults();

            for (int i = 0; i < wifiList.size(); i++) {
                wifiDeviceArrayAdapter.add(wifiList.get(i).SSID);
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi, container, false);
        Button connectButton = (Button) view.findViewById(R.id.connect);
        Button disconnectButton = (Button) view.findViewById(R.id.disconnect);

        wifiDeviceArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.wifi_device);

        wifiDeviceListView = (AbsListView) view.findViewById(R.id.listview_wifi_devices);
        wifiDeviceListView.setAdapter(wifiDeviceArrayAdapter);
        wifiDeviceListView.setOnItemClickListener(this);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.d( "Clicked on connect Button");
                mListener.onDeviceSelectedToConnect(WifiDevice.create(SERVER_IP, SERVERPORT));
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(getActivity());

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                connectToSelectedWifi(wifiDeviceArrayAdapter.getItem(position), input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void connectToSelectedWifi(String networkSSID, String networkPass) {

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";   // Please

        conf.wepKeys[0] = "\"" + networkPass + "\"";
        conf.wepTxKeyIndex = 0;
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        conf.preSharedKey = "\"" + networkPass + "\"";

        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(conf);
        @SuppressLint("MissingPermission") List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }
    }

    public interface OnFragmentInteractionListener {

        public void onDeviceSelectedToConnect(WifiDevice device);
    }
}
