# Commands

A **command** is a unit of robot behavior: drive forward, run the intake, follow a path, wait for a sensor. Commands are scheduled by the [CommandScheduler](scheduler.md) and may control one or more [Subsystems](subsystems.md) for some period of time.

All commands implement the `Command` interface from `com.vcs.valleylib.core.command`.

## The lifecycle

Every command goes through the same four phases:

```text
scheduled ──▶ initialize() ──▶ execute() ──▶ isFinished()? ── no ──▶ execute() ...
                                   │              │
                                   │             yes ──▶ end(false)
                                   │
                            canceled / preempted ──▶ end(true)
```

| Method | Called | Purpose |
| ------ | ------ | ------- |
| `initialize()` | Once, when the command is scheduled | Reset timers, sensors, and internal state |
| `execute()` | Every scheduler cycle while active | Actuator control logic (must be fast and non-blocking) |
| `isFinished()` | After each `execute()` | Return `true` when the command has completed (defaults to `false` — runs forever until interrupted) |
| `end(boolean interrupted)` | Once, when the command ends | Stop motors safely; `interrupted` is `true` when canceled or preempted |
| `getRequirements()` | On scheduling | Declare exclusive subsystem access (defaults to none) |

Only `execute()` is abstract — everything else has a sensible default, so the simplest possible command is:

```java
Command spin = new Command() {
    @Override
    public void execute() {
        flywheel.spin();
    }
};
```

## Requirements: preventing hardware conflicts

`getRequirements()` returns the set of subsystems a command needs **exclusive** access to. When a new command is scheduled that requires a subsystem already in use, the running command is *interrupted* (its `end(true)` is called) and the new one takes over.

```java
@Override
public Set<Subsystem> getRequirements() {
    return Set.of(drive);
}
```

!!! warning
    Forgetting requirements is the most common command-based bug: two commands silently fight over the same motors. Declare every subsystem your command touches.

## Built-in commands

### `InstantCommand`

Runs a single action once, then immediately finishes. Ideal for toggles, state changes, and one-shot hardware calls.

```java
Command close = new InstantCommand(claw::close);
```

### `WaitCommand`

Does nothing for a fixed duration (seconds). The backbone of timed autonomous sequences.

```java
Command pause = new WaitCommand(0.5);
```

### `WaitUntilCommand`

Finishes once a condition becomes true.

```java
Command untilLoaded = new WaitUntilCommand(sensor::hasGamePiece);
```

### `TimedCommand` (valleyLib-ftc)

An abstract command that runs for a fixed duration with structured callbacks — preferred over raw timer bookkeeping in autos:

```java
public class SpinUpCommand extends TimedCommand {
    public SpinUpCommand() { super(1.5); }               // run for 1.5 s

    @Override protected void onStart() { shooter.enable(); }
    @Override protected void onLoop(double elapsedSeconds) {
        shooter.setPower(Math.min(1.0, elapsedSeconds));  // ramp up
    }
    @Override protected void onEnd(boolean interrupted) { shooter.hold(); }
}
```

## `BaseCommand`: a managed template

`BaseCommand` is an abstract convenience base that manages lifecycle state for you. Instead of overriding the interface methods directly, you override protected hooks:

| Hook | Replaces |
| ---- | -------- |
| `onInitialize()` | `initialize()` |
| `onExecute()` | `execute()` |
| `onIsFinished()` | `isFinished()` |
| `onEnd(boolean interrupted)` | `end(...)` |

In exchange you get extra state queries:

- `justFinished()` — `true` exactly once after the command ends (self-clearing), handy for chained state machines.
- `wasInterrupted()` — whether the last run ended by interruption.

`BaseCommand` also guards against `execute()` running after the command has finished.

## `CommandWrapper`: decorating existing commands

`CommandWrapper` extends `BaseCommand` and delegates the whole lifecycle to an inner command. It is the base used to build custom decorators — subclass it and override just the behavior you want to change:

```java
public class LoggedCommand extends CommandWrapper {
    public LoggedCommand(Command inner) { super(inner); }

    @Override
    protected void onInitialize() {
        System.out.println("starting " + inner.getClass().getSimpleName());
        super.onInitialize();
    }
}
```

The wrapper automatically forwards `getRequirements()` from the inner command.

## Composing commands

You rarely subclass anything for day-to-day robot code. Most behavior is assembled from small pieces using:

- [Decorators](decorators.md) — `withTimeout`, `until`, `andThen`, `alongWith`, ...
- [Command groups & factories](command-groups.md) — `Commands.sequence(...)`, `Commands.parallel(...)`, ...
- The [AutoDsl](auto-dsl.md) — a declarative builder for autonomous routines.

For writing your own commands from scratch, see the [Custom Commands guide](../guides/custom-commands.md).
