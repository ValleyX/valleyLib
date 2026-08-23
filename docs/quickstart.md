# Quickstart

This guide takes you from an empty TeamCode folder to a working command-based robot with driver controls and a simple autonomous. It assumes you have completed [Installation](installation.md).

The command-based structure has four layers:

```
CommandOpMode  (the OpMode — runs the scheduler loop)
   └── RobotContainer  (creates subsystems + bindings)
         ├── Subsystems  (hardware: drive, intake, ...)
         └── Commands    (behavior, bound to buttons or run in auto)
```

## 1. Define a subsystem

Subsystems encapsulate hardware. Extend `FtcSubsystem` to get access to the `HardwareMap`; the base class automatically registers the subsystem with the scheduler.

```java
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.vcs.valleylib.ftc.hardware.FtcSubsystem;

public class IntakeSubsystem extends FtcSubsystem {
    private final DcMotor motor;

    public IntakeSubsystem(HardwareMap hardwareMap) {
        super(hardwareMap);
        motor = hardwareMap.get(DcMotor.class, "intake");
    }

    public void setPower(double power) {
        motor.setPower(power);
    }

    public void stop() {
        motor.setPower(0);
    }
}
```

!!! tip "Design rule"
    Expose *actions* (`setPower`, `stop`, `raiseToHigh`) rather than raw hardware objects. Commands should never touch motors directly — that keeps hardware access in one place.

## 2. Create a RobotContainer

The `RobotContainer` is the single place where subsystems are created and driver controls are wired up — mirroring WPILib's concept of the same name.

```java
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.Commands;
import com.vcs.valleylib.ftc.RobotContainer;
import com.vcs.valleylib.ftc.input.CommandGamepad;

public class Robot extends RobotContainer {
    public final IntakeSubsystem intake;
    public final CommandGamepad driver;

    public Robot(HardwareMap hwMap, Gamepad gp1) {
        intake = new IntakeSubsystem(hwMap);
        driver = CommandGamepad.forLogitechF310(gp1);
        configureBindings();
    }

    @Override
    public void configureBindings() {
        // Hold A: run intake; release: stop.
        // intake.startEnd(...) builds a command that REQUIRES the intake,
        // and the binding registers itself with the trigger poller.
        driver.a().whileTrue(
            intake.startEnd(() -> intake.setPower(1), intake::stop));
    }

    @Override
    public Command getAutonomousCommand() {
        // Wait 1s, run the intake at half power for 2s, then stop.
        return Commands.waitSeconds(1)
                .andThen(() -> intake.setPower(0.5))
                .andThen(Commands.waitSeconds(2))
                .finallyDo(intake::stop);
    }
}
```

## 3. Create a TeleOp OpMode

Extend `CommandOpMode` instead of `OpMode`. It runs the scheduler, polls triggers, and flushes telemetry for you every loop.

```java
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.vcs.valleylib.ftc.opmode.CommandOpMode;

@TeleOp(name = "Main TeleOp")
public class MainTeleOp extends CommandOpMode {
    private Robot robot;

    @Override
    protected void initialize() {
        robot = new Robot(hardwareMap, gamepad1);
    }

    @Override
    protected void run() {
        telemetryBus.put("Intake", "ready");
    }
}
```

!!! note "Who polls the triggers?"
    `Trigger` objects fire when polled. Binding a command (`onTrue`, `whileTrue`, ...) automatically registers the trigger with the shared `TriggerManager`, and `CommandOpMode` polls it once per loop — so the container's bindings just work, no extra wiring. See [Triggers](ftc/triggers.md).

## 4. Create an Autonomous OpMode

```java
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.vcs.valleylib.ftc.opmode.CommandOpMode;

@Autonomous(name = "Main Auto")
public class MainAuto extends CommandOpMode {
    @Override
    protected void initialize() {
        Robot robot = new Robot(hardwareMap, gamepad1);
        scheduler.schedule(robot.getAutonomousCommand());
    }

    @Override
    protected void run() {}
}
```

That's it. The scheduler executes your command chain step by step; when the OpMode stops, `CommandOpMode` cancels every running command so no motor is left powered.

## What just happened?

Every loop, `CommandOpMode` does four things in order:

1. **Polls triggers** — button edges schedule or cancel bound commands.
2. **Runs the scheduler** — subsystem `periodic()` methods run, default commands are scheduled for idle subsystems, and every active command gets one `execute()` call (finished commands are ended and released).
3. **Calls your `run()`** — for OpMode-specific logic and telemetry.
4. **Flushes telemetry** — everything you `put(...)` goes to the Driver Station and Panels.

## Next steps

- Understand the [command lifecycle](core/commands.md) and how to [write custom commands](guides/custom-commands.md).
- Compose behavior with [decorators](core/decorators.md) and [command groups](core/command-groups.md).
- Give your drivetrain a [default command](core/subsystems.md) for joystick driving.
- Build full autonomous routines with the [AutoDsl](core/auto-dsl.md) or [Pedro Pathing](pedro/overview.md).
