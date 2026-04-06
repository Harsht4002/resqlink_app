package com.resqlink.app.rendering;

import android.content.Context;

import com.resqlink.app.navigation.Graph;

import java.util.List;

import io.github.sceneview.SceneView;

/**
 * Manages the 3D scene: initialization, model loading, and path rendering.
 */
public class SceneController {

    private final SceneView sceneView;
    private final ModelLoader modelLoader;
    private final PathRenderer pathRenderer;
    private final GraphDebugRenderer graphDebugRenderer;

    public SceneController(SceneView sceneView, Context context) {
        this.sceneView = sceneView;
        this.modelLoader = new ModelLoader(sceneView, context);
        this.pathRenderer = new PathRenderer(sceneView);
        this.graphDebugRenderer = new GraphDebugRenderer(sceneView);
    }

    public void initializeScene() {
    }

    public void loadBuildingModel(String assetPath, ModelLoader.ModelLoadListener listener) {
        modelLoader.loadBuildingModel(assetPath, listener);
    }

    public void renderPath(List<float[]> pathPoints) {
        pathRenderer.renderPath(pathPoints);
    }

    public void clearPath() {
        pathRenderer.clearPath();
    }

    public void renderGraphDebug(Graph graph) {
        graphDebugRenderer.renderGraph(graph);
    }

    public void clearGraphDebug() {
        graphDebugRenderer.clearGraph();
    }
}
