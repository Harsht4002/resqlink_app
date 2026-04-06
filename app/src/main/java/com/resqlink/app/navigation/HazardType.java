package com.resqlink.app.navigation;

import dev.romainguy.kotlin.math.Float4;

public enum HazardType {
    FIRE("Fire", new Float4(0.91f, 0.22f, 0.14f, 0.92f), 50.0),
    STRUCTURAL("Structural Damage", new Float4(0.85f, 0.65f, 0.13f, 0.92f), 20.0),
    CHEMICAL("Chemical", new Float4(0.55f, 0.14f, 0.67f, 0.92f), 50.0),
    BLOCKED("Blocked", new Float4(0.35f, 0.35f, 0.35f, 0.92f), 1000.0);

    private final String label;
    private final Float4 color;
    private final double costMultiplier;

    HazardType(String label, Float4 color, double costMultiplier) {
        this.label = label;
        this.color = color;
        this.costMultiplier = costMultiplier;
    }

    public String getLabel() {
        return label;
    }

    public Float4 getColor() {
        return color;
    }

    public double getCostMultiplier() {
        return costMultiplier;
    }

    public static HazardType fromLabel(String label) {
        for (HazardType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        return null;
    }
}
