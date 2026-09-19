# ValleyLib

ValleyLib is a modern, command-based robotics library for FTC, heavily inspired by WPILib and FTCLib. It provides a clean separation between core command logic and FTC-specific hardware integration, with first-class Pedro Pathing 3 support.

**2.0.0 is the first release for [BIOBUZZ](https://www.firstinspires.org/programs/ftc/game-and-season), the 2026-2027 FTC season** — FTC SDK 12 and Pedro Pathing 3. Teams still on DECODE should stay on 1.0.8.

- `valleyLib-core`: Platform-agnostic scheduler, command, and subsystem abstractions (pure Java — desktop-testable).
- `valleyLib-ftc`: FTC-specific integration: OpModes, input handling, motor wrappers, controllers, telemetry, and Pedro Pathing.

## Documentation

**📖 Full documentation site: https://valleyx.github.io/valleyLib/** — published automatically from `master`.

The `docs/` folder is an [MkDocs Material](https://squidfunk.github.io/mkdocs-material/) site covering every feature. Build it locally with:

```bash
pip install -r docs/requirements.txt
mkdocs serve
```

then open http://127.0.0.1:8000. (Read the Docs builds are configured via `.readthedocs.yaml`.)

Quick links into the source docs:

- [Home / feature overview](docs/index.md)
- [Installation](docs/installation.md)
- [Quickstart](docs/quickstart.md)
- Command system: [Commands](docs/core/commands.md) · [Decorators](docs/core/decorators.md) · [Groups & Factories](docs/core/command-groups.md) · [Scheduler](docs/core/scheduler.md) · [Subsystems](docs/core/subsystems.md) · [AutoDsl](docs/core/auto-dsl.md) · [State Machines](docs/core/state-machines.md)
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

*(Replace `Tag` with a release tag like `2.0.0`)*

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
| 1.0.8   | Yes — DECODE season (Pedro Pathing 2.x, FTC SDK 11) |
| 2.0.0   | Yes (recommended) — first BIOBUZZ-season release (Pedro Pathing 3.x, FTC SDK 12) |

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

### Finite State Machines
A first-class `StateMachine` command for anything with loops, branches, timeouts, or overrides — scoring cycles, looping autos, mechanism modes:
```java
StateMachine<Cycle> cycle = new StateMachine<>(Cycle.INTAKE)
    .state(Cycle.INTAKE,   intake.runEnd(intake::in, intake::stop))
    .state(Cycle.TRANSFER, transfer.handoffCommand())
    .state(Cycle.SCORE,    outtake.scoreCommand())
    .transition(Cycle.INTAKE, Cycle.TRANSFER, sensor::hasGamePiece)
    .transitionOnFinish(Cycle.TRANSFER, Cycle.SCORE)
    .transitionAfter(Cycle.SCORE, 0.4, Cycle.INTAKE)
    .transitionFromAny(Cycle.INTAKE, driver.back());
```
Enum states, per-state commands, `onEnter`/`onExit` hooks, condition/finish/timeout/global transitions, terminal states, transition listeners, and an allocation-free hot path. It's a `Command`, so it schedules, binds, decorates, and nests like everything else. See the [State Machines guide](docs/core/state-machines.md).

### Autonomous DSLs
- **`AutoDsl`**: platform-agnostic builder with alias methods (`add`, `doInstant`, `waitFor`, `ifElse`) for easy migration from other command DSLs.
- **`PedroAutoDsl`**: path-first builder for Pedro Pathing autos.
- **`FollowPathCommand`** / **`PedroCommands`** / **`PedroSubsystem`**: full command-based Pedro Pathing 3 integration — no state machines, no manual `follower.update()`.

### Telemetry & Logging
- **`FtcTelemetryBus`**: one `put(...)` mirrors data to the Driver Station and the Panels dashboard; supports `clear()` for discarding stale queued data.
- **`FtcCommandLogger`**: optional listener that logs command lifecycle events (scheduled, finished, canceled) — enable with one override in `CommandOpMode`.

### Desktop Simulation & Testing
`valleyLib-core` has no Android dependencies: unit-test your commands, autos, and state machines on your laptop, with `simulationPeriodic()` hooks, a scheduler simulation mode for physics models, and a `ManualClock` that makes every timeout, wait, dwell transition, and debounce deterministic.

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
