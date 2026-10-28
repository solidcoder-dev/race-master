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

The Android manifest currently disables automatic backup deliberately.
Before production usage, establish a backup/export strategy for user-managed routes.

The legacy Maven/Spring Boot source tree (root `src/` and `pom.xml`) remains
unaltered and is **not** a dependency of the Android application. It is kept as a
reference for behavior and sample GPS files.
