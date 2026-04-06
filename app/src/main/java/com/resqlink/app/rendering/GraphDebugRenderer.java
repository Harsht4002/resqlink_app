package com.resqlink.app.rendering;

import com.google.android.filament.MaterialInstance;
import com.resqlink.app.navigation.Edge;
import com.resqlink.app.navigation.Graph;
import com.resqlink.app.navigation.Node;
import com.resqlink.app.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;

import dev.romainguy.kotlin.math.Float3;
import dev.romainguy.kotlin.math.Float4;
import dev.romainguy.kotlin.math.Quaternion;
import io.github.sceneview.SceneView;
import io.github.sceneview.node.CylinderNode;
import kotlin.Unit;

/**
 * Renders the graph geometry in-scene so graph coordinates can be compared against the GLB.
 */
public class GraphDebugRenderer {

    private static final float EDGE_OFFSET_Y = 0.05f;
    private static final float EDGE_RADIUS = 0.03f;
    private static final float ROOM_MARKER_RADIUS = 0.08f;
    private static final float ROOM_MARKER_HEIGHT = 0.26f;
    private static final float HELPER_MARKER_RADIUS = 0.06f;
    private static final float HELPER_MARKER_HEIGHT = 0.18f;
    private static final Float4 EDGE_COLOR = new Float4(0.91f, 0.58f, 0.18f, 0.48f);
    private static final Float4 ROOM_MARKER_COLOR = new Float4(0.09f, 0.65f, 0.79f, 0.95f);
    private static final Float4 HELPER_MARKER_COLOR = new Float4(0.96f, 0.39f, 0.10f, 0.92f);

    private final SceneView sceneView;
    private final List<io.github.sceneview.node.Node> debugNodes = new ArrayList<>();

    public GraphDebugRenderer(SceneView sceneView) {
        this.sceneView = sceneView;
    }

    public void renderGraph(Graph graph) {
        clearGraph();
        if (graph == null) {
            return;
        }

        var engine = sceneView.getEngine();
        if (engine == null || sceneView.getMaterialLoader() == null) {
            return;
        }

        MaterialInstance edgeMaterial = sceneView.getMaterialLoader().createColorInstance(
                EDGE_COLOR,
                0.0f, 0.24f, 0.9f
        );
        MaterialInstance roomMaterial = sceneView.getMaterialLoader().createColorInstance(
                ROOM_MARKER_COLOR,
                0.0f, 0.16f, 0.95f
        );
        MaterialInstance helperMaterial = sceneView.getMaterialLoader().createColorInstance(
                HELPER_MARKER_COLOR,
                0.0f, 0.18f, 0.95f
        );
        if (edgeMaterial == null || roomMaterial == null || helperMaterial == null) {
            return;
        }

        for (Edge edge : graph.getEdges()) {
            renderEdge(engine, edge, edgeMaterial);
        }

        for (Node node : graph.getAllNodes()) {
            boolean helperNode = node.getId().startsWith("_");
            float markerHeight = helperNode ? HELPER_MARKER_HEIGHT : ROOM_MARKER_HEIGHT;
            float markerRadius = helperNode ? HELPER_MARKER_RADIUS : ROOM_MARKER_RADIUS;
            MaterialInstance material = helperNode ? helperMaterial : roomMaterial;
            renderMarker(engine, node, markerHeight, markerRadius, material);
        }
    }

    private void renderEdge(com.google.android.filament.Engine engine,
                            Edge edge,
                            MaterialInstance material) {
        List<float[]> points = new ArrayList<>();
        points.add(edge.getFrom().getRenderPosition());
        points.addAll(edge.getRenderPoints());
        points.add(edge.getTo().getRenderPosition());

        for (int i = 0; i < points.size() - 1; i++) {
            float[] a = points.get(i);
            float[] b = points.get(i + 1);
            renderEdgeSegment(engine, a, b, material);
        }
    }

    private void renderEdgeSegment(com.google.android.filament.Engine engine,
                                   float[] a,
                                   float[] b,
                                   MaterialInstance material) {
        float ax = a[0];
        float ay = a[1] + EDGE_OFFSET_Y;
        float az = a[2];
        float bx = b[0];
        float by = b[1] + EDGE_OFFSET_Y;
        float bz = b[2];

        float dist = (float) MathUtils.euclideanDistance(ax, ay, az, bx, by, bz);
        if (dist <= 1e-6f) {
            return;
        }

        float mx = (ax + bx) / 2f;
        float my = (ay + by) / 2f;
        float mz = (az + bz) / 2f;
        Quaternion q = computeSegmentQuaternion(bx - ax, by - ay, bz - az, dist);

        CylinderNode segment = new CylinderNode(
                engine,
                EDGE_RADIUS,
                dist,
                new Float3(0, 0, 0),
                16,
                material,
                builder -> Unit.INSTANCE
        );
        segment.setPosition(new Float3(mx, my, mz));
        segment.setQuaternion(q);
        sceneView.addChildNode(segment);
        debugNodes.add(segment);
    }

    private void renderMarker(com.google.android.filament.Engine engine,
                              Node node,
                              float markerHeight,
                              float markerRadius,
                              MaterialInstance material) {
        CylinderNode marker = new CylinderNode(
                engine,
                markerRadius,
                markerHeight,
                new Float3(0, 0, 0),
                18,
                material,
                builder -> Unit.INSTANCE
        );
        marker.setPosition(new Float3(
                node.getRenderX(),
                node.getRenderY() + EDGE_OFFSET_Y + (markerHeight / 2f),
                node.getRenderZ()
        ));
        sceneView.addChildNode(marker);
        debugNodes.add(marker);
    }

    private Quaternion computeSegmentQuaternion(float dx, float dy, float dz, float dist) {
        float ny = dy / dist;
        float cx = dz / dist;
        float cz = -dx / dist;
        float crossLen = (float) Math.sqrt(cx * cx + cz * cz);

        if (crossLen < 1e-6f) {
            return ny > 0
                    ? new Quaternion(0f, 0f, 0f, 1f)
                    : new Quaternion(0f, 0f, 1f, 0f);
        }

        float ax = cx / crossLen;
        float az = cz / crossLen;
        float angle = (float) Math.acos(Math.max(-1f, Math.min(1f, ny)));
        float halfAngle = angle / 2f;
        float s = (float) Math.sin(halfAngle);
        float c = (float) Math.cos(halfAngle);

        return new Quaternion(ax * s, 0f, az * s, c);
    }

    public void clearGraph() {
        for (io.github.sceneview.node.Node node : debugNodes) {
            sceneView.removeChildNode(node);
        }
        debugNodes.clear();
    }
}
