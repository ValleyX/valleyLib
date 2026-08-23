# Sample Code Walkthrough

ValleyLib ships copy-ready starter code in `valleyLib-ftc/src/main/java/com/vcs/valleylib/ftc/samples`. These aren't toys — they demonstrate the intended structure for a real season codebase. Copy them into TeamCode and adapt.

## What's included

| File | Demonstrates |
| ---- | ------------ |
| `SampleTeleOp` | A `CommandOpMode` TeleOp using a robot container + command logging |
| `SimpleRobot` | A complete `RobotContainer`: subsystems, default command, button bindings, an auto command |
| `hardware/SampleDriveHardware` | A 4-motor tank drivetrain subsystem |
| `hardware/SampleIntakeHardware` | A CR-servo intake subsystem |
| `auto/SampleAutos` | Reusable Pedro autonomous factories (`simpleTaxi`, `taxiAndCycle`) |
| `auto/PedroMigrationSample` | Line-by-line migration from a Pedro state machine (see [Migration Guide](../pedro/migration.md)) |

## SampleTeleOp

The minimal TeleOp shell — three overrides:

```java
@TeleOp(name = "ValleyLib: Sample TeleOp", group = "Samples")
public class SampleTeleOp extends CommandOpMode {

    private SimpleRobot robot;

    @Override
    protected void initialize() {
        robot = new SimpleRobot(hardwareMap, gamepad1);
    }

    @Override
    protected void run() {
        telemetryBus.put("Drivetrain", "Active");
        telemetryBus.put("Intake Status", "Ready");
    }

    @Override
    protected boolean enableCommandLogging() {
        return true;    // live cmd/scheduled, cmd/finished, cmd/canceled telemetry
    }
}
```

Points to notice:

- No loop bookkeeping — `CommandOpMode` polls triggers, runs the scheduler, and flushes telemetry.
- `enableCommandLogging()` turns on [command lifecycle telemetry](../ftc/telemetry.md#command-lifecycle-logging) with one line.

## SimpleRobot

The container wires everything together (full listing on the [RobotContainer](../ftc/robot-container.md) page). The highlights:

```java
// Default command: joystick tank drive whenever the drivetrain is idle
// (drive.run(...) carries the drivetrain requirement)
drive.setDefaultCommand(drive.run(() ->
        drive.tankDrive(driver.leftY(), driver.rightY())));

// Hold-to-run intake — subsystem factories carry requirements,
// and bindings auto-register with the trigger poller
driver.a().whileTrue(intake.startEnd(intake::intakeIn, intake::stop));
driver.b().whileTrue(intake.startEnd(intake::intakeOut, intake::stop));

// Composed one-shot pulse
driver.rightBumper().onTrue(
        intake.runOnce(intake::intakeIn)
                .andThen(Commands.waitSeconds(0.5))
                .andThen(intake::stop));
```

Three binding idioms in ten lines: default commands, `whileTrue` + `startEnd`, and sequential composition on a button.

## Sample hardware subsystems

`SampleDriveHardware` shows the drivetrain pattern — construct and configure motors in the constructor, expose actions:

```java
public void tankDrive(double leftPower, double rightPower) { ... }
public void stop() { tankDrive(0.0, 0.0); }
```

`SampleIntakeHardware` is the minimal single-actuator subsystem (`intakeIn()`, `intakeOut()`, `stop()`), useful as a template for claws, launchers, and wrists.

Hardware names expected in the RC configuration: `leftFront`, `rightFront`, `leftRear`, `rightRear`, `intake`.

## SampleAutos

Reusable, parameterized autonomous factories built on [`PedroAutoDsl`](../pedro/auto-dsl.md):

```java
// Drive one path and report progress
Command auto = SampleAutos.simpleTaxi(drive, taxiPath);

// Taxi, then cycle while running the intake in parallel
Command auto2 = SampleAutos.taxiAndCycle(drive, intake, taxiPath, cyclePath);
```

The factory pattern — `static Command myAuto(subsystems..., paths...)` — keeps routines testable and lets one Autonomous OpMode offer several routines via a selector.

## Using the samples

1. Copy the files you want into your TeamCode package (adjust `package` lines).
2. Rename `SampleDriveHardware`/`SampleIntakeHardware` to match your robot and hardware config names.
3. For Pedro samples, create your `PedroSubsystem` subclass and Pedro `Constants` first — see [Command-Based Pedro](../pedro/overview.md).
