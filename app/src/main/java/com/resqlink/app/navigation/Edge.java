package com.resqlink.app.navigation;

/**
 * Represents a walkable connection between two nodes in the navigation graph.
 */
public class Edge {
    private final Node from;
    private final Node to;
    private final double cost;

    public Edge(Node from, Node to, double cost) {
        this.from = from;
        this.to = to;
        this.cost = cost;
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
}
