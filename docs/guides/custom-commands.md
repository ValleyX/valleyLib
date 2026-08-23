# Writing Custom Commands

The built-in commands, [factories](../core/command-groups.md), and [decorators](../core/decorators.md) cover most needs, but real robots always need a few bespoke commands — a PID drive-to-distance, a lift profile, a vision-aligned turn. This guide shows the recommended patterns.

## 1. Implement `Command`

At minimum, implement `execute()`. Most real commands should also implement:

- `initialize()` — reset timers and internal state (commands must be **re-runnable**)
- `isFinished()` — define completion
- `end(boolean interrupted)` — stop motors safely
- `getRequirements()` — declare subsystem ownership

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

## 2. Or extend a base class

Three convenience bases save boilerplate for common shapes:

=== "BaseCommand"

    Managed lifecycle with protected hooks and extra state queries (`justFinished()`, `wasInterrupted()`):

    ```java
    public class HomeLiftCommand extends BaseCommand {
        @Override protected void onInitialize() { lift.startHoming(); }
        @Override protected void onExecute()    { lift.stepDown(); }
        @Override protected boolean onIsFinished() { return lift.atLimitSwitch(); }
        @Override protected void onEnd(boolean interrupted) { lift.zeroAndStop(); }
    }
    ```

=== "TimedCommand"

    Fixed-duration commands with a live elapsed-time callback:

    ```java
    public class RumbleCommand extends TimedCommand {
        public RumbleCommand() { super(0.75); }
        @Override protected void onStart() { pad.rumble(1.0); }
        @Override protected void onLoop(double elapsed) { }
        @Override protected void onEnd(boolean interrupted) { pad.stopRumble(); }
    }
    ```

=== "CommandWrapper"

    Custom decorators around an existing command:

    ```java
    public class TimedLogCommand extends CommandWrapper {
        private long start;
        public TimedLogCommand(Command inner) { super(inner); }

        @Override protected void onInitialize() {
            start = System.currentTimeMillis();
            super.onInitialize();
        }
        @Override protected void onEnd(boolean interrupted) {
            super.onEnd(interrupted);
            System.out.println("took " + (System.currentTimeMillis() - start) + " ms");
        }
    }
    ```

## 3. Prefer composition when possible

Ask "can this be a factory + decorators?" before writing a class. The [subsystem factories](../core/subsystems.md#command-factories) even carry requirements for you:

```java
// Class-free drive-for-time (requires the drivetrain):
Command driveOut = drive.run(() -> drive.tankDrive(0.5, 0.5))
        .withTimeout(2.0)
        .finallyDo(drive::stop);

// Class-free "intake until loaded, max 3 s" (requires the intake):
Command autoIntake = intake.startEnd(intake::in, intake::stop)
        .until(sensor::hasGamePiece)
        .withTimeout(3.0);
```

Write a class when the command has real internal state — controllers, filters, motion profiles — or coordinates multiple subsystems with custom logic.

## 4. Use groups for composition

```java
Command auto = new SequentialCommandGroup(
    new InstantCommand(claw::close),
    new WaitCommand(0.2),
    new ParallelCommandGroup(
        new LiftToHeightCommand(lift, LiftHeight.HIGH),
        new DriveDistanceCommand(drive, 24)
    )
);
```

## Safety and correctness checklist

- ✅ Always stop actuators in `end(...)` — it runs on interruption too.
- ✅ Keep `execute()` non-blocking and fast; never `sleep()` or loop-wait.
- ✅ Declare **all** required subsystems in `getRequirements()`.
- ✅ Reset state in `initialize()`, not the constructor — commands get reused.
- ✅ Route hardware access through subsystem methods, not raw motors in commands.

## Common mistakes

| Mistake | Symptom |
| ------- | ------- |
| Forgetting requirements | Two commands fight over one mechanism; default command never yields |
| Blocking inside `execute()` | The whole scheduler (and OpMode loop) stalls |
| State only set in constructor | Command works once, misbehaves when rescheduled |
| No interrupted handling in `end` | Motors keep running after a cancel or preemption |
| Scheduling in `periodic()` unconditionally | Command spam — schedule from triggers or sequences instead |
