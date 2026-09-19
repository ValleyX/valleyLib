# Installation

ValleyLib is published through [JitPack](https://jitpack.io/#ValleyX/valleyLib) and is consumed from your FTC **TeamCode** project like any other Gradle dependency.

## 1. Add the JitPack repository

In your FTC project, open the top-level `build.gradle` (or `settings.gradle`, depending on how your project resolves repositories) and add JitPack:

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

If you plan to use the Panels telemetry dashboard, also add its repository (ValleyLib's FTC module links against it):

```gradle
repositories {
    maven { url = uri("https://mymaven.bylazar.com/releases") }
}
```

Pedro Pathing 3 publishes to Maven Central, so it needs no extra repository.

## 2. Add the dependencies

In `TeamCode/build.gradle`:

```gradle
dependencies {
    implementation 'com.github.ValleyX.valleyLib:core:<version>'
    implementation 'com.github.ValleyX.valleyLib:ftc:<version>'
}
```

Replace `<version>` with a release tag from the table below.

!!! tip
    `valleyLib-ftc` already depends on `valleyLib-core`, but declaring both explicitly makes desktop unit testing of your command logic easier.

## Version compatibility

| Version | Usable?           |
| ------- | ----------------- |
| 1.0.0   | No                |
| 1.0.1   | No                |
| 1.0.2   | Yes (deprecated)  |
| 1.0.3   | No                |
| 1.0.4   | No                |
| 1.0.5   | No                |
| 1.0.6   | Yes               |
| 1.0.7   | Yes               |
| 1.0.8   | Yes (Pedro Pathing 2.x) |
| 2.0.0   | **Yes (recommended)** — Pedro Pathing 3.x |

!!! warning
    Versions marked "No" fail to resolve or contain packaging defects. Always use the latest recommended release.

## Requirements

| Requirement | Value |
| ----------- | ----- |
| Java language level | 11 (built with JDK 17) |
| Android `minSdk` | 24 |
| FTC SDK | Current season SDK (`RobotCore`, `Hardware`, `RobotServer` are `compileOnly` — your TeamCode project provides them) |
| Pedro Pathing | **3.x** (`com.pedropathing:core` + `com.pedropathing:revhub`, pulled in transitively). ValleyLib 1.0.x targets Pedro 2.x instead. |
| Optional | Panels (for dashboard telemetry) |

## What each artifact contains

- **`core`** — pure-Java command framework. No Android or FTC SDK dependencies, so you can also add it to plain JVM test modules.
- **`ftc`** — Android library (AAR) with the FTC integrations. It pulls in Panels telemetry and the Pedro Pathing 3 artifacts (`core`, `revhub`) as `api` dependencies.

Pedro's tuning dashboard (`com.pedropathing:tuning`) is **not** pulled in — it carries its own web server and other runtime dependencies. Add it to your TeamCode project directly if you want it.

## Building ValleyLib from source

Clone the repo and publish to your local Maven repository:

```bash
git clone https://github.com/ValleyX/valleyLib.git
cd valleyLib
./gradlew :valleyLib-core:publishToMavenLocal :valleyLib-ftc:publishToMavenLocal
```

Then depend on `com.vcs.valleylib:core:2.0.0` / `com.vcs.valleylib:ftc:2.0.0` with `mavenLocal()` in your repositories.

Next up: the [Quickstart](quickstart.md).
