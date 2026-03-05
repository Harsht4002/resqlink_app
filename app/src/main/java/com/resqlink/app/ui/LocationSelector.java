package com.resqlink.app.ui;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.resqlink.app.navigation.Graph;

import java.util.List;

/**
 * Manages start and destination spinner dropdowns populated from graph nodes.
 */
public class LocationSelector {

    private final Spinner spinnerStart;
    private final Spinner spinnerDestination;

    private Graph graph;

    public LocationSelector(Spinner spinnerStart, Spinner spinnerDestination) {
        this.spinnerStart = spinnerStart;
        this.spinnerDestination = spinnerDestination;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        refreshOptions();
    }

    public void refreshOptions() {
        if (graph == null) return;

        List<String> ids = graph.getAllNodeIds();
        if (ids.isEmpty()) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                spinnerStart.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                ids
        );
        spinnerStart.setAdapter(adapter);
        spinnerDestination.setAdapter(adapter);

        if (!ids.isEmpty()) {
            spinnerStart.setSelection(0);
            spinnerDestination.setSelection(Math.min(1, ids.size() - 1));
        }
    }

    public String getSelectedStartId() {
        Object item = spinnerStart.getSelectedItem();
        return item != null ? item.toString() : null;
    }

    public String getSelectedEndId() {
        Object item = spinnerDestination.getSelectedItem();
        return item != null ? item.toString() : null;
    }
}
