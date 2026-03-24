package com.resqlink.app.rendering;


import com.resqlink.app.navigation.Node;
import com.resqlink.app.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;

import com.google.android.filament.MaterialInstance;

import dev.romainguy.kotlin.math.Float3;
import kotlin.Unit;
import dev.romainguy.kotlin.math.Float4;
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
    public void renderPath(List<Node> path) {
        clearPath();
        if (path == null || path.size() < 2) return;

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

        for (int i = 0; i < path.size() - 1; i++) {
            Node a = path.get(i);
            Node b = path.get(i + 1);

            float ax = a.getX();
            float ay = a.getY() + PATH_OFFSET_Y;
            float az = a.getZ();
            float bx = b.getX();
            float by = b.getY() + PATH_OFFSET_Y;
            float bz = b.getZ();

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

            Float3 rotation = computeSegmentRotation(dx, dy, dz, dist);
            addSegment(engine, mx, my, mz, dist, PATH_HALO_RADIUS, haloMaterial, rotation);
            addSegment(engine, mx, my, mz, dist, PATH_CORE_RADIUS, coreMaterial, rotation);
        }
    }

    private void addSegment(com.google.android.filament.Engine engine,
                            float mx,
                            float my,
                            float mz,
                            float dist,
                            float radius,
                            MaterialInstance material,
                            Float3 rotation) {
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
        segment.setRotation(rotation);
        sceneView.addChildNode(segment);
        pathNodes.add(segment);
    }

    private Float3 computeSegmentRotation(float dx, float dy, float dz, float dist) {
        float angleYDeg = (float) Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dy / dist))));
        float rx = 0;
        float rz = 0;
        if (Math.abs(dy) < 0.999f * dist) {
            float axisLen = (float) Math.sqrt(dz * dz + dx * dx);
            if (axisLen > 1e-6f) {
                rx = -dz / axisLen;
                rz = dx / axisLen;
            }
        } else {
            rz = 1;
        }
        return new Float3(rx * angleYDeg, angleYDeg, rz * angleYDeg);
    }

    public void clearPath() {
        for (io.github.sceneview.node.Node n : pathNodes) {
            sceneView.removeChildNode(n);
        }
        pathNodes.clear();
    }
}
