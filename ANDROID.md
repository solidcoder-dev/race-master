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

The route review map uses MapLibre with the OpenFreeMap Liberty vector style,
localized label fallback (`name:es`, then English, then the available local
name), and Mapterhorn Terrarium elevation tiles for hillshade. If the vector
style cannot be loaded, the app falls back to OpenTopoMap raster tiles. Visible
attribution is required for each source. All these endpoints are public hosted
services without an availability guarantee; for production reliability, host
the vector and elevation tiles on infrastructure with an operational owner or
use a provider with explicit service terms. The scale bar is calculated locally.

The Android manifest currently disables automatic backup deliberately.
Before production usage, establish a backup/export strategy for user-managed routes.

The legacy Maven/Spring Boot source tree (root `src/` and `pom.xml`) remains
unaltered and is **not** a dependency of the Android application. It is kept as a
reference for behavior and sample GPS files.
