# Command Groups & Factories

Command groups combine multiple commands into one. A group *is itself a command* — it can be scheduled, decorated, bound to buttons, and nested inside other groups. Groups aggregate the subsystem requirements of all their children, so scheduler conflict enforcement works through arbitrarily deep compositions.

## The group types

All groups live in `com.vcs.valleylib.core.command.decorators`.

### `SequentialCommandGroup`

Runs commands one after another, in order. Finishes when the last command finishes. If the group is interrupted, the currently running child receives `end(true)`.

```java
Command auto = new SequentialCommandGroup(
    new InstantCommand(claw::close),
    new WaitCommand(0.2),
    new DriveDistanceCommand(drive, 24)
);
```

### `ParallelCommandGroup`

Starts all commands at once. Finishes when **all** of them have finished.

```java
Command deploy = new ParallelCommandGroup(
    new LiftToHeightCommand(lift, HIGH),
    new ExtendArmCommand(arm)
);
```

### `RaceCommand`

Starts all commands at once. Finishes as soon as **any one** finishes; the rest are interrupted.

```java
Command driveOrGiveUp = new RaceCommand(
    new FollowPathCommand(drive, path, 1.0),
    new WaitCommand(5.0)     // safety timeout
);
```

### `DeadlineCommand`

Starts all commands at once. Finishes when the designated **deadline** command finishes; the others are interrupted at that moment.

```java
Command cycle = new DeadlineCommand(
    followPath,              // the deadline
    runIntake                // interrupted when the path ends
);
```

### `ConditionalCommand`

Chooses one of two commands at initialization time based on a condition — the command-based `if/else`.

```java
Command scorePosition = new ConditionalCommand(
    vision::seesLeftProp,
    driveLeft,               // when true
    driveCenter              // when false
);
```

!!! note "Groups are reusable"
    All group types reset their internal state in `initialize()`, so the same group instance can be scheduled again after it finishes — important for commands bound to buttons.

## The `Commands` factory class

`com.vcs.valleylib.core.command.Commands` provides static factories so you can build common commands without `new` noise. It is the idiomatic entry point for inline commands.

!!! tip "Need requirements? Use the subsystem factories"
    `Commands`-built commands carry **no** subsystem requirements. When the command actuates a mechanism, prefer the equivalent [subsystem factories](subsystems.md#command-factories) — `intake.runOnce(...)`, `drive.run(...)`, `intake.startEnd(...)`, `lift.runEnd(...)` — which require their subsystem and participate in scheduler conflict resolution.

| Factory | Returns |
| ------- | ------- |
| `Commands.none()` | A command that does nothing and finishes instantly |
| `Commands.runOnce(action)` | `InstantCommand` — runs the action once, then finishes |
| `Commands.run(action)` | Runs the action every cycle, never finishes on its own (great for default commands) |
| `Commands.startEnd(onStart, onEnd)` | Runs `onStart` when scheduled, `onEnd` when it ends — perfect for `whileTrue` bindings |
| `Commands.waitSeconds(seconds)` | `WaitCommand` |
| `Commands.waitUntil(condition)` | `WaitUntilCommand` |
| `Commands.sequence(cmds...)` | `SequentialCommandGroup` |
| `Commands.parallel(cmds...)` | `ParallelCommandGroup` |
| `Commands.race(cmds...)` | `RaceCommand` |
| `Commands.deadline(deadline, others...)` | `DeadlineCommand` |
| `Commands.either(onTrue, onFalse, condition)` | `ConditionalCommand` |

### Examples

```java
// Hold-to-run intake: start on press, stop on release
// (subsystem factory → the command requires the intake)
driver.a().whileTrue(intake.startEnd(intake::in, intake::stop));

// Default joystick drive (requires the drivetrain)
drive.setDefaultCommand(drive.run(() ->
        drive.arcadeDrive(driver.leftY(), driver.rightX())));

// A timed pulse
Command pulse = Commands.sequence(
    intake.runOnce(intake::in),
    Commands.waitSeconds(0.5),
    intake.runOnce(intake::stop)
);

// Vision-dependent auto branch
Command branch = Commands.either(leftAuto, centerAuto, vision::seesLeft);
```

## Factories vs. decorators vs. constructors

These three snippets are equivalent — pick whichever reads best:

```java
// Decorator style
Command a = closeClaw.andThen(Commands.waitSeconds(0.2)).andThen(driveOut);

// Factory style
Command b = Commands.sequence(closeClaw, Commands.waitSeconds(0.2), driveOut);

// Constructor style
Command c = new SequentialCommandGroup(closeClaw, new WaitCommand(0.2), driveOut);
```

For longer autonomous routines, consider the [AutoDsl](auto-dsl.md), which layers a readable builder on top of these groups.
