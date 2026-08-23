# RobotContainer

`RobotContainer` (in `com.vcs.valleylib.ftc`) mirrors WPILib's class of the same name: **one central place** where subsystems are constructed, operator controls are bound, and the autonomous command is chosen. OpModes then become thin shells that just hand the container a `HardwareMap` and gamepads.

## Why a container?

Without one, every OpMode re-declares subsystems and bindings, and TeleOp/Auto drift apart. With one:

- Subsystems are created exactly once per OpMode run, in one file.
- The same command objects power TeleOp buttons **and** autonomous routines.
- Adding a mechanism touches one class, not four OpModes.

## The contract

```java
public abstract class RobotContainer {
    public abstract void configureBindings();     // wire driver controls
    public abstract Command getAutonomousCommand(); // the auto routine
    public Command getTeleOpInitCommand() {        // optional teleop startup
        return null;                               // null = nothing to run
    }
}
```

| Member | Purpose |
| ------ | ------- |
| `configureBindings()` | Create triggers and attach commands to buttons |
| `getAutonomousCommand()` | Return the routine an Autonomous OpMode should schedule |
| `getTeleOpInitCommand()` | Optional command to schedule when TeleOp starts (e.g., zero the lift, set LED state). Default returns `null` — skip it in that case |

## A complete example

This is the library's shipped sample (`SimpleRobot`), lightly annotated:

```java
public class SimpleRobot extends RobotContainer {

    public final SampleDriveHardware drive;
    public final SampleIntakeHardware intake;
    public final CommandGamepad driver;

    public SimpleRobot(HardwareMap hardwareMap, Gamepad gamepad1) {
        // Subsystems register themselves with the scheduler automatically
        drive = new SampleDriveHardware(hardwareMap);
        intake = new SampleIntakeHardware(hardwareMap);

        driver = CommandGamepad.forLogitechF310(gamepad1);

        configureBindings();
    }

    @Override
    public void configureBindings() {
        // Default command: tank drive from the sticks whenever drive is idle.
        // drive.run(...) carries the drivetrain requirement automatically.
        drive.setDefaultCommand(drive.run(() ->
                drive.tankDrive(driver.leftY(), driver.rightY())));

        // Hold-to-run intake controls — subsystem factories carry requirements,
        // and bindings auto-register with the polled TriggerManager.
        driver.a().whileTrue(intake.startEnd(intake::intakeIn, intake::stop));
        driver.b().whileTrue(intake.startEnd(intake::intakeOut, intake::stop));

        // Composed one-shot: pulse the intake for half a second
        driver.rightBumper().onTrue(
                intake.runOnce(intake::intakeIn)
                        .andThen(Commands.waitSeconds(0.5))
                        .andThen(intake::stop));
    }

    @Override
    public Command getAutonomousCommand() {
        // Drive forward for 2 s, then guarantee the drivetrain stops
        return drive.run(() -> drive.tankDrive(0.5, 0.5))
                .withTimeout(2.0)
                .finallyDo(drive::stop);
    }
}
```

## Using it from OpModes

```java
@TeleOp(name = "TeleOp")
public class MainTeleOp extends CommandOpMode {
    private SimpleRobot robot;

    @Override
    protected void initialize() {
        robot = new SimpleRobot(hardwareMap, gamepad1);
        Command init = robot.getTeleOpInitCommand();
        if (init != null) scheduler.schedule(init);
    }

    @Override
    protected void run() {}
}
```

Note there's no trigger wiring in the OpMode at all: the container's bindings auto-registered with the shared `TriggerManager` when `configureBindings()` ran, and `CommandOpMode` polls that manager every loop.

```java
@Autonomous(name = "Auto")
public class MainAuto extends CommandOpMode {
    @Override
    protected void initialize() {
        SimpleRobot robot = new SimpleRobot(hardwareMap, gamepad1);
        scheduler.schedule(robot.getAutonomousCommand());
    }

    @Override
    protected void run() {}
}
```

!!! tip "Call `configureBindings()` inside the OpMode lifecycle"
    Trigger bindings auto-register with the shared `TriggerManager`, which `CommandOpMode` clears during `init()` — so construct the container (and run its `configureBindings()`) from the OpMode's `initialize()`, as shown above, and everything is wired automatically. See [Triggers](triggers.md#polling-triggermanager).
