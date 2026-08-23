# PedroAutoDsl

`PedroAutoDsl` (in `com.vcs.valleylib.ftc.pedro`) is a command-first autonomous builder tailored to Pedro Pathing. It is the drive-aware sibling of the core [AutoDsl](../core/auto-dsl.md): same builder style, plus `follow(...)` and `waitUntilDriveIdle()` steps that know about your [`PedroSubsystem`](overview.md).

```java
Command auto = PedroAutoDsl.auto(drive, a -> a
    .action(() -> intake.close())
    .follow(pathToSpike, 0.85)
    .action(() -> intake.dropPreload())
    .parallel(
        drive.follow(pathToBackdrop, 0.9),
        lift.toHeight(LiftHeight.HIGH)
    )
    .waitUntilDriveIdle()
    .action(() -> outtake.score())
);
```

`PedroAutoDsl.auto(drive, block)` returns a plain `Command` (a sequence under the hood) — schedule it from your Autonomous OpMode, or decorate it further (`auto.withTimeout(28)`).

## Builder methods

### Path following

| Method | Step added |
| ------ | ---------- |
| `follow(path)` | Follow the `PathChain` at full power; the step ends when the follower is done |
| `follow(path, maxPower)` | Same, with capped power |
| `waitUntilDriveIdle()` | Block until the follower reports not busy — use after `parallel(...)` blocks that contain a follow |

### Actions & commands

| Method | Step added |
| ------ | ---------- |
| `action(runnable)` | Run a `Runnable` once |
| `doInstant(runnable)` | Alias for `action` |
| `command(cmd)` | Run any command |
| `add(cmd)` | Alias for `command` |

### Timing & flow

| Method | Step added |
| ------ | ---------- |
| `waitSeconds(seconds)` | Pause |
| `waitFor(seconds)` | Alias for `waitSeconds` |
| `ifElse(condition, onTrue, onFalse)` | Conditional branch |
| `parallel(cmds...)` | Run commands together; step ends when **all** finish |

## Worked examples

### Simple taxi

```java
public static Command simpleTaxi(PedroSubsystem drive, PathChain taxiPath) {
    return PedroAutoDsl.auto(drive, auto -> auto
        .action(() -> System.out.println("auto:start"))
        .follow(taxiPath, 0.75)
        .waitUntilDriveIdle()
        .action(() -> System.out.println("auto:done")));
}
```

### Taxi + cycle with a parallel intake

```java
public static Command taxiAndCycle(PedroSubsystem drive,
                                   SampleIntakeHardware intake,
                                   PathChain taxiPath,
                                   PathChain cyclePath) {
    return PedroAutoDsl.auto(drive, auto -> auto
        .follow(taxiPath, 0.8)
        .parallel(
            PedroCommands.follow(drive, cyclePath, 0.9),
            Commands.startEnd(intake::intakeIn, intake::stop)
        )
        .waitUntilDriveIdle()
        .action(intake::stop));
}
```

Both examples ship with the library in `SampleAutos` — see the [Sample Code Walkthrough](../guides/samples.md).

### Vision-branched auto

```java
Command auto = PedroAutoDsl.auto(drive, a -> a
    .ifElse(vision::seesPropLeft,
        drive.follow(leftSpikePath),
        drive.follow(centerSpikePath))
    .action(intake::dropPreload)
    .follow(backdropPath, 0.9)
    .waitSeconds(0.3)
    .action(outtake::score)
);
```

## PedroAutoDsl vs. core AutoDsl

| | `AutoDsl` | `PedroAutoDsl` |
| --- | --- | --- |
| Module | `valleyLib-core` | `valleyLib-ftc` |
| Drive-aware `follow` / `waitUntilDriveIdle` | — | ✅ |
| `marker`, `when`, `either`, `race`, `deadline` steps | ✅ | — (use `command(...)` with `Commands.race(...)` etc.) |
| Works in desktop tests | ✅ | Needs Pedro classes |

Anything missing from one builder can always be added through `command(...)` — both produce ordinary command sequences.
