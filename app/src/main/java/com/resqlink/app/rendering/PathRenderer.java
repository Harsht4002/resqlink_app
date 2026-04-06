package com.resqlink.app.rendering;


import com.resqlink.app.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;

import com.google.android.filament.MaterialInstance;

import dev.romainguy.kotlin.math.Float3;
import dev.romainguy.kotlin.math.Float4;
import dev.romainguy.kotlin.math.Quaternion;
import kotlin.Unit;
import io.github.sceneview.SceneView;
import io.github.sceneview.node.CylinderNode;

/**
 * Renders the navigation path as 3D line segments in the scene.
 * Path is raised by 0.1f above node Y to avoid clipping into the floor.
 */
public class PathRenderer {

    private static final float PATH_OFFSET_Y = 0.1f;
    private static final float PATH_HALO_RADIUS = 0.095f;
    private static final float PATH_CORE_RADIUS = 0.05f;
    private static final Float4 PATH_HALO_COLOR = new Float4(0.54f, 0.71f, 0.98f, 0.28f);
    private static final Float4 PATH_CORE_COLOR = new Float4(0.26f, 0.52f, 0.96f, 0.92f);

    private final SceneView sceneView;
    private final List<io.github.sceneview.node.Node> pathNodes = new ArrayList<>();

    public PathRenderer(SceneView sceneView) {
        this.sceneView = sceneView;
    }

    /**
     * Renders the path as cylinder segments between consecutive nodes.
     * Uses y = node.y + 0.1f to avoid clipping into the floor.
     */
    public void renderPath(List<float[]> pathPoints) {
        clearPath();
        if (pathPoints == null || pathPoints.size() < 2) return;

        var engine = sceneView.getEngine();
        if (engine == null) return;

        if (sceneView.getMaterialLoader() == null) return;

        MaterialInstance haloMaterial = sceneView.getMaterialLoader().createColorInstance(
                PATH_HALO_COLOR,
                0.0f, 0.18f, 0.98f
        );
        MaterialInstance coreMaterial = sceneView.getMaterialLoader().createColorInstance(
                PATH_CORE_COLOR,
                0.0f, 0.28f, 0.72f
        );
        if (haloMaterial == null || coreMaterial == null) return;

        for (int i = 0; i < pathPoints.size() - 1; i++) {
            float[] a = pathPoints.get(i);
            float[] b = pathPoints.get(i + 1);
            if (a == null || b == null || a.length < 3 || b.length < 3) {
                continue;
            }

            float ax = a[0];
            float ay = a[1] + PATH_OFFSET_Y;
            float az = a[2];
            float bx = b[0];
            float by = b[1] + PATH_OFFSET_Y;
            float bz = b[2];

            float mx = (ax + bx) / 2;
            float my = (ay + by) / 2;
            float mz = (az + bz) / 2;
            float dist = (float) MathUtils.euclideanDistance(ax, ay, az, bx, by, bz);

            float dx = bx - ax;
            float dy = by - ay;
            float dz = bz - az;
            if (dist <= 1e-6f) {
                continue;
            }

            Quaternion q = computeSegmentQuaternion(dx, dy, dz, dist);
            addSegment(engine, mx, my, mz, dist, PATH_HALO_RADIUS, haloMaterial, q);
            addSegment(engine, mx, my, mz, dist, PATH_CORE_RADIUS, coreMaterial, q);
        }
    }

    private void addSegment(com.google.android.filament.Engine engine,
                            float mx,
                            float my,
                            float mz,
                            float dist,
                            float radius,
                            MaterialInstance material,
                            Quaternion quaternion) {
        CylinderNode segment = new CylinderNode(
                engine,
                radius,
                dist,
                new Float3(0, 0, 0),
                24,
                material,
                builder -> Unit.INSTANCE
        );
        segment.setPosition(new Float3(mx, my, mz));
        segment.setQuaternion(quaternion);
        sceneView.addChildNode(segment);
        pathNodes.add(segment);
    }

    /**
     * Computes the quaternion that rotates the cylinder's default Y axis
     * to point along the direction (dx, dy, dz).
     */
    private Quaternion computeSegmentQuaternion(float dx, float dy, float dz, float dist) {
        float ny = dy / dist;

        // Cross product: Y_axis × direction = (dz/dist, 0, -dx/dist)
        float cx = dz / dist;
        float cz = -dx / dist;
        float crossLen = (float) Math.sqrt(cx * cx + cz * cz);

        if (crossLen < 1e-6f) {
            // Direction is (nearly) parallel to Y axis
            return ny > 0
                    ? new Quaternion(0f, 0f, 0f, 1f)
                    : new Quaternion(0f, 0f, 1f, 0f);
        }

        // Normalized rotation axis
        float ax = cx / crossLen;
        float az = cz / crossLen;

        // Angle between Y axis and direction
        float angle = (float) Math.acos(Math.max(-1f, Math.min(1f, ny)));
        float halfAngle = angle / 2f;
        float s = (float) Math.sin(halfAngle);
        float c = (float) Math.cos(halfAngle);

        return new Quaternion(ax * s, 0f, az * s, c);
    }

    public void clearPath() {
        for (io.github.sceneview.node.Node n : pathNodes) {
            sceneView.removeChildNode(n);
        }
        pathNodes.clear();
    }
}
