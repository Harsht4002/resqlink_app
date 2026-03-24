package com.resqlink.app.ble;

public class BeaconMapping {
    private final int beaconId;
    private final int roomId;
    private final String nodeId;

    public BeaconMapping(int beaconId, int roomId, String nodeId) {
        this.beaconId = beaconId;
        this.roomId = roomId;
        this.nodeId = nodeId;
    }

    public int getBeaconId() {
        return beaconId;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
