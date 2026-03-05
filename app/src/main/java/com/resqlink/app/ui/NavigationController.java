package com.resqlink.app.ui;

import android.content.Context;
import android.widget.Toast;

import com.resqlink.app.navigation.Graph;
import com.resqlink.app.navigation.Node;
import com.resqlink.app.pathfinding.AStarPathfinder;
import com.resqlink.app.rendering.SceneController;

import java.util.List;

/**
 * Coordinates user input, pathfinding, and path display.
 */
public class NavigationController {

    private final Graph graph;
    private final AStarPathfinder pathfinder;
    private final SceneController sceneController;
    private final LocationSelector locationSelector;
    private final Context context;

    public NavigationController(Graph graph,
                                AStarPathfinder pathfinder,
                                SceneController sceneController,
                                LocationSelector locationSelector,
                                Context context) {
        this.graph = graph;
        this.pathfinder = pathfinder;
        this.sceneController = sceneController;
        this.locationSelector = locationSelector;
        this.context = context;
    }

    public Node getSelectedStart() {
        String id = locationSelector.getSelectedStartId();
        return id != null ? graph.getNode(id) : null;
    }

    public Node getSelectedEnd() {
        String id = locationSelector.getSelectedEndId();
        return id != null ? graph.getNode(id) : null;
    }

    public List<Node> computePath() {
        Node start = getSelectedStart();
        Node end = getSelectedEnd();
        if (start == null || end == null) return null;
        return pathfinder.findPath(start, end);
    }

    public void displayPath(List<Node> path) {
        if (path != null && !path.isEmpty()) {
            sceneController.renderPath(path);
        } else {
            sceneController.clearPath();
        }
    }

    /**
     * Called when user taps "Find Path". Computes path and displays it, or shows Toast if no path.
     */
    public void onPathRequested() {
        List<Node> path = computePath();
        if (path == null || path.isEmpty()) {
            Toast.makeText(context, "No path found", Toast.LENGTH_LONG).show();
            sceneController.clearPath();
            return;
        }
        displayPath(path);
    }
}
