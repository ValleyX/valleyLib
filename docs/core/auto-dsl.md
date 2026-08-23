# The AutoDsl Builder

`AutoDsl` (in `com.vcs.valleylib.core.auto`) is a small, declarative builder for autonomous routines. It produces a single `SequentialCommandGroup` under the hood, but reads like a script:

```java
Command auto = AutoDsl.auto(a -> a
    .run(claw::close)
    .waitSeconds(0.2)
    .command(driveToBackdrop)
    .either(vision::seesLeft, scoreLeft, scoreCenter)
    .parallel(park, retractLift)
);
```

Because the result is just a `Command`, you can schedule it, decorate it (`auto.withTimeout(28)`), or nest it inside other groups.

For Pedro Pathing autos, prefer the drive-aware [`PedroAutoDsl`](../pedro/auto-dsl.md), which adds `follow(...)` and `waitUntilDriveIdle()` steps.

## Builder methods

Each method appends one step to the sequence and returns the builder for chaining.

### Adding commands and actions

| Method | Step added |
| ------ | ---------- |
| `command(cmd)` | Runs the command |
| `add(cmd)` | Alias for `command` (for teams used to add-step DSLs) |
| `run(action)` | Runs a `Runnable` once (`InstantCommand`) |
| `doInstant(action)` | Alias for `run` |

### Timing

| Method | Step added |
| ------ | ---------- |
| `waitSeconds(seconds)` | Pause for a duration |
| `waitFor(seconds)` | Alias for `waitSeconds` |

### Conditionals

| Method | Step added |
| ------ | ---------- |
| `when(condition, cmd)` | Runs `cmd` only if `condition` is true when the step starts (skipped otherwise) |
| `either(condition, onTrue, onFalse)` | `if/else` between two commands |
| `ifElse(condition, onTrue, onFalse)` | Alias for `either` |

### Concurrency

| Method | Step added |
| ------ | ---------- |
| `parallel(cmds...)` | All commands together; step ends when **all** finish |
| `race(cmds...)` | All commands together; step ends when **any** finishes |
| `deadline(deadline, others...)` | Step ends when the **deadline** command finishes |

### Instrumentation

| Method | Step added |
| ------ | ---------- |
| `marker(label, sink)` | Emits a label to a callback — great for telemetry timelines and profiling |

```java
AutoDsl.auto(a -> a
    .marker("start", tag -> telemetryBus.put("auto", tag))
    .command(driveOut)
    .marker("driven", tag -> telemetryBus.put("auto", tag))
);
```

### Building

`build()` finalizes the sequence — but you rarely call it yourself; `AutoDsl.auto(block)` calls it for you and returns the finished command.

## A complete example

```java
Command auto = AutoDsl.auto(a -> a
    .marker("start", tag -> telemetryBus.put("auto", tag))
    .command(Commands.runOnce(intake::closeGate))
    .waitSeconds(0.15)
    .either(
        vision::seesPropLeft,
        drive.follow(leftPath),
        drive.follow(centerPath)
    )
    .deadline(
        drive.follow(backdropPath),
        Commands.run(shooter::spin).withTimeout(1.2)
    )
    .when(sensor::isReady, Commands.runOnce(outtake::drop))
);
```

## Migration-friendly aliases

The alias methods (`add`, `doInstant`, `waitFor`, `ifElse`) exist so routines written against other command DSLs port over with minimal renaming. They are exact synonyms — use whichever vocabulary your team prefers, but be consistent.

## AutoDsl vs. plain composition

There is nothing AutoDsl does that `Commands.sequence(...)` + decorators cannot. Choose AutoDsl when:

- the routine is long and linear (auto periods usually are),
- you want newcomers to read the routine top-to-bottom,
- you want timeline markers for debugging.

Stick with plain composition for short snippets and button bindings.
