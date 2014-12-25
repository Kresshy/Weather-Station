package com.kresshy.weatherstation;

import android.app.Application;

import com.kresshy.weatherstation.bluetooth.BluetoothService;
import com.kresshy.weatherstation.bluetooth.BluetoothService.State;

public class WeatherStationApplication extends Application {
	
	private BluetoothService mConnectionService = null;
	private State state = State.disconnected;
	
	@Override
	public void onCreate() {
		
	}
	
	public BluetoothService getConnectionService() {
		return mConnectionService;
	}

	public void setConnectionService(BluetoothService mConnectionService) {
		this.mConnectionService = mConnectionService;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

}
