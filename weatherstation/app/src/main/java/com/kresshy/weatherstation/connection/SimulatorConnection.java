package com.kresshy.weatherstation.connection;

import android.os.Parcelable;

import timber.log.Timber;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * A mock connection that generates simulated weather data. Useful for testing UI logic and thermal
 * analysis without physical hardware. Simulates realistic air conditions, including occasional
 * thermal pulses.
 */
public class SimulatorConnection implements Connection {
    private ConnectionState state = ConnectionState.stopped;
    private ScheduledExecutorService executor;
    private final Random random = new Random();
    private RawDataCallback callback;

    @Inject
    public SimulatorConnection() {}

    // Simulation state
    private double currentTemp = 22.0;
    private double currentWind = 3.0;
    private boolean thermalActive = false;
    private int thermalTicks = 0;

    @Override
    public void start(RawDataCallback callback) {
        this.callback = callback;
        state = ConnectionState.disconnected;
        callback.onConnectionStateChange(state);
        Timber.d("Simulator started");
    }

    @Override
    public void connect(Parcelable device, RawDataCallback callback) {
        this.callback = callback;
        state = ConnectionState.connecting;
        callback.onConnectionStateChange(state);

        // Simulate connection delay
        Executors.newSingleThreadScheduledExecutor()
                .schedule(
                        () -> {
                            state = ConnectionState.connected;
                            if (this.callback != null) {
                                this.callback.onConnectionStateChange(state);
                                this.callback.onConnected();
                                startDataSimulation();
                            }
                        },
                        1,
                        TimeUnit.SECONDS);
    }

    /** Starts a background thread that generates weather PDUs at 1Hz. */
    private void startDataSimulation() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(
                () -> {
                    if (state != ConnectionState.connected || callback == null) return;

                    // --- Realistic Air Simulation ---
                    // 2% chance to start a thermal
                    if (random.nextInt(100) < 2 && !thermalActive) {
                        thermalActive = true;
                        thermalTicks = 45; // 45 second thermal pulse
                        Timber.d("SIMULATOR: Strong Thermal Pulse Starting!");
                    }

                    if (thermalActive) {
                        // Rising temp, falling wind
                        currentTemp += 0.04 + (random.nextDouble() * 0.02);
                        currentWind -= 0.15 + (random.nextDouble() * 0.1);
                        thermalTicks--;
                        if (thermalTicks <= 0 || currentWind < 0.1) thermalActive = false;
                    } else {
                        // Drift back to baseline (22°C, 3m/s)
                        currentTemp +=
                                (22.0 - currentTemp) * 0.01 + (random.nextDouble() - 0.5) * 0.02;
                        currentWind +=
                                (3.0 - currentWind) * 0.01 + (random.nextDouble() - 0.5) * 0.1;
                    }

                    // Clamp values
                    currentWind = Math.max(0.1, currentWind);

                    // Construct PDU string matching the station contract: "WS_{JSON}_end"
                    String json =
                            String.format(
                                    "{\"version\":1,\"numberOfNodes\":1,\"measurements\":[{\"windSpeed\":%.2f,\"temperature\":%.2f,\"nodeId\":0}]}",
                                    currentWind, currentTemp);

                    String pdu = "WS_" + json + "_end";
                    if (callback != null) {
                        callback.onRawDataReceived(pdu);
                    }
                },
                0,
                1,
                TimeUnit.SECONDS); // 1Hz update rate
    }

    @Override
    public void stop() {
        state = ConnectionState.stopped;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void write(byte[] out) {
        Timber.d("Simulator received write: " + new String(out));
    }

    @Override
    public ConnectionState getState() {
        return state;
    }

    @Override
    public void setCallback(RawDataCallback callback) {
        this.callback = callback;
    }
}
