package com.resqlink.app.pathfinding;

import com.resqlink.app.navigation.Graph;
import com.resqlink.app.navigation.Node;
import com.resqlink.app.utils.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* pathfinding on the navigation graph.
 * Uses Euclidean distance heuristic. Returns empty list if no path found.
 */
public class AStarPathfinder {

    private final Graph graph;

    public AStarPathfinder(Graph graph) {
        this.graph = graph;
    }

    /**
     * Finds the optimal path from start to goal.
     *
     * @return Ordered list of nodes from start to goal, or empty list if no path exists
     */
    public List<Node> findPath(Node start, Node goal) {
        if (start == null || goal == null || start.equals(goal)) {
            return start != null && start.equals(goal) ? Collections.singletonList(start) : new ArrayList<>();
        }

        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n ->
                gScore.getOrDefault(n, Double.POSITIVE_INFINITY) + heuristic(n, goal)));
        openSet.add(start);

        Set<Node> closedSet = new HashSet<>();

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (closedSet.contains(current)) continue;
            if (current.equals(goal)) {
                return reconstructPath(cameFrom, current);
            }
            closedSet.add(current);

            for (Node neighbor : graph.getNeighbors(current)) {
                if (closedSet.contains(neighbor)) continue;

                double cost = graph.getEdgeCost(current, neighbor);
                double tentativeG = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + cost;

                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return new ArrayList<>();
    }

    private double heuristic(Node a, Node b) {
        return MathUtils.euclideanDistance(a, b);
    }

    private List<Node> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<Node> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }
}
