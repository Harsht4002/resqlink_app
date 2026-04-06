package com.resqlink.app.navigation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HazardManager {
    private final Map<String, HazardType> hazardNodes = new HashMap<>();

    public void addHazard(String nodeId, HazardType type) {
        hazardNodes.put(nodeId, type);
    }

    public void removeHazard(String nodeId) {
        hazardNodes.remove(nodeId);
    }

    public boolean hasHazard(String nodeId) {
        return hazardNodes.containsKey(nodeId);
    }

    public HazardType getHazard(String nodeId) {
        return hazardNodes.get(nodeId);
    }

    public Map<String, HazardType> getAllHazards() {
        return Collections.unmodifiableMap(hazardNodes);
    }

    public void clear() {
        hazardNodes.clear();
    }

    public double getEdgeCostMultiplier(String nodeIdA, String nodeIdB) {
        HazardType typeA = hazardNodes.get(nodeIdA);
        HazardType typeB = hazardNodes.get(nodeIdB);
        if (typeA == null && typeB == null) {
            return 1.0;
        }
        double multA = typeA != null ? typeA.getCostMultiplier() : 1.0;
        double multB = typeB != null ? typeB.getCostMultiplier() : 1.0;
        return Math.max(multA, multB);
    }
}
