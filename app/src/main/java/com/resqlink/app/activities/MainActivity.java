package com.resqlink.app.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.resqlink.app.R;
import com.resqlink.app.ble.BeaconMapping;
import com.resqlink.app.ble.BeaconMappingLoader;
import com.resqlink.app.ble.BlePacketCodec;
import com.resqlink.app.ble.BleScannerManager;
import com.resqlink.app.ble.VictimBroadcaster;
import com.resqlink.app.navigation.Graph;
import com.resqlink.app.navigation.GraphLoader;
import com.resqlink.app.navigation.Node;
import com.resqlink.app.pathfinding.AStarPathfinder;
import com.resqlink.app.rendering.ModelLoader;
import com.resqlink.app.rendering.SceneController;
import com.resqlink.app.ui.LocationSelector;
import com.resqlink.app.ui.NavigationController;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.github.sceneview.SceneView;

public class MainActivity extends AppCompatActivity implements BleScannerManager.Listener, VictimBroadcaster.Listener {

    private static final int BLE_PERMISSION_REQ_CODE = 1001;
    private static final int ENABLE_BLUETOOTH_REQ_CODE = 1002;
    private static final int NETWORK_ID = 0x0001;
    private static final int PENDING_BLE_ACTION_NONE = 0;
    private static final int PENDING_BLE_ACTION_SCAN = 1;
    private static final int PENDING_BLE_ACTION_BROADCAST = 2;

    private SceneView sceneView;
    private EditText etJsonPath;
    private EditText etModelPath;
    private Spinner spinnerStart;
    private Spinner spinnerDestination;
    private Spinner spinnerVictims;
    private Spinner spinnerVictimLocation;
    private Button btnLoad;
    private Button btnFindPath;
    private Button btnNextTurn;
    private Button btnNavigateVictim;
    private Button btnStartBleScan;
    private Button btnBroadcastVictim;
    private TextView tvRouteStatus;
    private TextView tvTurnInstruction;
    private TextView tvDetectedRoom;
    private Button btnTogglePanel;
    private View panelInput;
    private View panelPrimaryControls;
    private View panelScroll;
    private BottomSheetBehavior<View> panelBehavior;

    private Graph graph;
    private SceneController sceneController;
    private LocationSelector locationSelector;
    private NavigationController navigationController;
    private BleScannerManager bleScannerManager;
    private VictimBroadcaster victimBroadcaster;
    private boolean isBleScanning = false;
    private boolean wasUserBroadcasting = false;
    private int currentDetectedRoomId = -1;
    private int pendingBleAction = PENDING_BLE_ACTION_NONE;

    private final Map<Integer, BeaconMapping> beaconMap = new HashMap<>();
    private final Map<Integer, BlePacketCodec.DeviceLocationPacket> victimsByDeviceId = new HashMap<>();
    private final List<String> victimSpinnerItems = new ArrayList<>();
    private ArrayAdapter<String> victimAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sceneView = findViewById(R.id.sceneView);
        etJsonPath = findViewById(R.id.etJsonPath);
        etModelPath = findViewById(R.id.etModelPath);
        btnLoad = findViewById(R.id.btnLoad);
        spinnerStart = findViewById(R.id.spinnerStart);
        spinnerDestination = findViewById(R.id.spinnerDestination);
        spinnerVictims = findViewById(R.id.spinnerVictims);
        spinnerVictimLocation = findViewById(R.id.spinnerVictimLocation);
        btnFindPath = findViewById(R.id.btnFindPath);
        btnNextTurn = findViewById(R.id.btnNextTurn);
        btnNavigateVictim = findViewById(R.id.btnNavigateVictim);
        btnStartBleScan = findViewById(R.id.btnStartBleScan);
        btnBroadcastVictim = findViewById(R.id.btnBroadcastVictim);
        tvRouteStatus = findViewById(R.id.tvRouteStatus);
        tvTurnInstruction = findViewById(R.id.tvTurnInstruction);
        tvDetectedRoom = findViewById(R.id.tvDetectedRoom);
        btnTogglePanel = findViewById(R.id.btnTogglePanel);
        panelInput = findViewById(R.id.panelInput);
        panelPrimaryControls = findViewById(R.id.panelPrimaryControls);
        panelScroll = findViewById(R.id.panelScroll);

