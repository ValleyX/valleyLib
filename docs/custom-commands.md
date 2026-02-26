# Creating a Custom Command in ValleyLib

This guide shows the recommended way to build your own command with `valleyLib-core`.

## 1) Implement `Command`

At minimum, implement `execute()`. Most real commands should also implement:

- `initialize()` to reset timers/internal state.
- `isFinished()` to define completion.
- `end(boolean interrupted)` to stop motors safely.
- `getRequirements()` to declare subsystem ownership.

```java
public class DriveDistanceCommand implements Command {
    private final DriveSubsystem drive;
    private final double targetInches;

    public DriveDistanceCommand(DriveSubsystem drive, double targetInches) {
        this.drive = drive;
        this.targetInches = targetInches;
    }

    @Override
    public void initialize() {
        drive.resetEncoders();
    }

    @Override
    public void execute() {
        drive.driveForward(0.4);
    }

    @Override
    public boolean isFinished() {
        return drive.getAverageDistanceInches() >= targetInches;
    }

    @Override
    public void end(boolean interrupted) {
        drive.stop();
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return Set.of(drive);
    }
}
```

## 2) Register subsystems and schedule commands

In your OpMode init flow:

1. Create subsystem instances.
2. `scheduler.registerSubsystem(...)` for each subsystem.
3. Set default commands for idle behavior.
4. Schedule autonomous/triggered commands as needed.

## 3) Use command groups for composition

- `SequentialCommandGroup` for strict order.
- `ParallelCommandGroup` for concurrent actions.
- `InstantCommand` for one-shot actions.
- `WaitCommand` for timeline spacing.

Example:

```java
Command auto = new SequentialCommandGroup(
    new InstantCommand(() -> claw.close()),
    new WaitCommand(0.2),
    new ParallelCommandGroup(
        new LiftToHeightCommand(lift, LiftHeight.HIGH),
        new DriveDistanceCommand(drive, 24)
    )
);
```

## 4) Safety and correctness checklist

- Always stop actuators in `end(...)`.
- Keep `execute()` non-blocking and fast.
- Declare all required subsystems in `getRequirements()`.
- Prefer subsystem methods over writing hardware calls across many commands.
- Make commands reusable by resetting state in `initialize()`.

## Common mistakes

- **Forgetting requirements**: can cause conflicting commands to run at once.
- **Blocking inside `execute()`**: stalls the scheduler.
- **State only set in constructor**: command may fail if reused.
- **No interrupted handling**: can leave hardware running after cancel.
