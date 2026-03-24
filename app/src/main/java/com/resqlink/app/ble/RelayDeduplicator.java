package com.resqlink.app.ble;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility logic for relay nodes: de-duplicate by (deviceId, seq)
 * and validate hop/ttl forwarding conditions.
 */
public class RelayDeduplicator {
    private final Map<Integer, Integer> latestSeqByDevice = new HashMap<>();

    public boolean shouldForward(BlePacketCodec.DeviceLocationPacket packet) {
        if (packet == null) return false;
        if (packet.hopCount >= packet.ttl) return false;
        Integer prevSeq = latestSeqByDevice.get(packet.deviceId);
        if (prevSeq != null && packet.seq <= prevSeq) {
            return false;
        }
        latestSeqByDevice.put(packet.deviceId, packet.seq);
        return true;
    }

    public byte[] buildForwardedPacket(BlePacketCodec.DeviceLocationPacket packet) {
        return BlePacketCodec.encodeDeviceLocation(
                packet.networkId,
                packet.role,
                packet.deviceId,
                packet.roomId,
                packet.floor,
                packet.hopCount + 1,
                packet.ttl,
                packet.seq
        );
    }
}
