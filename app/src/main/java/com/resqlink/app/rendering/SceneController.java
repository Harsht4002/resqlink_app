package com.resqlink.app.rendering;

import android.content.Context;

import com.resqlink.app.navigation.Node;

import java.util.List;

import io.github.sceneview.SceneView;

/**
 * Manages the 3D scene: initialization, model loading, and path rendering.
 */
public class SceneController {

    private final SceneView sceneView;
    private final ModelLoader modelLoader;
    private final PathRenderer pathRenderer;

    public SceneController(SceneView sceneView, Context context) {
        this.sceneView = sceneView;
        this.modelLoader = new ModelLoader(sceneView, context);
        this.pathRenderer = new PathRenderer(sceneView);
    }

    public void initializeScene() {
    }

    public void loadBuildingModel(String assetPath, ModelLoader.ModelLoadListener listener) {
        modelLoader.loadBuildingModel(assetPath, listener);
    }

    public void renderPath(List<Node> path) {
        pathRenderer.renderPath(path);
    }

    public void clearPath() {
        pathRenderer.clearPath();
    }
}
