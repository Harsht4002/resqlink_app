package com.resqlink.app.ble;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class BlePacketCodec {
    public static final int PACKET_TYPE_ROOM_BEACON = 0;
    public static final int PACKET_TYPE_DEVICE_LOCATION = 1;
    public static final int VERSION = 1;
    public static final int MANUFACTURER_ID = 0x1234;
    public static final int ROLE_VICTIM = 0;
    public static final int ROLE_RESCUER = 1;
    public static final int ROLE_RELAY = 2;

    private BlePacketCodec() {}

    public static byte[] encodeRoomBeacon(int networkId, int roomId, int floor, int flags, int beaconId) {
        ByteBuffer buf = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) PACKET_TYPE_ROOM_BEACON);
        buf.put((byte) VERSION);
        buf.putShort((short) networkId);
        buf.putShort((short) roomId);
        buf.put((byte) floor);
        buf.put((byte) flags);
        buf.putShort((short) beaconId);
        return buf.array();
    }

    public static byte[] encodeDeviceLocation(
            int networkId,
            int role,
            int deviceId,
            int roomId,
            int floor,
            int hopCount,
            int ttl,
            int seq
    ) {
        ByteBuffer buf = ByteBuffer.allocate(15).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) PACKET_TYPE_DEVICE_LOCATION);
        buf.put((byte) VERSION);
        buf.putShort((short) networkId);
        buf.put((byte) role);
        buf.putShort((short) deviceId);
        buf.putShort((short) roomId);
        buf.put((byte) floor);
        buf.put((byte) hopCount);
        buf.put((byte) ttl);
        buf.put((byte) (seq & 0xFF));
        buf.put((byte) ((seq >> 8) & 0xFF));
        buf.put((byte) ((seq >> 16) & 0xFF));
        return buf.array();
    }

    public static RoomBeaconPacket decodeRoomBeacon(byte[] data) {
        if (data == null || data.length < 10) return null;
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int packetType = buf.get() & 0xFF;
        int version = buf.get() & 0xFF;
        if (packetType != PACKET_TYPE_ROOM_BEACON || version != VERSION) return null;
        int networkId = buf.getShort() & 0xFFFF;
        int roomId = buf.getShort() & 0xFFFF;
        int floor = buf.get();
        int flags = buf.get() & 0xFF;
        int beaconId = buf.getShort() & 0xFFFF;
        return new RoomBeaconPacket(networkId, roomId, floor, flags, beaconId);
    }

    public static DeviceLocationPacket decodeDeviceLocation(byte[] data) {
        if (data == null || data.length < 15) return null;
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int packetType = buf.get() & 0xFF;
        int version = buf.get() & 0xFF;
        if (packetType != PACKET_TYPE_DEVICE_LOCATION || version != VERSION) return null;
        int networkId = buf.getShort() & 0xFFFF;
        int role = buf.get() & 0xFF;
        int deviceId = buf.getShort() & 0xFFFF;
        int roomId = buf.getShort() & 0xFFFF;
        int floor = buf.get();
        int hopCount = buf.get() & 0xFF;
        int ttl = buf.get() & 0xFF;
        int seq = (buf.get() & 0xFF) | ((buf.get() & 0xFF) << 8) | ((buf.get() & 0xFF) << 16);
        return new DeviceLocationPacket(networkId, role, deviceId, roomId, floor, hopCount, ttl, seq);
    }

    public static class RoomBeaconPacket {
        public final int networkId;
        public final int roomId;
        public final int floor;
        public final int flags;
        public final int beaconId;

        public RoomBeaconPacket(int networkId, int roomId, int floor, int flags, int beaconId) {
            this.networkId = networkId;
            this.roomId = roomId;
            this.floor = floor;
            this.flags = flags;
            this.beaconId = beaconId;
        }
    }

    public static class DeviceLocationPacket {
        public final int networkId;
        public final int role;
        public final int deviceId;
        public final int roomId;
        public final int floor;
        public final int hopCount;
        public final int ttl;
        public final int seq;

        public DeviceLocationPacket(
                int networkId,
                int role,
                int deviceId,
                int roomId,
                int floor,
                int hopCount,
                int ttl,
                int seq
        ) {
            this.networkId = networkId;
            this.role = role;
            this.deviceId = deviceId;
            this.roomId = roomId;
            this.floor = floor;
            this.hopCount = hopCount;
            this.ttl = ttl;
            this.seq = seq;
        }
    }
}
