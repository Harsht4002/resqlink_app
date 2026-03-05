package com.resqlink.app.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a navigation node in the building graph.
 * Node coordinates (x, y, z) must match the 3D model's coordinate system.
 */
public class Node {
    private final String id;
    private final float x;
    private final float y;
    private final float z;
    private final int floor;
    private final List<String> neighborIds;

    public Node(String id, float x, float y, float z, int floor) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.floor = floor;
        this.neighborIds = new ArrayList<>();
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
        return "Node{id='" + id + "', x=" + x + ", y=" + y + ", z=" + z + ", floor=" + floor + "}";
    }
}
