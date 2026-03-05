package com.resqlink.app.utils;

import com.resqlink.app.navigation.Node;

/**
 * Math utilities for pathfinding and path rendering.
 */
public final class MathUtils {

    private MathUtils() {}

    /**
     * Euclidean distance between two 3D points.
     */
    public static double euclideanDistance(float x1, float y1, float z1, float x2, float y2, float z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Euclidean distance between two nodes.
     */
    public static double euclideanDistance(Node a, Node b) {
        return euclideanDistance(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    /**
     * Direction vector from A to B, normalized. Returns {dx, dy, dz}.
     */
    public static float[] directionVector(float ax, float ay, float az, float bx, float by, float bz) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) {
            return new float[]{0, 0, 0};
        }
        return new float[]{dx / len, dy / len, dz / len};
    }
}
