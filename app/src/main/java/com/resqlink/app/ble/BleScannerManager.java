package com.resqlink.app.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.SparseArray;

import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public class BleScannerManager {
    public interface Listener {
        void onDetectedRoom(int roomId, int beaconId);
        void onVictimLocation(BlePacketCodec.DeviceLocationPacket packet);
        void onScanError(String message);
    }

    private final Context context;
    private final int networkId;
    private final Listener listener;
    private final Map<Integer, BeaconReading> beaconReadings = new HashMap<>();
    private int lastDetectedRoom = -1;

    private static class BeaconReading {
        int rssi;
        int roomId;
        long timestampMs;
    }

    public BleScannerManager(Context context, int networkId, Listener listener) {
        this.context = context;
        this.networkId = networkId;
        this.listener = listener;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (!hasScanPermission()) {
            listener.onScanError("Bluetooth scan permission missing");
            return;
        }
        BluetoothAdapter adapter = getBluetoothAdapter();
        if (adapter == null) {
            listener.onScanError("Bluetooth unsupported on this device");
            return;
        }
        if (!adapter.isEnabled()) {
            listener.onScanError("Bluetooth is turned off");
            return;
        }
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            listener.onScanError("Bluetooth LE scanner unavailable");
            return;
        }
        scanner.startScan(scanCallback);
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        BluetoothAdapter adapter = getBluetoothAdapter();
        BluetoothLeScanner scanner = adapter != null ? adapter.getBluetoothLeScanner() : null;
        if (scanner != null) {
            scanner.stopScan(scanCallback);
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getScanRecord() == null) return;
            SparseArray<byte[]> manufacturerData = result.getScanRecord().getManufacturerSpecificData();
            if (manufacturerData == null) return;
            byte[] payload = manufacturerData.get(BlePacketCodec.MANUFACTURER_ID);
            if (payload == null || payload.length < 2) return;

            int pktType = payload[0] & 0xFF;
            if (pktType == BlePacketCodec.PACKET_TYPE_ROOM_BEACON) {
                BlePacketCodec.RoomBeaconPacket packet = BlePacketCodec.decodeRoomBeacon(payload);
                if (packet == null || packet.networkId != networkId) return;
                updateSmoothedRoom(packet.beaconId, packet.roomId, result.getRssi());
            } else if (pktType == BlePacketCodec.PACKET_TYPE_DEVICE_LOCATION) {
                BlePacketCodec.DeviceLocationPacket packet = BlePacketCodec.decodeDeviceLocation(payload);
                if (packet == null || packet.networkId != networkId) return;
                if (packet.role == BlePacketCodec.ROLE_VICTIM) {
                    listener.onVictimLocation(packet);
                }
            }
        }
    };

    private void updateSmoothedRoom(int beaconId, int roomId, int rssi) {
        long now = SystemClock.elapsedRealtime();
        BeaconReading reading = new BeaconReading();
        reading.rssi = rssi;
        reading.roomId = roomId;
        reading.timestampMs = now;
        beaconReadings.put(beaconId, reading);

        int bestBeaconId = -1;
        int bestRoomId = -1;
        int bestRssi = Integer.MIN_VALUE;
        for (Map.Entry<Integer, BeaconReading> entry : beaconReadings.entrySet()) {
            BeaconReading candidate = entry.getValue();
            if (now - candidate.timestampMs > 5000) continue;
            if (candidate.rssi > bestRssi) {
                bestRssi = candidate.rssi;
                bestBeaconId = entry.getKey();
                bestRoomId = candidate.roomId;
            }
        }

        if (bestBeaconId != -1 && bestRoomId != lastDetectedRoom) {
            lastDetectedRoom = bestRoomId;
            listener.onDetectedRoom(bestRoomId, bestBeaconId);
        }
    }

    private boolean hasScanPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }
}
