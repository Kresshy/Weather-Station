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
 * UART service (NUS) and other common BLE-to-Serial modules via dynamic property-based discovery.
 */
public class BleConnection implements Connection {

    private final Context context;
    private HardwareEventListener listener;
    private ConnectionState state = ConnectionState.stopped;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic uartWriteCharacteristic;

    // Well-known UART Service UUIDs for prioritization
    private static final UUID NUS_SERVICE_UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID NUS_RX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID NUS_TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    private static final UUID HM10_SERVICE_UUID =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHAR_UUID =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    // CCCD UUID for enabling notifications
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final StringBuilder messageBuffer = new StringBuilder();
    private static final String END_MARKER = "_end";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Constructs a new BleConnection.
     *
     * @param context The application context.
     */
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

        // Ensure any previous connection is fully closed before re-connecting
        if (bluetoothGatt != null) {
            Timber.d("Closing existing GATT before new connection attempt");
            if (PermissionHelper.hasConnectPermission(context)) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }

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
                        if (PermissionHelper.hasConnectPermission(context)) {
                            gatt.close();
                        }
                        if (bluetoothGatt == gatt) {
                            bluetoothGatt = null;
                        }
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
                        if (!found) {
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
                public void onDescriptorWrite(
                        BluetoothGatt gatt,
                        android.bluetooth.BluetoothGattDescriptor descriptor,
                        int status) {
                    if (CLIENT_CHARACTERISTIC_CONFIG_UUID.equals(descriptor.getUuid())
                            && status == BluetoothGatt.GATT_SUCCESS) {
                        Timber.i("BLE Notification channel confirmed ready.");
                        state = ConnectionState.connected;
                        mainHandler.post(
                                () -> {
                                    if (listener != null) {
                                        listener.onConnected();
                                        listener.onConnectionStateChange(ConnectionState.connected);
                                    }
                                });
                    }
                }

                @Override
                public void onCharacteristicChanged(
                        BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    // Handled by the modern overload below to ensure single processing
                }

                @Override
                public void onCharacteristicChanged(
                        @NonNull BluetoothGatt gatt,
                        @NonNull BluetoothGattCharacteristic characteristic,
                        @NonNull byte[] value) {
                    processRawData(value);
                }
            };

    // Well-known System Service UUIDs to ignore during dynamic discovery
    private static final UUID SERVICE_GENERIC_ACCESS =
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_GENERIC_ATTRIBUTE =
            UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");

    private boolean setupUartCharacteristic(BluetoothGatt gatt) {
        if (!PermissionHelper.hasConnectPermission(context)) return false;

        BluetoothGattCharacteristic rxCandidate = null;
        BluetoothGattCharacteristic txCandidate = null;

        // 1. Try Known Profiles First (Nordic NUS)
        BluetoothGattService nus = gatt.getService(NUS_SERVICE_UUID);
        if (nus != null) {
            rxCandidate = nus.getCharacteristic(NUS_RX_UUID);
            txCandidate = nus.getCharacteristic(NUS_TX_UUID);
            if (rxCandidate != null) {
                Timber.d("Latching onto Nordic NUS profile");
            }
        }

        // 2. Try HM-10
        if (rxCandidate == null) {
            BluetoothGattService hm10 = gatt.getService(HM10_SERVICE_UUID);
            if (hm10 != null) {
                rxCandidate = hm10.getCharacteristic(HM10_CHAR_UUID);
                txCandidate = rxCandidate; // HM-10 uses same char for RX/TX
                if (rxCandidate != null) {
                    Timber.d("Latching onto HM-10 UART profile");
                }
            }
        }

        // 3. Dynamic Fallback: Search by properties, excluding system services
        if (rxCandidate == null) {
            for (BluetoothGattService service : gatt.getServices()) {
                UUID serviceUuid = service.getUuid();

                // Skip well-known system services that might have Notify/Write (like Service
                // Changed)
                if (serviceUuid.equals(SERVICE_GENERIC_ACCESS)
                        || serviceUuid.equals(SERVICE_GENERIC_ATTRIBUTE)) {
                    continue;
                }

                BluetoothGattCharacteristic currentRx = null;
                BluetoothGattCharacteristic currentTx = null;

                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    int props = characteristic.getProperties();

                    // Look for Notify/Indicate (App RX)
                    if (currentRx == null
                            && ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                                    || (props & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                                            != 0)) {
                        currentRx = characteristic;
                    }

                    // Look for Write (App TX)
                    if (currentTx == null
                            && ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                                    || (props
                                                    & BluetoothGattCharacteristic
                                                            .PROPERTY_WRITE_NO_RESPONSE)
                                            != 0)) {
                        currentTx = characteristic;
                    }
                }

                // If we found a pair in this non-system service, use it
                if (currentRx != null) {
                    rxCandidate = currentRx;
                    txCandidate = currentTx;
                    Timber.i("Found potential Dynamic UART service: %s", serviceUuid);
                    break;
                }
            }
        }

        if (rxCandidate != null) {
            uartWriteCharacteristic = txCandidate;
            enableNotifications(gatt, rxCandidate);
            return true;
        }

        return false;
    }

    private void enableNotifications(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!PermissionHelper.hasConnectPermission(context)) return;

        gatt.setCharacteristicNotification(characteristic, true);
        android.bluetooth.BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        if (descriptor != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                        descriptor,
                        android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(
                        android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
            Timber.d("CCCD notification enabled for %s", characteristic.getUuid());
        }
    }

    private void processRawData(byte[] data) {
        if (data == null || data.length == 0) return;

        String incoming = new String(data, StandardCharsets.UTF_8).trim();
        if (incoming.isEmpty()) return;

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
