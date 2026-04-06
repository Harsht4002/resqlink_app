package com.resqlink.app.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a navigation node in the building graph.
 * Node coordinates (x, y, z) must match the 3D model's coordinate system.
 */
public class Node {
    private final int roomId;
    private final String id;
    private final float x;
    private final float y;
    private final float z;
    private final float renderX;
    private final float renderY;
    private final float renderZ;
    private final int floor;
    private final List<String> neighborIds;

    public Node(int roomId, String id, float x, float y, float z, int floor) {
        this(roomId, id, x, y, z, x, y, z, floor);
    }

    public Node(int roomId,
                String id,
                float x,
                float y,
                float z,
                float renderX,
                float renderY,
                float renderZ,
                int floor) {
        this.roomId = roomId;
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.renderX = renderX;
        this.renderY = renderY;
        this.renderZ = renderZ;
        this.floor = floor;
        this.neighborIds = new ArrayList<>();
    }

    public int getRoomId() {
        return roomId;
    }

    public String getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public int getFloor() {
        return floor;
    }

    public float getRenderX() {
        return renderX;
    }

    public float getRenderY() {
        return renderY;
    }

    public float getRenderZ() {
        return renderZ;
    }

    public List<String> getNeighborIds() {
        return neighborIds;
    }

    public void addNeighbor(String neighborId) {
        if (!neighborIds.contains(neighborId)) {
            neighborIds.add(neighborId);
        }
    }

    public float[] getPosition() {
        return new float[]{x, y, z};
    }

    public float[] getRenderPosition() {
        return new float[]{renderX, renderY, renderZ};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{roomId=" + roomId
                + ", id='" + id + '\''
                + ", x=" + x
                + ", y=" + y
                + ", z=" + z
                + ", renderX=" + renderX
                + ", renderY=" + renderY
                + ", renderZ=" + renderZ
                + ", floor=" + floor
                + "}";
    }
}
