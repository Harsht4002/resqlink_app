package com.resqlink.app.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class VictimBroadcaster {
    public interface Listener {
        void onStatus(String message);
    }

    private final Context context;
    private final int networkId;
    private final int deviceId;
    private final Listener listener;
    private final BluetoothLeAdvertiser advertiser;
    private int seq = 1;
    private boolean broadcasting = false;

    public VictimBroadcaster(Context context, int networkId, int deviceId, Listener listener) {
        this.context = context;
        this.networkId = networkId;
        this.deviceId = deviceId;
        this.listener = listener;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        this.advertiser = adapter != null ? adapter.getBluetoothLeAdvertiser() : null;
    }

    @SuppressLint("MissingPermission")
    public void startBroadcast(int roomId, int floor) {
        if (!hasAdvertisePermission()) {
            listener.onStatus("Bluetooth advertise permission missing");
            return;
        }
        if (advertiser == null) {
            listener.onStatus("BLE advertiser unavailable");
            return;
        }
        stopBroadcast();
        byte[] payload = BlePacketCodec.encodeDeviceLocation(
                networkId,
                BlePacketCodec.ROLE_VICTIM,
                deviceId,
                roomId,
                floor,
                0,
                5,
                seq++
        );
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(false)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .addManufacturerData(BlePacketCodec.MANUFACTURER_ID, payload)
                .setIncludeDeviceName(false)
                .build();
        advertiser.startAdvertising(settings, data, callback);
        broadcasting = true;
        listener.onStatus("Victim broadcast started");
    }

    @SuppressLint("MissingPermission")
    public void stopBroadcast() {
        if (advertiser != null && broadcasting) {
            advertiser.stopAdvertising(callback);
        }
        broadcasting = false;
    }

    public boolean isBroadcasting() {
        return broadcasting;
    }

    private final AdvertiseCallback callback = new AdvertiseCallback() {};

    private boolean hasAdvertisePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
