# Command-Based Pedro Pathing

The `com.vcs.valleylib.ftc.pedro` package integrates [Pedro Pathing](https://pedropathing.com/) with the command framework, so path following becomes just another command — composable with everything else in the library, and free of manual `follower.update()` calls and state machines.

| Class | Role |
| ----- | ---- |
| `PedroSubsystem` | Subsystem that owns the `Follower` and updates it every cycle |
| `FollowPathCommand` | A command that drives one `PathChain` to completion |
| `PedroCommands` | Static command factories for pathing workflows |
| `PedroAutoDsl` | Path-first autonomous builder — see [PedroAutoDsl](auto-dsl.md) |

## PedroSubsystem

Extend `PedroSubsystem` for your drivetrain. It requires a `HardwareMap` and a constructed Pedro `Follower`:

```java
public class DriveSubsystem extends PedroSubsystem {
    public DriveSubsystem(HardwareMap hardwareMap) {
        super(hardwareMap, Constants.createFollower(hardwareMap));
    }
}
```

The key detail: **`periodic()` calls `follower.update()`**. Since the scheduler runs `periodic()` every loop, the follower advances automatically — in autonomous, in teleop, always. You never call `update()` yourself.

### Command factories

The subsystem exposes ready-made commands:

| Method | Returns |
| ------ | ------- |
| `follow(path)` | Command that follows the `PathChain` at full power |
| `follow(path, maxPower)` | Same, with capped max power |
| `waitUntilIdle()` | Command that finishes when the follower is no longer busy |
| `setMaxPower(maxPower)` | One-shot command setting the follower's max power |
| `getFollower()` | Direct access to the underlying `Follower` (starting pose, path builder, pose reads) |

## FollowPathCommand

`FollowPathCommand` is what `follow(...)` returns. Its lifecycle:

- **initialize** — sets the follower max power and starts `followPath(path)`.
- **execute** — nothing; the subsystem's `periodic()` does the updating.
- **isFinished** — true when `!follower.isBusy()`.
- **requirements** — the drive subsystem, so no other drive command (including a teleop default command) can fight the path.

```java
Command toBackdrop = new FollowPathCommand(drive, backdropPath, 0.9);
// equivalent to:
Command same = drive.follow(backdropPath, 0.9);
```

Because it's a normal command, every decorator works:

```java
drive.follow(riskyPath, 0.8)
     .withTimeout(4.0)                  // bail out if stuck
     .deadlineWith(intake.runInCommand()) // intake only while driving
     .finallyDo(drive::stop);
```

## PedroCommands

Static equivalents, plus a sequence helper:

```java
PedroCommands.follow(drive, path);
PedroCommands.follow(drive, path, 0.85);
PedroCommands.waitUntilIdle(drive);
PedroCommands.followSequence(drive, path1, path2, path3);  // in order
```

`followSequence` is handy for quick multi-segment autos:

```java
Command auto = PedroCommands.followSequence(drive, p1, p2, p3)
        .andThen(claw::open)
        .alongWith(vision.trackTag());
```

## Using Pedro in TeleOp

Since paths are commands, on-the-fly automation is a button binding:

```java
// Driver presses X: automatically drive to the scoring position
driver.x().onTrue(drive.follow(toScoringPose, 0.7));

// Manual driving is the default command; the path command preempts it
// (both require the drive subsystem) and manual control resumes after.
```

This is the payoff of requirements: the follow command and the default drive command can never run simultaneously.

## Recommended structure

1. Keep **all** pathing in commands — no direct follower calls in the OpMode loop.
2. Let the subsystem's `periodic()` own `follower.update()`.
3. Combine path commands with mechanism commands via `parallel`, `deadline`, and `race`.
4. Set the starting pose in `initialize()` via `drive.getFollower().setStartingPose(...)`.
5. Build `PathChain`s in `initialize()` using `drive.getFollower().pathBuilder()`.

Coming from a switch-statement Pedro auto? The [Migration Guide](migration.md) maps every piece of the traditional structure onto ValleyLib.
