# Track Checker for Android

Native app: Kotlin + Jetpack Compose, DDD by feature under
`app/src/main/kotlin/org/picolobruno/trackchecker/routes/{domain,application,infrastructure,ui}`.

## Requirements

- Android Studio and SDK 35
- JDK 17
- Gradle 8.11.1 (GitHub CI pins this distribution)

Open the repository root in Android Studio. With a local Gradle installation:

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

To generate a Gradle wrapper locally, run `gradle wrapper --gradle-version 8.11.1`
then commit the wrapper scripts and JAR.

## First iteration

1. Open **Mis rutas** and import KML, GPX or KMZ using Android's document picker.
2. A newly imported, independent route is persisted immediately as **Revisar**.
3. Tap a route to inspect its map and list; edit names and point types.
4. Validate once it has a single start and finish. Duplicate names warn but do not block.
5. Rename/delete routes through the contextual menu.

Every point has an independent internal ID. Editing a name never modifies the
original KML/GPX/KMZ. Invalid imports leave existing routes untouched.

The first map implementation uses **MapLibre demo tiles**, which are suitable
for development only. Configure an appropriate tile provider and offline
strategy before operational deployment, especially in areas without connectivity.

## Legacy code and test fixtures

The Maven/Spring Boot source tree (root `src/` and `pom.xml`) is not compiled
into the Android app. It remains intact as a historical implementation reference.

The copied `ET 1 WP NOMAD 2024.kml` fixture tests real KML extraction and
guards against incorrectly assuming first/last Placemarks are start/finish.

## Limits

- Score calculation and track import are intentionally **not part** of this iteration.
- Renaming or reclassifying a point changes a READY route back to DRAFT.
- Work remains local to the device; no account or cloud synchronization yet.
- Deleting a route is permanent in this MVP; confirm carefully.
