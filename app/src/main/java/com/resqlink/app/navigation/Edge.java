package com.resqlink.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a walkable connection between two nodes in the navigation graph.
 */
public class Edge {
    private final Node from;
    private final Node to;
    private final double cost;
    private final List<float[]> renderPoints;

    public Edge(Node from, Node to, double cost) {
        this(from, to, cost, new ArrayList<>());
    }

    public Edge(Node from, Node to, double cost, List<float[]> renderPoints) {
        this.from = from;
        this.to = to;
        this.cost = cost;
        this.renderPoints = new ArrayList<>(renderPoints);
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

    public double getCost() {
        return cost;
    }

    public List<float[]> getRenderPoints() {
        return Collections.unmodifiableList(renderPoints);
    }
}
