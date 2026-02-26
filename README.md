# ValleyLib

ValleyLib is a lightweight command-based robotics framework for FTC, split into:

- `valleyLib-core`: scheduler, command primitives, and subsystem abstractions.
- `valleyLib-ftc`: FTC-facing wrappers (`CommandOpMode`, telemetry, hardware helpers).

## Getting started

1. Create subsystems by extending `Subsystem`.
2. Create commands that implement `Command`.
3. Register subsystems in your OpMode and schedule commands through the shared `CommandScheduler`.
4. Extend `CommandOpMode` in FTC and run robot logic from `initialize()`/`run()`.

## Creating a custom command

Use this pattern for reusable commands:

```java
public final class DriveForwardCommand implements Command {
    private final DriveSubsystem drive;
    private final double power;
    private final long durationMs;
    private long startTime;

    public DriveForwardCommand(DriveSubsystem drive, double power, double seconds) {
        this.drive = drive;
        this.power = power;
        this.durationMs = (long) (seconds * 1000);
    }

    @Override
    public void initialize() {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void execute() {
        drive.arcadeDrive(power, 0.0);
    }

    @Override
    public void end(boolean interrupted) {
        drive.stop();
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime >= durationMs;
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return Set.of(drive);
    }
}
```

### Command checklist

- Put setup/reset logic in `initialize()`.
- Put repeated actuator logic in `execute()`.
- Always make `end()` leave hardware in a safe state.
- Implement `isFinished()` for finite commands (or leave false for default/continuous commands).
- Declare every used subsystem from `getRequirements()`.

## Improvement opportunities in this repository

- Add unit tests for scheduler behavior (conflict preemption, default-command scheduling, cancellation).
- Add tests for command groups to ensure nested requirement behavior stays correct.
- Add a `docs/` folder with subsystem templates and OpMode bootstrapping examples.
- Consider adding convenience factory helpers (e.g., `Commands.runOnce`, `Commands.waitSeconds`) to reduce boilerplate.

## Current architecture notes

- `CommandScheduler` is currently a singleton, which keeps integration simple for FTC OpModes.
- Default commands are intended to run only while a subsystem is idle.
- Command groups should aggregate child requirements so scheduler conflict handling still works.
