# How to Build from Source

Build and test PDFDriveReader on your machine.

## Requirements

- JDK 17 or later to run Gradle.
- JDK 25 to compile. The build compiles Java and Kotlin with a JDK 25 toolchain, whichever JDK runs Gradle. If Gradle finds no JDK 25 on your machine, the foojay toolchain resolver downloads one, which needs network access.
- Android SDK with platform 36 (`compileSdk`). The app runs on Android 13 (API 33) and later.
- Android Studio Otter 3 Feature Drop (2025.2.3) or later, the first release that supports AGP 9.0. The bundled JDK can stay the Gradle JDK.

## Build and run the tests

Run unit tests, lint and a debug build:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Run the instrumented tests on a connected device or a running emulator:

```bash
./gradlew connectedCheck
```

## Build without network access

Gradle cannot download JDK 25 offline. Install JDK 25 first. Gradle detects JDKs in the usual install locations. For a JDK in another folder, add its path to `org.gradle.java.installations.paths` in the `gradle.properties` file of your Gradle user home, not in the project's `gradle.properties`:

```properties
org.gradle.java.installations.paths=<path-to-jdk-25>
```
