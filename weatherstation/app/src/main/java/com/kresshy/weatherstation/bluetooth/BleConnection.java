package com.kresshy.weatherstation.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.kresshy.weatherstation.connection.Connection;
import com.kresshy.weatherstation.connection.ConnectionState;
import com.kresshy.weatherstation.connection.HardwareEventListener;
import com.kresshy.weatherstation.util.PermissionHelper;

import timber.log.Timber;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;

/**
 * Manages Bluetooth Low Energy (BLE) connections to the Weather Station. Supports standard Nordic
 * UART service (NUS) commonly used in weather station modules.
 */
public class BleConnection implements Connection {

    private final Context context;
    private HardwareEventListener listener;
    private ConnectionState state = ConnectionState.stopped;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic uartWriteCharacteristic;

    // Standard Nordic UART Service (NUS) UUIDs
    private static final UUID UART_SERVICE_UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UART_RX_CHARACTERISTIC_UUID =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); // Device TX
    private static final UUID UART_TX_CHARACTERISTIC_UUID =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); // Device RX

    // Alternative common HM-10 UART UUIDs
    private static final UUID HM10_SERVICE_UUID =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHAR_UUID =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private final StringBuilder messageBuffer = new StringBuilder();
    private static final String END_MARKER = "_end";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public BleConnection(@dagger.hilt.android.qualifiers.ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public void start(HardwareEventListener listener) {
        this.listener = listener;
        this.state = ConnectionState.disconnected;
        listener.onConnectionStateChange(ConnectionState.disconnected);
    }

    @Override
    public void connect(Parcelable device, HardwareEventListener listener) {
        if (!(device instanceof BluetoothDevice)) return;
        BluetoothDevice btDevice = (BluetoothDevice) device;

        this.listener = listener;
        this.state = ConnectionState.connecting;
        listener.onConnectionStateChange(ConnectionState.connecting);

        if (PermissionHelper.hasConnectPermission(context)) {
            bluetoothGatt = btDevice.connectGatt(context, false, gattCallback);
        } else {
            Timber.e("Missing BLUETOOTH_CONNECT permission for BLE");
            listener.onToastMessage("Missing Permissions");
        }
    }

    @Override
    public void stop() {
        if (bluetoothGatt != null) {
            if (PermissionHelper.hasConnectPermission(context)) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }
        state = ConnectionState.stopped;
        if (listener != null) {
            listener.onConnectionStateChange(ConnectionState.stopped);
        }
    }

    @Override
    public void write(byte[] out) {
        if (uartWriteCharacteristic != null
                && bluetoothGatt != null
                && state == ConnectionState.connected) {
            if (PermissionHelper.hasConnectPermission(context)) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    bluetoothGatt.writeCharacteristic(
                            uartWriteCharacteristic,
                            out,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                } else {
                    uartWriteCharacteristic.setValue(out);
                    bluetoothGatt.writeCharacteristic(uartWriteCharacteristic);
                }
            }
        }
    }

    @Override
    public ConnectionState getState() {
        return state;
    }

    @Override
    public void setCallback(HardwareEventListener listener) {
        this.listener = listener;
    }

    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Timber.d(
                                "BLE Connected: %s, discovering services...",
                                gatt.getDevice().getAddress());
                        if (PermissionHelper.hasConnectPermission(context)) {
                            gatt.discoverServices();
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Timber.d("BLE Disconnected: %s", gatt.getDevice().getAddress());
                        state = ConnectionState.disconnected;
                        mainHandler.post(
                                () -> {
                                    if (listener != null) {
                                        listener.onConnectionStateChange(
                                                ConnectionState.disconnected);
                                        listener.onToastMessage("BLE Connection Lost");
                                    }
                                });
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Timber.d("BLE Services discovered for %s", gatt.getDevice().getAddress());
                        boolean found = setupUartCharacteristic(gatt);
                        if (found) {
                            Timber.d("BLE UART Service and Characteristics found.");
                            state = ConnectionState.connected;
                            mainHandler.post(
                                    () -> {
                                        if (listener != null) {
                                            listener.onConnected();
                                            listener.onConnectionStateChange(
                                                    ConnectionState.connected);
                                        }
                                    });
                        } else {
                            Timber.e(
                                    "No UART service found on BLE device: %s",
                                    gatt.getDevice().getAddress());
                            mainHandler.post(
                                    () -> {
                                        if (listener != null)
                                            listener.onToastMessage("Incompatible BLE device");
                                    });
                            stop();
                        }
                    } else {
                        Timber.e("BLE Service discovery failed with status: %d", status);
                    }
                }

                @Override
                public void onCharacteristicChanged(
                        BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    byte[] data;
                    if (android.os.Build.VERSION.SDK_INT
                            >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        data = characteristic.getValue();
                    } else {
                        data = characteristic.getValue();
                    }
                    processRawData(data);
                }

                @Override
                public void onCharacteristicChanged(
                        @NonNull BluetoothGatt gatt,
                        @NonNull BluetoothGattCharacteristic characteristic,
                        @NonNull byte[] value) {
                    processRawData(value);
                }
            };

    private boolean setupUartCharacteristic(BluetoothGatt gatt) {
        if (!PermissionHelper.hasConnectPermission(context)) return false;

        // Try NUS
        BluetoothGattService nusService = gatt.getService(UART_SERVICE_UUID);
        if (nusService != null) {
            BluetoothGattCharacteristic rxChar =
                    nusService.getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
            uartWriteCharacteristic = nusService.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);
            if (rxChar != null) {
                gatt.setCharacteristicNotification(rxChar, true);
                // Also need to write to CCCD for notifications in a real app,
                // but many modules enable it by default or use indications.
                return true;
            }
        }

        // Try HM-10
        BluetoothGattService hm10Service = gatt.getService(HM10_SERVICE_UUID);
        if (hm10Service != null) {
            BluetoothGattCharacteristic hmChar = hm10Service.getCharacteristic(HM10_CHAR_UUID);
            uartWriteCharacteristic = hmChar;
            if (hmChar != null) {
                gatt.setCharacteristicNotification(hmChar, true);
                return true;
            }
        }

        return false;
    }

    private void processRawData(byte[] data) {
        if (data == null || data.length == 0) return;

        String incoming = new String(data, StandardCharsets.UTF_8);
        Timber.v("BLE Data Received: %s", incoming);
        messageBuffer.append(incoming);

        int endIdx = messageBuffer.indexOf(END_MARKER);
        while (endIdx != -1) {
            int startWS = messageBuffer.lastIndexOf("WS_", endIdx);
            int startLegacy = messageBuffer.lastIndexOf("start_", endIdx);
            int startIdx = Math.max(startWS, startLegacy);

            if (startIdx != -1) {
                String fullMessage =
                        messageBuffer.substring(startIdx, endIdx + END_MARKER.length());
                Timber.d("Parsed BLE PDU: %s", fullMessage);
                mainHandler.post(
                        () -> {
                            if (listener != null) listener.onRawDataReceived(fullMessage);
                        });
                messageBuffer.delete(0, endIdx + END_MARKER.length());
            } else {
                messageBuffer.delete(0, endIdx + END_MARKER.length());
            }
            endIdx = messageBuffer.indexOf(END_MARKER);
        }
    }
}
