package com.resqlink.app.rendering;

import android.content.Context;

import kotlin.Unit;
import io.github.sceneview.SceneView;
import io.github.sceneview.node.ModelNode;

/**
 * Loads the 3D building model (GLB) from assets.
 */
public class ModelLoader {

    private final SceneView sceneView;
    private final Context context;

    public ModelLoader(SceneView sceneView, Context context) {
        this.sceneView = sceneView;
        this.context = context;
    }

    /**
     * Loads the building model asynchronously from the given asset path.
     *
     * @param assetPath Path relative to assets folder (e.g. "building.glb")
     * @param listener  Callback when load completes (success or failure)
     */
    public void loadBuildingModel(String assetPath, ModelLoadListener listener) {
        io.github.sceneview.loaders.ModelLoader loader = sceneView.getModelLoader();
        if (loader == null) {
            if (listener != null) listener.onLoadFailed(new IllegalStateException("ModelLoader not available"));
            return;
        }

        loader.loadModelInstanceAsync(assetPath, (String p) -> p, (modelInstance) -> {
            if (modelInstance != null) {
                ModelNode node = new ModelNode(modelInstance, true, null, null);
                sceneView.addChildNode(node);
                if (listener != null) listener.onLoadSuccess(node);
            } else {
                if (listener != null) listener.onLoadFailed(new RuntimeException("Failed to load model"));
            }
            return Unit.INSTANCE;
        });
    }

    public interface ModelLoadListener {
        void onLoadSuccess(ModelNode node);
        void onLoadFailed(Throwable error);
    }
}
