package com.resqlink.app.ble;

import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeaconMappingLoader {
    public List<BeaconMapping> loadMappings(AssetManager assetManager, String assetPath) throws Exception {
        String json = readText(assetManager.open(assetPath));
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.getJSONArray("mappings");
        List<BeaconMapping> mappings = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            mappings.add(new BeaconMapping(
                    obj.getInt("beaconId"),
                    obj.getInt("roomId"),
                    obj.getString("nodeId")
            ));
        }
        return mappings;
    }

    public Map<Integer, BeaconMapping> toBeaconIdMap(List<BeaconMapping> mappings) {
        Map<Integer, BeaconMapping> map = new HashMap<>();
        for (BeaconMapping mapping : mappings) {
            map.put(mapping.getBeaconId(), mapping);
        }
        return map;
    }

    private String readText(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
