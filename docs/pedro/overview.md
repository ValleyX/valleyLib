# Command-Based Pedro Pathing

The `com.vcs.valleylib.ftc.pedro` package integrates [Pedro Pathing](https://pedropathing.com/) with the command framework, so path following becomes just another command — composable with everything else in the library, and free of manual `follower.update()` calls and state machines.

!!! info "Pedro Pathing 3"
    ValleyLib 2.0 targets Pedro Pathing **3.x** (`com.pedropathing:core` + `com.pedropathing:revhub`). Pedro 3 renamed a lot: `PathChain` became `Path`, `followPath(...)` became `follow(...)`, and `setMaxPower(...)` is gone. If you are coming from Pedro 2, read [Migrating from Traditional Pedro Pathing](migration.md) — it covers both migrations.

| Class | Role |
| ----- | ---- |
| `PedroSubsystem` | Subsystem that owns the `Follower` and updates it every cycle |
| `FollowPathCommand` | A command that drives one `Path` to completion |
| `PedroCommands` | Static command factories for pathing workflows |
| `PedroAutoDsl` | Path-first autonomous builder — see [PedroAutoDsl](auto-dsl.md) |
| `FollowerState` | The "is the drive done?" rule, in one testable place |

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
| `follow(path)` | Command that follows the `Path` at the follower's configured speed |
| `follow(path, maxSpeed)` | Same, capped for this path only |
| `waitUntilIdle()` | Command that finishes once the follower has settled after its path |
| `stopCommand()` | One-shot command that stops the follower |
| `setMaxSpeed(maxSpeed)` | One-shot command setting the follower's default speed cap |
| `getFollower()` | Direct access to the underlying `Follower` (starting pose, pose reads) |
| `getPose()` | The follower's current field pose |
| `stop()` | Plain action (not a command) — stops the follower |
| `isIdle()` | Plain predicate — true once the follower is done with its path |

### Speed, not power

Pedro 3 has no follower-wide `setMaxPower`. A speed cap is `maxPathSpeed`: a **fraction of the robot's maximum achievable velocity**, which Pedro attaches to a path as a modifier and reverts when the path ends.

`follow(path, 0.75)` does exactly that — the cap applies to this path and nothing else, so an interrupted or cancelled path can't leave the robot permanently slow. `setMaxSpeed(0.75)` is the global version, equivalent to the old `setMaxPower`.

`follow(path)` attaches no modifier at all, leaving whatever your Pedro configuration sets. That is a deliberate change from ValleyLib 1.x, where it forced max power to `1.0`.

!!! note
    Because a cap is a fraction of *velocity*, not motor power, `0.75` means "drive at three quarters of the robot's top speed", not "apply 75% power".

## FollowPathCommand

`FollowPathCommand` is what `follow(...)` returns. Its lifecycle:

- **initialize** — starts `follower.follow(path)`, with the speed cap attached to the path when one was given.
- **execute** — nothing; the subsystem's `periodic()` does the updating.
- **isFinished** — `drive.isIdle()` (see below).
- **end(interrupted)** — stops the follower when interrupted. Without this, a preempted path would keep driving, because the follower lives in the subsystem and `update()` runs every cycle regardless of which command holds the drivetrain.
- **requirements** — the drive subsystem, so no other drive command (including a teleop default command) can fight the path.

```java
Command toBackdrop = new FollowPathCommand(drive, backdropPath, 0.9);
// equivalent to:
Command same = drive.follow(backdropPath, 0.9);
```

Because it's a normal command, every decorator works:

```java
drive.follow(riskyPath, 0.8)
     .withTimeout(4.0)                    // bail out if stuck
     .deadlineWith(intake.runInCommand()) // intake only while driving
     .finallyDo(drive::stop);
```

### When is the drive "done"?

`FollowerState.isIdle(follower)` answers this, and it is deliberately not `!follower.isBusy()`.

Pedro 3 clears the busy flag only while the follower is *holding* the end of a path. A follower configured with `holdEnd = false` goes straight to idle at the end of a path and stays "busy" forever; so does one that was stopped or handed to manual control. A bare `!isBusy()` check waits for a flag that will never clear.

The rule ValleyLib uses instead:

1. Still in `FOLLOW` mode → not done.
2. Holding → done once the busy flag clears, i.e. it has settled on the end pose.
3. Anything else (idle, manual) → done.

## PedroCommands

Static equivalents, plus a sequence helper:

```java
PedroCommands.follow(drive, path);
PedroCommands.follow(drive, path, 0.85);
PedroCommands.waitUntilIdle(drive);
PedroCommands.followSequence(drive, path1, path2, path3);  // in order
```

`followSequence` runs each path as its own command, stopping between them. To flow through several legs without stopping, compose them into one Pedro path instead — `Paths.path(leg1, leg2, leg3)` — and follow that.

```java
Command auto = PedroCommands.followSequence(drive, p1, p2, p3)
        .andThen(claw::open)
        .alongWith(vision.trackTag());
```

## Building paths

Pedro 3 builds paths standalone — no follower required — from the `Paths` factory, and heading interpolation is a method on the path itself:

```java
import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

Path toBackdrop = Paths.path(
        Paths.line(new Pose(71.3, 18.9), new Pose(56.2, 34.2))
                .linear(Math.toRadians(90), Math.toRadians(180)),
        Paths.curve(new Pose(56.2, 34.2), new Pose(30.0, 40.0), new Pose(15.3, 35.7))
                .tangent());
```

| Factory | Shape |
| ------- | ----- |
| `Paths.line(start, end)` | A straight line |
| `Paths.curve(poses...)` | A Bézier curve through control poses |
| `Paths.through(poses...)` | A Bézier curve fitted through the given poses |
| `Paths.path(paths...)` | Several paths joined into one |

| Heading | Meaning |
| ------- | ------- |
| `.tangent()` / `.reverseTangent()` | Face along (or against) the path |
| `.constant(heading)` | Hold one heading |
| `.linear(start, end)` | Sweep between two headings |
| `.facingPoint(point)` | Track a field point |

## Using Pedro in TeleOp

Since paths are commands, on-the-fly automation is a button binding:

```java
// Driver presses X: automatically drive to the scoring position
driver.x().onTrue(drive.follow(toScoringPose, 0.7));

// Manual driving is the default command; the path command preempts it
// (both require the drive subsystem) and manual control resumes after.
```

This is the payoff of requirements: the follow command and the default drive command can never run simultaneously. Manual driving in Pedro 3 goes through `follower.manual(forward, lateral, heading)`:

```java
drive.setDefaultCommand(drive.run(() ->
        drive.getFollower().manual(-gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x)));
```

## Recommended structure

1. Keep **all** pathing in commands — no direct follower calls in the OpMode loop.
2. Let the subsystem's `periodic()` own `follower.update()`.
3. Combine path commands with mechanism commands via `parallel`, `deadline`, and `race`.
4. Set the starting pose in `initialize()` via `drive.getFollower().setPose(...)`.
5. Build `Path`s in `initialize()` (or as constants) with `Paths`.

Coming from a switch-statement Pedro auto? The [Migration Guide](migration.md) maps every piece of the traditional structure onto ValleyLib.
