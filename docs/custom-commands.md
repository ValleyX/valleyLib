# Custom Commands Guide

This guide shows how to build robust custom commands in ValleyLib.

## Lifecycle refresher

Every command follows this lifecycle:

1. `initialize()` runs once when scheduled.
2. `execute()` runs every scheduler cycle.
3. `isFinished()` decides when command is done.
4. `end(interrupted)` runs once on finish/cancel.

## Template

```java
public final class MyCommand implements Command {
    @Override
    public void initialize() {}

    @Override
    public void execute() {}

    @Override
    public boolean isFinished() { return false; }

    @Override
    public void end(boolean interrupted) {}

    @Override
    public Set<Subsystem> getRequirements() {
        return Set.of();
    }
}
```

## Example: run a motor for a fixed time

```java
public final class TimedIntakeCommand implements Command {
    private final IntakeSubsystem intake;
    private final double power;
    private final long durationMs;
    private long startTime;

    public TimedIntakeCommand(IntakeSubsystem intake, double power, double seconds) {
        this.intake = intake;
        this.power = power;
        this.durationMs = (long) (seconds * 1000);
    }

    @Override
    public void initialize() {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void execute() {
        intake.setPower(power);
    }

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime >= durationMs;
    }

    @Override
    public void end(boolean interrupted) {
        intake.setPower(0.0);
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return Set.of(intake);
    }
}
```

## Best practices

- Keep commands focused on one behavior.
- Prefer composition with command groups over giant command classes.
- Use `end()` to guarantee safe shutdown.
- Avoid raw hardware access in multiple commands; route through subsystem APIs.
- Always declare requirements to prevent command conflicts.
