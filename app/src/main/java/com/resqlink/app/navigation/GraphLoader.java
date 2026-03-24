package com.resqlink.app.navigation;

import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads navigation graph from a JSON file in assets.
 * Node coordinates must match the 3D model's coordinate system.
 */
public class GraphLoader {

    /**
     * Loads a Graph from the given asset path.
     *
     * @param assetManager Android AssetManager
     * @param path         Path relative to assets folder (e.g. "navigation_graph.json")
     * @return The loaded Graph, or null on error
     */
    public Graph loadFromAssets(AssetManager assetManager, String path) {
        try {
            InputStream is = assetManager.open(path);
            String json = readStream(is);
            return parseGraph(json);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private Graph parseGraph(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        Graph graph = new Graph();

        JSONArray nodesArray = root.getJSONArray("nodes");
        for (int i = 0; i < nodesArray.length(); i++) {
            JSONObject n = nodesArray.getJSONObject(i);
            int roomId = n.getInt("roomId");
            String id = n.getString("id");
            float x = (float) n.optDouble("x", 0);
            float y = (float) n.optDouble("y", 0);
            float z = (float) n.optDouble("z", 0);
            int floor = n.optInt("floor", 0);
            Node node = new Node(roomId, id, x, y, z, floor);
            graph.addNode(node);
        }

        JSONArray edgesArray = root.getJSONArray("edges");
        for (int i = 0; i < edgesArray.length(); i++) {
            JSONObject e = edgesArray.getJSONObject(i);
            String fromId = e.getString("from");
            String toId = e.getString("to");
            double cost = e.optDouble("cost", 1.0);
            Node from = graph.getNode(fromId);
            Node to = graph.getNode(toId);
            if (from != null && to != null) {
                graph.addEdge(new Edge(from, to, cost));
            }
        }

        return graph;
    }
}
