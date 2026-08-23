# ValleyLib

ValleyLib is a modern, command-based robotics library for FTC, heavily inspired by WPILib and FTCLib. It provides a clean separation between core command logic and FTC-specific hardware integration, with first-class Pedro Pathing support.

- `valleyLib-core`: Platform-agnostic scheduler, command, and subsystem abstractions (pure Java — desktop-testable).
- `valleyLib-ftc`: FTC-specific integration: OpModes, input handling, motor wrappers, controllers, telemetry, and Pedro Pathing.

## Documentation

**Full documentation site:** the `docs/` folder is an [MkDocs Material](https://squidfunk.github.io/mkdocs-material/) site covering every feature. Build it locally with:

```bash
pip install -r docs/requirements.txt
mkdocs serve
```

then open http://127.0.0.1:8000. (Read the Docs builds are configured via `.readthedocs.yaml`.)

Quick links into the source docs:

- [Home / feature overview](docs/index.md)
- [Installation](docs/installation.md)
- [Quickstart](docs/quickstart.md)
- Command system: [Commands](docs/core/commands.md) · [Decorators](docs/core/decorators.md) · [Groups & Factories](docs/core/command-groups.md) · [Scheduler](docs/core/scheduler.md) · [Subsystems](docs/core/subsystems.md) · [AutoDsl](docs/core/auto-dsl.md)
- FTC integration: [CommandOpMode](docs/ftc/command-opmode.md) · [Gamepads](docs/ftc/gamepads.md) · [Triggers](docs/ftc/triggers.md) · [Hardware](docs/ftc/hardware.md) · [PID & Feedforward](docs/ftc/control.md) · [Telemetry](docs/ftc/telemetry.md) · [RobotContainer](docs/ftc/robot-container.md)
- Pedro Pathing: [Overview](docs/pedro/overview.md) · [PedroAutoDsl](docs/pedro/auto-dsl.md) · [Migration guide](docs/pedro/migration.md)
- Guides: [Custom commands](docs/guides/custom-commands.md) · [Simulation & testing](docs/guides/simulation-testing.md) · [Samples](docs/guides/samples.md)
- Reference: [API summary](docs/reference/api-summary.md) · [Design notes](docs/reference/design-notes.md)

## Installation (JitPack)

Add the JitPack repository to your `settings.gradle` or `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then add the dependencies:

```gradle
dependencies {
    implementation 'com.github.ValleyX.valleyLib:core:Tag'
    implementation 'com.github.ValleyX.valleyLib:ftc:Tag'
}
```

*(Replace `Tag` with a release tag like `1.0.7`)*

| Version | Usable?           |
| ------- | ----------------- |
| 1.0.0   | No                |
| 1.0.1   | No                |
| 1.0.2   | Yes (deprecated)  |
| 1.0.3   | No                |
| 1.0.4   | No                |
| 1.0.5   | No                |
| 1.0.6   | Yes               |
| 1.0.7   | Yes (recommended) |

## Key Features

### Fluent Command API
Compose complex robot behavior using functional decorators:
```java
driveCommand
    .until(robot::atTarget)
    .andThen(intakeCommand.withTimeout(1.0))
    .finallyDo(() -> drive.stop());
```
Common decorators include `withTimeout`, `until`, `onlyWhile`, `unless`, `beforeStarting`, `finallyDo`, `repeatedly`, `andThen`, `alongWith`, `raceWith`, and `deadlineWith`.

### Advanced Input Handling
`CommandGamepad` provides easy trigger binding with support for both Xbox and PlayStation naming conventions:
- **Xbox**: `a()`, `b()`, `x()`, `y()`, `leftBumper()`, `rightBumper()`, `dpadUp()`, etc.
- **PlayStation**: `cross()`, `circle()`, `square()`, `triangle()`, `l1()`, `r1()`.
- Full button coverage including `start()`, `back()`, `guide()`, and stick clicks (`l3()`/`r3()`).
- Presets for `forLogitechF310(gamepad)` and `forDualShockLike(gamepad)`.
- Configurable stick deadbands and response curves via `withStickDeadband()` and `withStickExponent()`.
- WPILib-style `Trigger` bindings: `onTrue`, `onFalse`, `onChange`, `whileTrue`, `whileFalse`, `toggleOnTrue`, plus `and`/`or`/`negate`/`debounce` composition.
- Bindings **auto-register** with the polled `TriggerManager` — `driver.a().onTrue(cmd)` just works inside a `CommandOpMode`.

### Subsystem Command Factories
Build requirement-carrying commands inline, WPILib-style — no command classes needed:
```java
drive.setDefaultCommand(drive.run(() -> drive.tankDrive(driver.leftY(), driver.rightY())));
driver.a().whileTrue(intake.startEnd(intake::in, intake::stop));
```
`runOnce`, `run`, `startEnd`, and `runEnd` all require their subsystem, so the scheduler resolves conflicts correctly.

### Hardware & Control
- FTCLib-style `Motor`, `MotorEx`, and `MotorGroup` wrappers with GoBILDA presets, a smart encoder (overflow-corrected velocity, acceleration estimation), and velocity/position control modes.
- A full controller stack: `PIDFController`, `PIDController`, `PDController`, `PController`, and WPILib's `SimpleMotorFeedforward`.

### Autonomous DSLs
- **`AutoDsl`**: platform-agnostic builder with alias methods (`add`, `doInstant`, `waitFor`, `ifElse`) for easy migration from other command DSLs.
- **`PedroAutoDsl`**: path-first builder for Pedro Pathing autos.
- **`FollowPathCommand`** / **`PedroCommands`** / **`PedroSubsystem`**: full command-based Pedro Pathing integration — no state machines, no manual `follower.update()`.

### Telemetry & Logging
- **`FtcTelemetryBus`**: one `put(...)` mirrors data to the Driver Station and the Panels dashboard; supports `clear()` for discarding stale queued data.
- **`FtcCommandLogger`**: optional listener that logs command lifecycle events (scheduled, finished, canceled) — enable with one override in `CommandOpMode`.

### Desktop Simulation & Testing
`valleyLib-core` has no Android dependencies: unit-test your commands and autos on your laptop, with `simulationPeriodic()` hooks and a scheduler simulation mode for physics models.

## Sample Starters

See `valleyLib-ftc/src/main/java/com/vcs/valleylib/ftc/samples` for copy-ready templates:
- **`SampleTeleOp`** + **`SimpleRobot`**: a complete TeleOp with a RobotContainer, default drive command, and button bindings.
- **`SampleDriveHardware`** / **`SampleIntakeHardware`**: subsystem templates.
- **`SampleAutos`**: `simpleTaxi(...)` and `taxiAndCycle(...)` Pedro autonomous factories.
- **`PedroMigrationSample`**: line-by-line migration from a traditional Pedro state machine.

## Building from source

```bash
./gradlew :valleyLib-core:test :valleyLib-ftc:assembleRelease
```
