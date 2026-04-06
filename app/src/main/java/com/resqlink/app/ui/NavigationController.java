package com.resqlink.app.ui;

import android.content.Context;
import android.widget.Toast;

import com.resqlink.app.navigation.Edge;
import com.resqlink.app.navigation.Graph;
import com.resqlink.app.navigation.Node;
import com.resqlink.app.pathfinding.AStarPathfinder;
import com.resqlink.app.rendering.SceneController;
import com.resqlink.app.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Coordinates user input, pathfinding, and path display.
 */
public class NavigationController {

    private final Graph graph;
    private final AStarPathfinder pathfinder;
    private final SceneController sceneController;
    private final LocationSelector locationSelector;
    private final Context context;
    private List<Node> activePath = new ArrayList<>();
    private List<Integer> turnIndices = new ArrayList<>();
    private int currentTurnStep = 0;

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
        return computePath(start, end);
    }

    public List<Node> computePath(Node start, Node end) {
        if (start == null || end == null) return null;
        return pathfinder.findPath(start, end);
    }

    public void displayPath(List<Node> path) {
        if (path != null && !path.isEmpty()) {
            sceneController.clearPath();
            sceneController.renderPath(buildRenderPath(path));
        } else {
            sceneController.clearPath();
        }
    }

    public boolean hasActiveRoute() {
        return activePath != null && activePath.size() > 1 && currentTurnStep < turnIndices.size();
    }

    public String getCurrentInstruction() {
        if (!hasActiveRoute()) {
            return "No active route";
        }
        int nodeIndex = turnIndices.get(currentTurnStep);
        Node currentNode = activePath.get(nodeIndex);

        // Last step - arrival.
        if (nodeIndex == activePath.size() - 1) {
            return "Arrive at " + currentNode.getId();
        }

        // Distance to next turn or destination.
        String distInfo = "";
        if (currentTurnStep + 1 < turnIndices.size()) {
            int nextTurnIdx = turnIndices.get(currentTurnStep + 1);
            double dist = computeSegmentDistance(nodeIndex, nextTurnIdx);
            String nextName = activePath.get(nextTurnIdx).getId();
            distInfo = " - walk " + formatDistance(dist) + " to " + nextName;
        }

        // First step - start.
        if (currentTurnStep == 0) {
            return "Start at " + currentNode.getId() + distInfo;
        }

        // Middle steps - turn direction.
        int prevTurnNodeIdx = turnIndices.get(currentTurnStep - 1);
        String direction = computeTurnDirection(prevTurnNodeIdx, nodeIndex);
        return direction + " at " + currentNode.getId() + distInfo;
    }

    public String moveToNextTurn() {
        if (!hasActiveRoute()) {
            return "No active route";
        }

        currentTurnStep++;
        if (currentTurnStep >= turnIndices.size()) {
            sceneController.clearPath();
            return "Route completed";
        }

        int currentNodeIndex = turnIndices.get(currentTurnStep);
        List<Node> remainingPath = activePath.subList(currentNodeIndex, activePath.size());
        displayPath(remainingPath);
        return getCurrentInstruction();
    }

    /**
     * Called when user taps "Find Path". Computes path and displays it, or shows Toast if no path.
     */
    public void onPathRequested() {
        List<Node> path = computePath();
        handlePathResult(path);
    }

    public void onPathRequested(Node start, Node end) {
        List<Node> path = computePath(start, end);
        handlePathResult(path);
    }

    private void handlePathResult(List<Node> path) {
        if (path == null || path.isEmpty()) {
            Toast.makeText(context, "No path found", Toast.LENGTH_LONG).show();
            sceneController.clearPath();
            activePath = new ArrayList<>();
            turnIndices = new ArrayList<>();
            currentTurnStep = 0;
            return;
        }
        activePath = new ArrayList<>(path);
        turnIndices = computeTurnIndices(activePath);
        currentTurnStep = 0;
        displayPath(activePath);
    }

    public int getCurrentTurnStep() {
        return currentTurnStep;
    }

    public int getTotalTurnSteps() {
        return turnIndices.size();
    }

    private double computeSegmentDistance(int fromIndex, int toIndex) {
        double dist = 0;
        for (int i = fromIndex; i < toIndex && i + 1 < activePath.size(); i++) {
            Node from = activePath.get(i);
            Node to = activePath.get(i + 1);
            dist += MathUtils.euclideanDistance(
                    from.getRenderX(), from.getRenderY(), from.getRenderZ(),
                    to.getRenderX(), to.getRenderY(), to.getRenderZ()
            );
        }
        return dist;
    }

    private String formatDistance(double distance) {
        return String.format(Locale.US, "%.1fm", distance);
    }

    private List<Integer> computeTurnIndices(List<Node> path) {
        List<Integer> indices = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return indices;
        }

        indices.add(0);
        for (int i = 1; i < path.size() - 1; i++) {
            Node prev = path.get(i - 1);
            Node curr = path.get(i);
            Node next = path.get(i + 1);

            float v1x = curr.getRenderX() - prev.getRenderX();
            float v1z = curr.getRenderZ() - prev.getRenderZ();
            float v2x = next.getRenderX() - curr.getRenderX();
            float v2z = next.getRenderZ() - curr.getRenderZ();

            double len1 = Math.sqrt(v1x * v1x + v1z * v1z);
            double len2 = Math.sqrt(v2x * v2x + v2z * v2z);
            if (len1 < 1e-5 || len2 < 1e-5) {
                continue;
            }

            double n1x = v1x / len1;
            double n1z = v1z / len1;
            double n2x = v2x / len2;
            double n2z = v2z / len2;
            double dot = n1x * n2x + n1z * n2z;
            dot = Math.max(-1.0, Math.min(1.0, dot));
            double angle = Math.toDegrees(Math.acos(dot));
            if (angle >= 20.0) {
                indices.add(i);
            }
        }
        indices.add(path.size() - 1);
        return indices;
    }

