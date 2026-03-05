package com.resqlink.app.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.resqlink.app.R;
import com.resqlink.app.navigation.Graph;
import com.resqlink.app.navigation.GraphLoader;
import com.resqlink.app.pathfinding.AStarPathfinder;
import com.resqlink.app.rendering.ModelLoader;
import com.resqlink.app.rendering.SceneController;
import com.resqlink.app.ui.LocationSelector;
import com.resqlink.app.ui.NavigationController;

import io.github.sceneview.SceneView;

public class MainActivity extends AppCompatActivity {

    private SceneView sceneView;
    private EditText etJsonPath;
    private EditText etModelPath;
    private Spinner spinnerStart;
    private Spinner spinnerDestination;
    private Button btnLoad;
    private Button btnFindPath;

    private Graph graph;
    private SceneController sceneController;
    private LocationSelector locationSelector;
    private NavigationController navigationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sceneView = findViewById(R.id.sceneView);
        etJsonPath = findViewById(R.id.etJsonPath);
        etModelPath = findViewById(R.id.etModelPath);
        btnLoad = findViewById(R.id.btnLoad);
        spinnerStart = findViewById(R.id.spinnerStart);
        spinnerDestination = findViewById(R.id.spinnerDestination);
        btnFindPath = findViewById(R.id.btnFindPath);

        sceneController = new SceneController(sceneView, this);
        sceneController.initializeScene();

        locationSelector = new LocationSelector(spinnerStart, spinnerDestination);

        btnLoad.setOnClickListener(v -> loadGraphAndModel());
        btnFindPath.setOnClickListener(v -> {
            if (navigationController != null) {
                navigationController.onPathRequested();
            } else {
                Toast.makeText(this, "Load graph and model first", Toast.LENGTH_SHORT).show();
            }
        });

        loadGraphAndModel();
    }

    private void loadGraphAndModel() {
        String jsonPath = etJsonPath.getText().toString().trim();
        if (jsonPath.isEmpty()) jsonPath = "navigation_graph.json";

        String modelPath = etModelPath.getText().toString().trim();
        if (modelPath.isEmpty()) modelPath = "building.glb";

        graph = new GraphLoader().loadFromAssets(getAssets(), jsonPath);
        if (graph == null) {
            Toast.makeText(this, "Failed to load graph from " + jsonPath, Toast.LENGTH_SHORT).show();
            return;
        }

        locationSelector.setGraph(graph);
        navigationController = new NavigationController(
                graph,
                new AStarPathfinder(graph),
                sceneController,
                locationSelector,
                this
        );

        sceneController.clearPath();
        sceneController.loadBuildingModel(modelPath, new ModelLoader.ModelLoadListener() {
            @Override
            public void onLoadSuccess(io.github.sceneview.node.ModelNode node) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Model loaded", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onLoadFailed(Throwable error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Model load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
