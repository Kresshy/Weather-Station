package com.kresshy.weatherstation.application;

import android.app.Application;

import com.kresshy.weatherstation.bluetooth.BluetoothConnection;
import com.kresshy.weatherstation.bluetooth.BluetoothConnection.State;

public class WeatherStationApplication extends Application {
	
	private BluetoothConnection mConnectionService = null;
	private State state = State.disconnected;
	
	@Override
	public void onCreate() {
		
	}
	
	public BluetoothConnection getConnectionService() {
		return mConnectionService;
	}

	public void setConnectionService(BluetoothConnection mConnectionService) {
		this.mConnectionService = mConnectionService;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

}
