# Subsystems

A **subsystem** represents a cohesive piece of robot hardware: drivetrain, intake, lift, shooter, arm. Subsystems are the resource the scheduler arbitrates — a command that *requires* a subsystem gets exclusive use of it while running.

Two base classes are provided:

- **`Subsystem`** (`valleyLib-core`) — the platform-agnostic base.
- **`FtcSubsystem`** (`valleyLib-ftc`) — adds a `protected HardwareMap hardwareMap` field for FTC hardware access.

## Writing a subsystem

```java
public class LiftSubsystem extends FtcSubsystem {
    private final DcMotor liftMotor;

    public LiftSubsystem(HardwareMap hardwareMap) {
        super(hardwareMap);
        liftMotor = hardwareMap.get(DcMotor.class, "lift");
    }

    // High-level actions — commands call these, never the motor directly
    public void raise()  { liftMotor.setPower(0.8); }
    public void lower()  { liftMotor.setPower(-0.5); }
    public void stop()   { liftMotor.setPower(0); }

    public boolean atTop() { return liftMotor.getCurrentPosition() > 2200; }

    @Override
    public void periodic() {
        // Runs every scheduler cycle regardless of active commands:
        // sensor caching, state estimation, safety checks.
    }
}
```

!!! note "Automatic registration"
    The `Subsystem` constructor registers the instance with `CommandScheduler.getInstance()` automatically. Just construct your subsystems in `initialize()` — no extra wiring needed.

## `periodic()`

Called once per scheduler cycle, **before** commands execute, whether or not any command is using the subsystem. Use it for:

- reading/caching sensors once per loop
- state estimation (odometry, filters)
- safety limits (e.g., cut power at end stops)

The Pedro integration uses exactly this hook: [`PedroSubsystem.periodic()`](../pedro/overview.md) calls `follower.update()` so path following advances automatically.

## `simulationPeriodic()`

Called only when the scheduler's simulation mode is enabled (or via `runSimulationStep()`). Override it to advance a desktop physics model — see [Desktop Simulation & Testing](../guides/simulation-testing.md).

## Default commands

A **default command** runs whenever no other command requires the subsystem. The classic example is joystick driving:

```java
drive.setDefaultCommand(Commands.run(() ->
        drive.tankDrive(driver.leftY(), driver.rightY())));
```

How it works each cycle:

1. If the subsystem is idle (no command holds its requirement), the scheduler schedules the default command.
2. When another command that requires the subsystem is scheduled (say, an auto-align bound to a button), the default command is interrupted.
3. When that command ends, the subsystem is idle again, so the default command resumes on the next cycle.

```java
drive.setDefaultCommand(command);   // set (or replace)
drive.getDefaultCommand();          // returns the command, or null
```

!!! tip
    Default commands typically never finish on their own (`Commands.run(...)` is ideal). A default command that finishes will simply be rescheduled next cycle.

## Requirements vs. periodic

Two different mechanisms, often confused:

| Mechanism | Runs when | Use for |
| --------- | --------- | ------- |
| `periodic()` | Always, every cycle | Sensors, estimation, safety |
| Commands requiring the subsystem | Only while scheduled | Actuation / behavior |

Keep *actuation* in commands and *observation* in `periodic()`, and the scheduler's exclusivity guarantees stay meaningful.

## Command factories

Every subsystem carries WPILib-style factory methods that build commands **requiring that subsystem** — so they participate fully in scheduler conflict resolution (preempting and being preempted), unlike the requirement-free [`Commands`](command-groups.md#the-commands-factory-class) factories:

| Factory | Behavior |
| ------- | -------- |
| `subsystem.runOnce(action)` | Runs the action once, then finishes |
| `subsystem.run(action)` | Runs the action every cycle, never finishes on its own — ideal for default commands |
| `subsystem.startEnd(onStart, onEnd)` | `onStart` when scheduled, `onEnd` when it ends (finished *or* interrupted) — ideal for `whileTrue` |
| `subsystem.runEnd(action, onEnd)` | Runs every cycle, plus an end action |

```java
// Default drive command that owns the drivetrain:
drive.setDefaultCommand(drive.run(() ->
        drive.tankDrive(driver.leftY(), driver.rightY())));

// Hold-to-run intake with proper requirements:
driver.a().whileTrue(intake.startEnd(intake::in, intake::stop));
```

The factory pattern shines as named methods **on** the subsystem, keeping hardware detail inside it:

```java
public Command raiseCommand() {
    return startEnd(this::raise, this::stop).until(this::atTop);
}
```

Call sites then read like English: `driver.y().onTrue(lift.raiseCommand())`.

!!! tip "Which factory class?"
    Use **subsystem factories** for anything that actuates the subsystem (they carry the requirement). Use the static **`Commands`** factories for glue that touches no mechanism — waits, telemetry markers, combining groups.

## Design guidelines

- **One mechanism, one subsystem.** If two mechanisms can move independently, make them separate subsystems so commands can run in parallel.
- **Expose actions, not hardware.** `lift.raise()` beats `lift.getMotor().setPower(0.8)` scattered across ten commands.
- **Prefer subsystem command factories** over hand-rolled classes for simple behaviors; write a command class when there's real internal state (controllers, profiles) — see [Writing Custom Commands](../guides/custom-commands.md).
