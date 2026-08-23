# ValleyLib

**ValleyLib** is a modern, command-based robotics library for [FIRST Tech Challenge (FTC)](https://www.firstinspires.org/robotics/ftc), heavily inspired by [WPILib](https://docs.wpilib.org/) and [FTCLib](https://ftclib.org/). It brings the proven *command-based* programming paradigm from FRC to FTC, with first-class support for [Pedro Pathing](https://pedropathing.com/), the Panels dashboard, and desktop simulation.

If you have written FRC code with WPILib — or FTC code with FTCLib — you will feel at home immediately. If you haven't, the [Quickstart](quickstart.md) walks you through everything from scratch.

## Why command-based?

Traditional FTC OpModes tend to grow into giant `loop()` methods full of `if` statements and state machines. Command-based programming replaces that with two simple building blocks:

- **Subsystems** represent your hardware (drivetrain, intake, lift, ...). They expose high-level actions and own their motors and sensors.
- **Commands** represent behavior (drive forward, run the intake for 2 seconds, follow a path, ...). They have a well-defined lifecycle and can be composed like Lego bricks.

A central **CommandScheduler** runs everything, enforces that two commands never fight over the same subsystem, and runs default commands (like joystick driving) whenever a subsystem is idle.

```java
// An entire autonomous routine, readable top to bottom:
driveCommand
    .until(robot::atTarget)
    .andThen(intakeCommand.withTimeout(1.0))
    .finallyDo(() -> drive.stop());
```

## The two modules

| Module | Contents | Platform |
| ------ | -------- | -------- |
| **`valleyLib-core`** | The command framework: `Command`, `CommandScheduler`, `Subsystem`, command groups, decorators, and the `AutoDsl` autonomous builder. | Pure Java — runs on desktop for tests and simulation. |
| **`valleyLib-ftc`** | Everything FTC-specific: `CommandOpMode`, gamepad/trigger bindings, motor wrappers, PID/feedforward controllers, telemetry, and Pedro Pathing integration. | Android (FTC SDK). |

This separation means your command logic can be **unit-tested and simulated on your laptop** without a robot — see [Desktop Simulation & Testing](guides/simulation-testing.md).

## Feature overview

<div class="grid cards" markdown>

- **Fluent Command API** — every command supports decorators like `withTimeout`, `until`, `unless`, `andThen`, `alongWith`, `raceWith`, `deadlineWith`, `repeatedly`, and more. → [Decorators](core/decorators.md)

- **Command groups & factories** — sequential, parallel, race, deadline, and conditional groups, plus the `Commands` factory class for quick one-liners. → [Command Groups](core/command-groups.md)

- **Automatic scheduling** — subsystem requirements, default commands, lifecycle listeners, safe interruption, and requirement-carrying subsystem factories (`drive.run(...)`, `intake.startEnd(...)`), all handled by one scheduler. → [Scheduler](core/scheduler.md) / [Subsystems](core/subsystems.md)

- **Gamepad triggers** — WPILib-style `Trigger` bindings (`onTrue`, `whileTrue`, `toggleOnTrue`, ...) that auto-register with the loop poller, with Xbox *and* PlayStation naming, deadbands, and response curves. → [Gamepads](ftc/gamepads.md) / [Triggers](ftc/triggers.md)

- **Hardware wrappers** — FTCLib-style `Motor`, `MotorEx`, and `MotorGroup` with built-in encoders, velocity/position control, and GoBILDA presets. → [Hardware](ftc/hardware.md)

- **Control theory toolbox** — a full `PIDFController` family plus WPILib's `SimpleMotorFeedforward`. → [Control](ftc/control.md)

- **Pedro Pathing, command-based** — `PedroSubsystem`, `FollowPathCommand`, and a path-first autonomous DSL that replaces state machines entirely. → [Pedro Pathing](pedro/overview.md)

- **Telemetry & logging** — one `FtcTelemetryBus` that mirrors data to the Driver Station *and* the Panels dashboard, plus automatic command lifecycle logging. → [Telemetry](ftc/telemetry.md)

- **Autonomous DSLs** — declarative builders (`AutoDsl`, `PedroAutoDsl`) with markers, waits, conditionals, and parallel actions. → [AutoDsl](core/auto-dsl.md)

- **Desktop simulation** — `simulationPeriodic()` hooks and a simulation mode on the scheduler for hardware-free iteration. → [Simulation](guides/simulation-testing.md)

</div>

## At a glance: a complete TeleOp

```java
@TeleOp(name = "My TeleOp")
public class MainTeleOp extends CommandOpMode {

    private DriveSubsystem drive;
    private IntakeSubsystem intake;
    private CommandGamepad driver;

    @Override
    protected void initialize() {
        drive = new DriveSubsystem(hardwareMap);
        intake = new IntakeSubsystem(hardwareMap);
        driver = CommandGamepad.forLogitechF310(gamepad1);
    }

    @Override
    protected void configureBindings() {
        // Joystick driving whenever nothing else uses the drivetrain
        drive.setDefaultCommand(drive.run(() ->
                drive.tankDrive(driver.leftY(), driver.rightY())));

        // Hold A to intake, hold B to eject — bindings register themselves
        driver.a().whileTrue(intake.startEnd(intake::in, intake::stop));
        driver.b().whileTrue(intake.startEnd(intake::out, intake::stop));
    }

    @Override
    protected void run() {
        telemetryBus.put("Drive", "active");
    }
}
```

That's the whole OpMode. No manual `follower.update()`, no telemetry bookkeeping, no state machine — `CommandOpMode` handles the loop.

## Where to go next

1. [Installation](installation.md) — add ValleyLib to your TeamCode project via JitPack.
2. [Quickstart](quickstart.md) — build your first subsystem, robot container, and OpMode.
3. [Commands](core/commands.md) — learn the core abstraction everything else builds on.
4. [API Summary](reference/api-summary.md) — a one-page index of every public class in the library.
