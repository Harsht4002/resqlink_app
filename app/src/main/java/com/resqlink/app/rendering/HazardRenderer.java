package com.resqlink.app.rendering;

import com.google.android.filament.MaterialInstance;
import com.resqlink.app.navigation.Graph;
import com.resqlink.app.navigation.HazardType;
import com.resqlink.app.navigation.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.romainguy.kotlin.math.Float3;
import io.github.sceneview.SceneView;
import io.github.sceneview.node.CylinderNode;
import kotlin.Unit;

public class HazardRenderer {

    private static final float HAZARD_MARKER_RADIUS = 0.12f;
    private static final float HAZARD_MARKER_HEIGHT = 0.35f;
    private static final float HAZARD_OFFSET_Y = 0.15f;

    private final SceneView sceneView;
    private final List<io.github.sceneview.node.Node> hazardNodes = new ArrayList<>();

    public HazardRenderer(SceneView sceneView) {
        this.sceneView = sceneView;
    }

    public void renderHazards(Graph graph, Map<String, HazardType> hazards) {
        clearHazards();
        if (graph == null || hazards == null || hazards.isEmpty()) {
            return;
        }

        var engine = sceneView.getEngine();
        if (engine == null || sceneView.getMaterialLoader() == null) {
            return;
        }

        Map<HazardType, MaterialInstance> materialCache = new HashMap<>();
        for (HazardType type : HazardType.values()) {
            MaterialInstance mat = sceneView.getMaterialLoader().createColorInstance(
                    type.getColor(), 0.0f, 0.2f, 0.95f);
            if (mat != null) {
                materialCache.put(type, mat);
            }
        }

        for (Map.Entry<String, HazardType> entry : hazards.entrySet()) {
            Node node = graph.getNode(entry.getKey());
            if (node == null) {
                continue;
            }
            MaterialInstance material = materialCache.get(entry.getValue());
            if (material == null) {
                continue;
            }

            CylinderNode marker = new CylinderNode(
                    engine,
                    HAZARD_MARKER_RADIUS,
                    HAZARD_MARKER_HEIGHT,
                    new Float3(0, 0, 0),
                    18,
                    material,
                    builder -> Unit.INSTANCE
            );
            marker.setPosition(new Float3(
                    node.getRenderX(),
                    node.getRenderY() + HAZARD_OFFSET_Y + (HAZARD_MARKER_HEIGHT / 2f),
                    node.getRenderZ()
            ));
            sceneView.addChildNode(marker);
            hazardNodes.add(marker);
        }
    }

    public void clearHazards() {
        for (io.github.sceneview.node.Node node : hazardNodes) {
            sceneView.removeChildNode(node);
        }
        hazardNodes.clear();
    }
}
