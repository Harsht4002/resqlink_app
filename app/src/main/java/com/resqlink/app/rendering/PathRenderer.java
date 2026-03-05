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
    private static final float CYLINDER_RADIUS = 0.08f;

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

        MaterialInstance mat = sceneView.getMaterialLoader() != null
                ? sceneView.getMaterialLoader().createColorInstance(
                        new Float4(0.13f, 0.59f, 1f, 1f),
                        0.5f, 0.5f, 0.4f)
                : (MaterialInstance) null;
        if (mat == null) return;

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

            CylinderNode segment = new CylinderNode(
                    engine,
                    CYLINDER_RADIUS,
                    dist,
                    new Float3(0, 0, 0),
                    24,
                    mat,
                    builder -> Unit.INSTANCE
            );
            segment.setPosition(new Float3(mx, my, mz));

            float dx = bx - ax;
            float dy = by - ay;
            float dz = bz - az;
            if (dist > 1e-6f) {
                float angleYDeg = (float) Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dy / dist))));
                float rx = 0, rz = 0;
                if (Math.abs(dy) < 0.999f * dist) {
                    float axisLen = (float) Math.sqrt(dz * dz + dx * dx);
                    if (axisLen > 1e-6f) {
                        rx = -dz / axisLen;
                        rz = dx / axisLen;
                    }
                } else {
                    rz = 1;
                }
                segment.setRotation(new Float3(rx * angleYDeg, angleYDeg, rz * angleYDeg));
            }
            sceneView.addChildNode(segment);
            pathNodes.add(segment);
        }
    }

    public void clearPath() {
        for (io.github.sceneview.node.Node n : pathNodes) {
            sceneView.removeChildNode(n);
        }
        pathNodes.clear();
    }
}
