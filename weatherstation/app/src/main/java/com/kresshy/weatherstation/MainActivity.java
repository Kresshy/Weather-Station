package com.kresshy.weatherstation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.kresshy.weatherstation.bluetooth.BluetoothService;
import com.kresshy.weatherstation.fragment.BluetoothDeviceListFragment;
import com.kresshy.weatherstation.fragment.DashboardFragment;
import com.kresshy.weatherstation.fragment.NavigationDrawerFragment;

import java.util.Set;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, BluetoothDeviceListFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    // Member fields
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothDevice mBluetoothDevice;
    private static Set<BluetoothDevice> pairedDevices;
    public static String mConnectedDeviceName;
    public static String DEVICE_NAME;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISABLE_BT = 2;

    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_STATE = 3;
    public static final int MESSAGE_CONNECTED = 4;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                Log.v(TAG, "RECEIVED BLUETOOTH STATE CHANGE: STATE_TURNING_ON");
            }

            if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                Log.v(TAG, "RECEIVED BLUETOOTH STATE CHANGE: STATE_ON");

                pairedDevices = mBluetoothAdapter.getBondedDevices();

                // If there are paired devices, add each one to the ArrayAdapter
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
            }
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
                setTitle("Select Device");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "None found";
                    mNewDevicesArrayAdapter.add(noDevices);
                    Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "ONSTART");

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if (mBluetoothAdapter.isEnabled() && ((WeatherStationApplication) getApplication()).getConnectionService() == null) {
            ((WeatherStationApplication) getApplication()).setConnectionService(new BluetoothService(mHandler));
            ((WeatherStationApplication) getApplication()).getConnectionService().start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        if (mBluetoothAdapter.isEnabled()) {
            pairedDevices = mBluetoothAdapter.getBondedDevices();

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (position) {
            case 0:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new BluetoothDeviceListFragment())
                        .commit();

                break;
            case 1:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new DashboardFragment())
                        .commit();

                break;
            case 2:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, new DashboardFragment())
                        .commit();

                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String message = "";

            switch (msg.what) {

                case MESSAGE_TOAST:

                    message = (String) msg.obj;
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    break;

                case MESSAGE_READ:

                    byte[] data = (byte[]) msg.obj;
                    message = new String(data);

                    Log.i(TAG, message);

                    break;

                case MESSAGE_STATE:

                    ((WeatherStationApplication) getApplication()).setState((BluetoothService.State) msg.obj);

                    break;

                case MESSAGE_CONNECTED:

                    Toast.makeText(getApplicationContext(), "Connected to timer", Toast.LENGTH_LONG).show();

                    ((WeatherStationApplication) getApplication()).setState((BluetoothService.State) msg.obj);
                    // update the main content by replacing fragments
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, new DashboardFragment())
                            .commit();
                    break;
            }
        }
    };

    public ArrayAdapter<String> getPairedDevicesArrayAdapter() {
        return mPairedDevicesArrayAdapter;
    }

    public ArrayAdapter<String> getNewDevicesArrayAdapter() {
        return mNewDevicesArrayAdapter;
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    public static Set<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    @Override
    public void onDeviceSelectedToConnect(String address) {

        BluetoothDevice mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        Log.i(TAG, mBluetoothDevice.getName() + mBluetoothDevice.getAddress());

        ((WeatherStationApplication) getApplication()).getConnectionService().connect(mBluetoothDevice);
    }

    @Override
    public void startBluetoothDiscovery() {
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public void stopBluetoothDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }
}
