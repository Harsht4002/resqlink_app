package com.resqlink.app.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Holds the navigation graph. Pathfinding operates only on this graph, never on the 3D mesh.
 */
public class Graph {
    private final Map<String, Node> nodes;
    private final Map<Integer, Node> nodesByRoomId;
    private final List<Edge> edges;
    private final Map<String, Map<String, Double>> edgeCostMap;
    private final Map<String, Map<String, Edge>> edgeMap;

    public Graph() {
        this.nodes = new HashMap<>();
        this.nodesByRoomId = new HashMap<>();
        this.edges = new ArrayList<>();
        this.edgeCostMap = new HashMap<>();
        this.edgeMap = new HashMap<>();
    }

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        nodesByRoomId.put(node.getRoomId(), node);
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
        edge.getFrom().addNeighbor(edge.getTo().getId());
        edge.getTo().addNeighbor(edge.getFrom().getId());
        edgeCostMap
                .computeIfAbsent(edge.getFrom().getId(), k -> new HashMap<>())
                .put(edge.getTo().getId(), edge.getCost());
        edgeCostMap
                .computeIfAbsent(edge.getTo().getId(), k -> new HashMap<>())
                .put(edge.getFrom().getId(), edge.getCost());
        edgeMap
                .computeIfAbsent(edge.getFrom().getId(), k -> new HashMap<>())
                .put(edge.getTo().getId(), edge);
        edgeMap
                .computeIfAbsent(edge.getTo().getId(), k -> new HashMap<>())
                .put(edge.getFrom().getId(), edge);
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public List<Node> getAllNodes() {
        List<Node> sortedNodes = new ArrayList<>(nodes.values());
        sortedNodes.sort(Comparator.comparingInt(Node::getRoomId).thenComparing(Node::getId));
        return sortedNodes;
    }

    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        for (String neighborId : node.getNeighborIds()) {
            Node neighbor = nodes.get(neighborId);
            if (neighbor != null) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    public List<String> getAllNodeIds() {
        List<Node> sortedNodes = new ArrayList<>(nodes.values());
        sortedNodes.sort(Comparator.comparingInt(Node::getRoomId).thenComparing(Node::getId));

        List<String> ids = new ArrayList<>(sortedNodes.size());
        for (Node node : sortedNodes) {
            ids.add(node.getId());
        }
        return ids;
    }

    public List<String> getSelectableNodeIds() {
        List<Node> sortedNodes = new ArrayList<>(nodes.values());
        sortedNodes.sort(Comparator.comparingInt(Node::getRoomId).thenComparing(Node::getId));

        List<String> ids = new ArrayList<>();
        for (Node node : sortedNodes) {
            if (!node.getId().startsWith("_")) {
                ids.add(node.getId());
            }
        }
        return ids;
    }

    public Node getNodeByRoomId(int roomId) {
        return nodesByRoomId.get(roomId);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public double getEdgeCost(Node a, Node b) {
        Map<String, Double> costs = edgeCostMap.get(a.getId());
        if (costs == null) return Double.POSITIVE_INFINITY;
        Double cost = costs.get(b.getId());
        return cost != null ? cost : Double.POSITIVE_INFINITY;
    }

    public Edge getEdge(Node a, Node b) {
        if (a == null || b == null) {
            return null;
        }
        Map<String, Edge> row = edgeMap.get(a.getId());
        return row != null ? row.get(b.getId()) : null;
    }
}
