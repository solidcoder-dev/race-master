# Track Checker for Android

Native Android application built with Kotlin and Jetpack Compose.

## Development

Open the repository root in Android Studio with JDK 17 and Android SDK 36.
The build uses Android Gradle Plugin 8.13.2 and the committed Gradle 8.13 Wrapper.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows, use `gradlew.bat` instead. CI uses the same Wrapper.
The package namespace and application ID are `org.solidcoder.trackchecker`.

## Map imagery

The route review map uses MapLibre. To enable satellite imagery and shaded
topographic relief, create a MapTiler API key and provide it as a Gradle property
in your personal `~/.gradle/gradle.properties` (outside the repository):

```properties
MAPTILER_API_KEY=your-key-here
```

Alternatively, set the `MAPTILER_API_KEY` environment variable when building.
Without a key, the development-only MapLibre demo style is used and the map
style selector is hidden. Do not distribute the app with the demo style.

MapTiler keys in a mobile APK are visible to users. Before release, use a
dedicated, restricted mobile key, verify the map request User-Agent against
MapTiler's allowed User-Agent policy, and check the provider's quota and terms.

The Android manifest currently disables automatic backup deliberately.
Before production usage, establish a backup/export strategy for user-managed routes.

The legacy Maven/Spring Boot source tree (root `src/` and `pom.xml`) remains
unaltered and is **not** a dependency of the Android application. It is kept as a
reference for behavior and sample GPS files.