private List<float[]> buildRenderPath(List<Node> path) {
    List<float[]> points = new ArrayList<>();
    if (path == null || path.size() < 2) return points;

    for (int i = 0; i < path.size() - 1; i++) {
        Node from = path.get(i);
        Node to = path.get(i + 1);

        // ALWAYS connect directly node → node
        points.add(from.getRenderPosition());
        points.add(to.getRenderPosition());
    }

    return points;
}
    private void appendEdgePoints(List<float[]> points, Edge edge, Node from, Node to) {
        List<float[]> renderPoints = edge.getRenderPoints();
        if (renderPoints.isEmpty()) {
            return;
        }

        boolean forward = edge.getFrom().equals(from) && edge.getTo().equals(to);
        if (forward) {
            for (float[] point : renderPoints) {
                appendPoint(points, point);
            }
        } else {
            for (int i = renderPoints.size() - 1; i >= 0; i--) {
                appendPoint(points, renderPoints.get(i));
            }
        }
    }

    private void appendPoint(List<float[]> points, float[] candidate) {
        if (candidate == null || candidate.length < 3) {
            return;
        }
        if (points.isEmpty()) {
            points.add(copyPoint(candidate));
            return;
        }

        float[] last = points.get(points.size() - 1);
        double dist = MathUtils.euclideanDistance(
                last[0], last[1], last[2],
                candidate[0], candidate[1], candidate[2]
        );
        if (dist > 1e-5) {
            points.add(copyPoint(candidate));
        }
    }

    private float[] copyPoint(float[] point) {
        return new float[]{point[0], point[1], point[2]};
    }

    private String computeTurnDirection(int fromTurnNodeIndex, int toTurnNodeIndex) {
        if (toTurnNodeIndex + 1 >= activePath.size() || fromTurnNodeIndex >= activePath.size()) {
            return "Continue";
        }

        Node from = activePath.get(fromTurnNodeIndex);
        Node at = activePath.get(toTurnNodeIndex);
        Node next = activePath.get(toTurnNodeIndex + 1);

        float inX = at.getRenderX() - from.getRenderX();
        float inZ = at.getRenderZ() - from.getRenderZ();
        float outX = next.getRenderX() - at.getRenderX();
        float outZ = next.getRenderZ() - at.getRenderZ();

        double inLen = Math.sqrt(inX * inX + inZ * inZ);
        double outLen = Math.sqrt(outX * outX + outZ * outZ);
        if (inLen < 1e-5 || outLen < 1e-5) {
            return "Continue";
        }

        double nInX = inX / inLen;
        double nInZ = inZ / inLen;
        double nOutX = outX / outLen;
        double nOutZ = outZ / outLen;
        double cross = nInX * nOutZ - nInZ * nOutX;
        double dot = nInX * nOutX + nInZ * nOutZ;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angle = Math.toDegrees(Math.acos(dot));

        if (angle < 20.0) {
            return "Continue straight";
        } else if (cross > 0) {
            return angle > 100 ? "Take a sharp left" : "Turn left";
        } else {
            return angle > 100 ? "Take a sharp right" : "Turn right";
        }
    }
}
