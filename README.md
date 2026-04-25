# ValleyLib

ValleyLib is a modern, command-based robotics library for FTC, heavily inspired by WPILib. It provides a clean separation between core command logic and FTC-specific hardware integration.

- `valleyLib-core`: Platform-agnostic scheduler, command, and subsystem abstractions.
- `valleyLib-ftc`: FTC-specific integration layers, input handling, and utilities.

### JitPack Setup
To use ValleyLib in your project, add the JitPack repository to your `settings.gradle` or `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then add the dependencies:
```gradle
dependencies {
    implementation 'com.github.ValleyX:ValleyLib:core:Tag'
    implementation 'com.github.ValleyX:ValleyLib:ftc:Tag'
}
```
*(Replace `Tag` with a release tag like `1.0.0`)*

## Documentation

- [Getting started guide](docs/getting-started.md)
- [Pedro migration guide](docs/pedro-migration.md)
- [Custom command guide](docs/custom-commands.md)
- [FTC trigger binding guide](docs/ftc-trigger-bindings.md)
- [Pedro command-based guide](docs/pedro-command-based.md)
- [Advanced controls, logging, and simulation](docs/advanced-controls-and-simulation.md)
- [Codebase review and recommendations](docs/codebase-review.md)

## Key Features

### Fluent Command API
Compose complex robot behavior using functional decorators:
```java
driveCommand
    .until(robot::atTarget)
    .andThen(intakeCommand.withTimeout(1.0))
    .finallyDo(() -> drive.stop());
```
Common decorators include `withTimeout`, `until`, `onlyWhile`, `unless`, `beforeStarting`, `finallyDo`, `andThen`, `alongWith`, `raceWith`, and `deadlineWith`.

### Advanced Input Handling
`CommandGamepad` provides easy trigger binding with support for both Xbox and PlayStation naming conventions:
- **Xbox**: `a()`, `b()`, `x()`, `y()`, `leftBumper()`, `rightBumper()`, `dpadUp()`, etc.
- **PlayStation**: `cross()`, `circle()`, `square()`, `triangle()`, `l1()`, `r1()`.
- Includes presets for `forLogitechF310(gamepad)` and `forDualShockLike(gamepad)`.
- Configurable stick deadbands and custom shaping (exponents) via `withStickDeadband()` and `withStickExponent()`.

### Autonomous DSLs
ValleyLib provides powerful DSLs for building autonomous routines:
- **`AutoDsl`**: A platform-agnostic builder API with alias methods like `add`, `doInstant`, `waitFor`, and `ifElse` for easy migration from other command DSLs.
- **`PedroAutoDsl`**: Specifically for Pedro Pathing, integrating path follows with command execution.
- **`FollowPathCommand`**: A command wrapper for individual path execution.
- **`PedroSubsystem`**: A subsystem abstraction for Pedro pathing.

### Telemetry & Logging
- **`FtcTelemetryBus`**: A centralized telemetry system that mirrors data to both the Driver Station and Panels dashboard.
- **`FtcCommandLogger`**: Optional listener that logs command lifecycle events (scheduled, finished, interrupted) to aid real-time debugging.

## Sample Starters

ValleyLib includes several building blocks to help teams get started quickly:
- **Sample Hardware**: `SampleDriveHardware` and `SampleIntakeHardware`.
- **Sample Autos**: `SampleAutos.simpleTaxi(drive, taxiPath)` and `SampleAutos.taxiAndCycle(drive, intake, taxiPath, cyclePath)`.
- **RobotContainer**: A template for centralizing robot subsystems and button bindings.

See `valleyLib-ftc/src/main/java/com/vcs/valleylib/ftc/samples` for copy-ready templates built around `PedroAutoDsl` and `PedroCommands`.
