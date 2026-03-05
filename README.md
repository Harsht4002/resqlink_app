# ResQLink

Indoor navigation application for Android. Loads a 3D building model, uses a JSON navigation graph, computes paths with A*, and renders the path in the 3D scene.

## Requirements

- Android minSdk 26
- Java 11
- SceneView (Filament-based) for 3D rendering

## Setup

1. Open the project in Android Studio
2. Ensure `local.properties` has correct `sdk.dir`
3. Build: `./gradlew assembleDebug`

## Project Structure

- `activities/` - MainActivity
- `navigation/` - Node, Edge, Graph, GraphLoader
- `pathfinding/` - AStarPathfinder
- `rendering/` - ModelLoader, PathRenderer, SceneController
- `ui/` - LocationSelector, NavigationController
- `utils/` - MathUtils
- `assets/` - building.glb, navigation_graph.json

## Usage

1. Enter JSON path (default: `navigation_graph.json`) and 3D model path (default: `building.glb`)
2. Tap "Load" to load the graph and model
3. Select start and destination from dropdowns
4. Tap "Find Path" to compute and display the path

## Important Notes

- **Node coordinates** must match the 3D model coordinate system
- **Path height**: Rendered at `y = node.y + 0.1f` to avoid floor clipping
- **GLB size**: Keep under 200k triangles for mobile
- **Pathfinding** operates only on the graph, never on the mesh
- **SceneView lifecycle**: SceneView 2.3.x is lifecycle-aware. If you experience leaks or crashes, check the [SceneView docs](https://sceneview.github.io/) for manual resume/pause/destroy handling.
