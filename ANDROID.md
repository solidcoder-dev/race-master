# Track Checker for Android

This is a separate native Android application built with Kotlin and Jetpack Compose.

## Development

Open the repository root in Android Studio with JDK 17 and Android SDK 35.
Gradle 8.11.1 is the supported build version for Android Gradle Plugin 8.9.2.

To build in a terminal where Gradle 8.11.1 is installed:

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

A Gradle wrapper can be generated via `gradle wrapper --gradle-version 8.11.1`
and committed from a local Android Studio environment. GitHub Actions installs
the same pinned Gradle distribution.

The legacy Maven/Spring Boot source tree (root `src/` and `pom.xml`) remains
unaltered and is **not** a dependency of the Android application. It is kept as a
reference for behavior and sample GPS files.