        panelBehavior = BottomSheetBehavior.from(panelInput);
        panelBehavior.setFitToContents(true);
        panelBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        panelBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@androidx.annotation.NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    btnTogglePanel.setText(R.string.panel_collapse);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    btnTogglePanel.setText(R.string.panel_expand);
                }
            }

            @Override
            public void onSlide(@androidx.annotation.NonNull View bottomSheet, float slideOffset) {
            }
        });
        configureBottomSheet();

        sceneController = new SceneController(sceneView, this);
        sceneController.initializeScene();

        locationSelector = new LocationSelector(spinnerStart, spinnerDestination);
        victimAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, victimSpinnerItems);
        spinnerVictims.setAdapter(victimAdapter);
        victimSpinnerItems.add("No victims detected");
        victimAdapter.notifyDataSetChanged();

        spinnerVictimLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!wasUserBroadcasting || graph == null) return;
                String nodeId = (String) parent.getItemAtPosition(position);
                Node node = graph.getNode(nodeId);
                if (node != null) {
                    victimBroadcaster.startBroadcast(node.getRoomId(), node.getFloor());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bleScannerManager = new BleScannerManager(this, NETWORK_ID, this);
        victimBroadcaster = new VictimBroadcaster(this, NETWORK_ID, new Random().nextInt(65535), this);

        btnLoad.setOnClickListener(v -> loadGraphAndModel());
        btnFindPath.setOnClickListener(v -> {
            if (navigationController != null) {
                navigationController.onPathRequested();
                updateRouteUiAfterFind();
            } else {
                Toast.makeText(this, getString(R.string.load_graph_model_first), Toast.LENGTH_SHORT).show();
            }
        });
        btnNextTurn.setOnClickListener(v -> onNextTurnClicked());
        btnStartBleScan.setOnClickListener(v -> toggleBleScan());
        btnBroadcastVictim.setOnClickListener(v -> toggleVictimBroadcast());
        btnNavigateVictim.setOnClickListener(v -> onNavigateToVictim());
        btnTogglePanel.setOnClickListener(v -> togglePanel());

        loadGraphAndModel();
        ensureBlePermissions();
    }

    private void togglePanel() {
        if (panelBehavior == null) return;
        int state = panelBehavior.getState();
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            panelBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            panelBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void configureBottomSheet() {
        panelInput.post(() -> {
            int peekHeight = measurePeekHeight();
            if (peekHeight > 0) {
                panelBehavior.setPeekHeight(peekHeight, true);
            }
            applyExpandedScrollLimit();
        });
    }

    private int measurePeekHeight() {
        int width = panelInput.getWidth();
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        panelPrimaryControls.measure(widthSpec, heightSpec);
        View header = findViewById(R.id.panelHeader);
        header.measure(widthSpec, heightSpec);
        return header.getMeasuredHeight() + panelPrimaryControls.getMeasuredHeight();
    }

    private void applyExpandedScrollLimit() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int maxScrollHeight = (int) (metrics.heightPixels * 0.42f);
        ViewGroup.LayoutParams params = panelScroll.getLayoutParams();
        params.height = maxScrollHeight;
        panelScroll.setLayoutParams(params);
    }

    private void loadGraphAndModel() {
        String jsonPath = etJsonPath.getText().toString().trim();
        if (jsonPath.isEmpty()) jsonPath = "navigation_graph.json";

        String modelPath = etModelPath.getText().toString().trim();
        if (modelPath.isEmpty()) modelPath = "building.glb";

        graph = new GraphLoader().loadFromAssets(getAssets(), jsonPath);
        if (graph == null) {
            Toast.makeText(this, "Failed to load graph from " + jsonPath, Toast.LENGTH_SHORT).show();
            return;
        }

        locationSelector.setGraph(graph);
        List<String> nodeIds = graph.getAllNodeIds();
        ArrayAdapter<String> victimLocAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, nodeIds);
        spinnerVictimLocation.setAdapter(victimLocAdapter);
        loadBeaconMappings();
        navigationController = new NavigationController(
                graph,
                new AStarPathfinder(graph),
                sceneController,
                locationSelector,
                this
        );

        sceneController.clearPath();
        tvRouteStatus.setText(getString(R.string.route_idle));
        tvTurnInstruction.setText(getString(R.string.turn_instruction_placeholder));
        sceneController.loadBuildingModel(modelPath, new ModelLoader.ModelLoadListener() {
            @Override
            public void onLoadSuccess(io.github.sceneview.node.ModelNode node) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Model loaded", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onLoadFailed(Throwable error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Model load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBleScanning) {
            bleScannerManager.start();
        }
        if (wasUserBroadcasting) {
            victimBroadcaster.resumeBroadcast();
        }
    }

    @Override
    protected void onPause() {
        bleScannerManager.stop();
        victimBroadcaster.stopBroadcast();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bleScannerManager.stop();
        victimBroadcaster.stopBroadcast();
        super.onDestroy();
    }

    private void updateRouteUiAfterFind() {
        if (navigationController == null || !navigationController.hasActiveRoute()) {
            tvRouteStatus.setText(getString(R.string.route_idle));
            tvTurnInstruction.setText(getString(R.string.turn_instruction_placeholder));
            return;
        }

        tvRouteStatus.setText(
                getString(
                        R.string.route_step_format,
                        navigationController.getCurrentTurnStep() + 1,
                        navigationController.getTotalTurnSteps()
                )
        );
        tvTurnInstruction.setText(navigationController.getCurrentInstruction());
    }

    private void onNextTurnClicked() {
        if (navigationController == null || !navigationController.hasActiveRoute()) {
            Toast.makeText(this, getString(R.string.route_idle), Toast.LENGTH_SHORT).show();
            return;
        }
        String instruction = navigationController.moveToNextTurn();
        if (!navigationController.hasActiveRoute()) {
            tvRouteStatus.setText(getString(R.string.route_completed));
            tvTurnInstruction.setText(instruction);
            return;
        }
        tvRouteStatus.setText(
                getString(
                        R.string.route_step_format,
                        navigationController.getCurrentTurnStep() + 1,
                        navigationController.getTotalTurnSteps()
                )
        );
        tvTurnInstruction.setText(instruction);
    }

    private void loadBeaconMappings() {
        beaconMap.clear();
        try {
            BeaconMappingLoader loader = new BeaconMappingLoader();
            List<BeaconMapping> mappings = loader.loadMappings(getAssets(), "beacon_mappings.json");
            beaconMap.putAll(loader.toBeaconIdMap(mappings));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load beacon mappings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleBleScan() {
        if (!hasBlePermissions()) {
            ensureBlePermissions();
            return;
        }
        if (!ensureBluetoothEnabled(PENDING_BLE_ACTION_SCAN)) {
            return;
        }
        if (isBleScanning) {
            bleScannerManager.stop();
            isBleScanning = false;
            btnStartBleScan.setText(R.string.start_ble_scan);
        } else {
            bleScannerManager.start();
            isBleScanning = true;
            btnStartBleScan.setText(R.string.stop_ble_scan);
        }
    }

    private void toggleVictimBroadcast() {
        if (!hasBlePermissions()) {
            ensureBlePermissions();
            return;
        }
        if (!ensureBluetoothEnabled(PENDING_BLE_ACTION_BROADCAST)) {
            return;
        }
        if (victimBroadcaster.isBroadcasting() || wasUserBroadcasting) {
            victimBroadcaster.stopBroadcast();
            wasUserBroadcasting = false;
            btnBroadcastVictim.setText(R.string.start_victim_broadcast);
        } else {
            String selectedNodeId = (String) spinnerVictimLocation.getSelectedItem();
            Node selectedNode = selectedNodeId != null && graph != null
                    ? graph.getNode(selectedNodeId) : null;
            if (selectedNode == null) {
                Toast.makeText(this, "Select a broadcast location first", Toast.LENGTH_SHORT).show();
                return;
            }
            wasUserBroadcasting = true;
            victimBroadcaster.startBroadcast(selectedNode.getRoomId(), selectedNode.getFloor());
            btnBroadcastVictim.setText(R.string.stop_victim_broadcast);
        }
    }

    private boolean ensureBluetoothEnabled(int requestedAction) {
        BluetoothAdapter adapter = getBluetoothAdapter();
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth unsupported on this device", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (adapter.isEnabled()) {
            return true;
        }
        pendingBleAction = requestedAction;
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, ENABLE_BLUETOOTH_REQ_CODE);
        return false;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        return bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }

    private void onNavigateToVictim() {
        if (navigationController == null || graph == null) {
            return;
        }
        if (victimsByDeviceId.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_victim_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        String selected = (String) spinnerVictims.getSelectedItem();
        if (selected == null || !selected.startsWith("Victim")) {
            Toast.makeText(this, getString(R.string.no_victim_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        int deviceId = parseDeviceId(selected);
        BlePacketCodec.DeviceLocationPacket packet = victimsByDeviceId.get(deviceId);
        if (packet == null) {
            Toast.makeText(this, getString(R.string.no_victim_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        Node startNode = graph.getNodeByRoomId(currentDetectedRoomId);
        if (startNode == null) {
            startNode = navigationController.getSelectedStart();
        }
        Node victimNode = graph.getNodeByRoomId(packet.roomId);
        if (startNode == null || victimNode == null) {
            Toast.makeText(this, "Missing start/victim node", Toast.LENGTH_SHORT).show();
            return;
        }
        navigationController.onPathRequested(startNode, victimNode);
        updateRouteUiAfterFind();
    }

    private int parseDeviceId(String item) {
        try {
            int start = item.indexOf('#');
            int end = item.indexOf(' ');
            if (start == -1) return -1;
            if (end == -1) end = item.length();
            return Integer.parseInt(item.substring(start + 1, end));
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureBlePermissions() {
        if (hasBlePermissions()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT},
                    BLE_PERMISSION_REQ_CODE
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    BLE_PERMISSION_REQ_CODE
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != ENABLE_BLUETOOTH_REQ_CODE) {
            return;
        }

        BluetoothAdapter adapter = getBluetoothAdapter();
        boolean enabled = adapter != null && adapter.isEnabled();
        int action = pendingBleAction;
        pendingBleAction = PENDING_BLE_ACTION_NONE;

        if (!enabled) {
            Toast.makeText(this, "Bluetooth is required for BLE features", Toast.LENGTH_SHORT).show();
            return;
        }

        if (action == PENDING_BLE_ACTION_SCAN) {
            toggleBleScan();
        } else if (action == PENDING_BLE_ACTION_BROADCAST) {
            toggleVictimBroadcast();
        }
    }

    @Override
    public void onDetectedRoom(int roomId, int beaconId) {
        BeaconMapping mapping = beaconMap.get(beaconId);
        if (mapping != null) {
            roomId = mapping.getRoomId();
        }
        currentDetectedRoomId = roomId;
        Node node = graph != null ? graph.getNodeByRoomId(roomId) : null;
        String roomText = node != null ? node.getId() : ("roomId " + roomId);
        runOnUiThread(() -> tvDetectedRoom.setText("Detected room: " + roomText + " (beacon " + beaconId + ")"));
    }

    @Override
    public void onVictimLocation(BlePacketCodec.DeviceLocationPacket packet) {
        victimsByDeviceId.put(packet.deviceId, packet);
        runOnUiThread(this::refreshVictimSpinner);
    }

    private void refreshVictimSpinner() {
        victimSpinnerItems.clear();
        if (victimsByDeviceId.isEmpty()) {
            victimSpinnerItems.add("No victims detected");
        } else {
            for (BlePacketCodec.DeviceLocationPacket packet : victimsByDeviceId.values()) {
                Node node = graph != null ? graph.getNodeByRoomId(packet.roomId) : null;
                String roomLabel = node != null ? node.getId() : ("roomId " + packet.roomId);
                victimSpinnerItems.add("Victim#" + packet.deviceId + " - " + roomLabel + " - seq " + packet.seq);
            }
        }
        victimAdapter.notifyDataSetChanged();
    }

    @Override
    public void onScanError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onStatus(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
