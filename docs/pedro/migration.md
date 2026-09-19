# Migrating from Traditional Pedro Pathing

This guide converts a traditional state-machine Pedro Pathing autonomous into a ValleyLib command-based one, piece by piece. A runnable version of this migration ships with the library as `PedroMigrationSample`.

## First: Pedro 2 → Pedro 3

ValleyLib 2.0 targets Pedro Pathing 3.x. If your code was written against Pedro 2, the Pedro-side API changed underneath you as well:

| Pedro 2 | Pedro 3 |
| ------- | ------- |
| `com.pedropathing:ftc` | `com.pedropathing:core` + `com.pedropathing:revhub` |
| `com.pedropathing.geometry.Pose` | `com.pedropathing.math.Pose` |
| `PathChain` | `Path` (`com.pedropathing.paths.Path`) |
| `follower.pathBuilder().addPath(new BezierLine(a, b))...build()` | `Paths.line(a, b)`, `Paths.curve(...)`, joined with `Paths.path(...)` |
| `.setLinearHeadingInterpolation(a, b)` | `.linear(a, b)` on the path |
| `.setConstantHeadingInterpolation(h)` | `.constant(h)` |
| `.setTangentHeadingInterpolation()` | `.tangent()` |
| `follower.followPath(chain)` | `follower.follow(path)` |
| `follower.setStartingPose(pose)` | `follower.setPose(pose)` |
| `follower.getPose().getX()` | `follower.pose().x()` |
| `follower.setMaxPower(p)` | `maxPathSpeed` — a fraction of top speed, attached per path |
| `follower.setTeleOpDrive(...)` | `follower.manual(forward, lateral, heading)` |

And the ValleyLib-side changes that follow from it:

| ValleyLib 1.x | ValleyLib 2.0 |
| ------------- | ------------- |
| `drive.follow(chain)` / `follow(chain, maxPower)` | `drive.follow(path)` / `follow(path, maxSpeed)` |
| `drive.setMaxPower(p)` | `drive.setMaxSpeed(p)` |
| `follow(path)` forced max power `1.0` | `follow(path)` leaves your configured speed alone |
| — | `drive.stop()` / `drive.stopCommand()` / `drive.isIdle()` / `drive.getPose()` |

An interrupted `FollowPathCommand` now stops the follower. In 1.x it did not, so a preempted path kept driving.

## Direct mapping

### 1. Fields and setup

| Traditional | ValleyLib |
| ----------- | --------- |
| `Follower follower` field | Lives inside your [`PedroSubsystem`](overview.md) |
| `TelemetryManager panelsTelemetry` | Managed by [`FtcTelemetryBus`](../ftc/telemetry.md) inside `CommandOpMode` |
| `Paths paths` / `Path` fields | Defined in `initialize()` or as class members |
| `int pathState` | **Gone** — the scheduler tracks progress |

### 2. Initialization (`init`)

| Traditional | ValleyLib |
| ----------- | --------- |
| `follower = Constants.createFollower(hardwareMap);` | Happens when you construct your `PedroSubsystem` |
| `follower.setStartingPose(new Pose(...));` | Still yours: `drive.getFollower().setPose(...)` in `initialize()` |
| `paths = new Paths(follower);` | Build paths with `Paths.line(...)` / `Paths.curve(...)` in `initialize()` |

### 3. The main loop (`loop`)

| Traditional | ValleyLib |
| ----------- | --------- |
| `follower.update();` | **Automatic** — `PedroSubsystem.periodic()` |
| `autonomousPathUpdate();` (the switch) | **Replaced** by your scheduled command chain |
| `telemetry.update();` / `panelsTelemetry.update();` | **Automatic** — `CommandOpMode` flushes the bus |

### 4. The logic (`autonomousPathUpdate`)

The `switch(pathState)` machine becomes a linear command chain:

- moving to the next state → sequencing (`.andThen(...)` or the DSL's step order)
- `if (!follower.isBusy())` checks → `waitUntilDriveIdle()` / `FollowPathCommand.isFinished()`

!!! warning "Don't hand-roll `!follower.isBusy()`"
    In Pedro 3 the busy flag is cleared only while the follower *holds* the end of a path. With `holdEnd = false`, or after `stop()`, it never clears — a hand-written `!isBusy()` wait hangs. Use `waitUntilDriveIdle()` / `drive.isIdle()`, which check the follower's mode too. See [When is the drive "done"?](overview.md#when-is-the-drive-done).

## Before and after

=== "Traditional state machine"

    ```java
    public void loop() {
        follower.update();
        switch (pathState) {
            case 0:
                follower.follow(scorePreload);
                pathState = 1;
                break;
            case 1:
                if (!follower.isBusy()) {
                    intake.open();
                    follower.follow(park);
                    pathState = 2;
                }
                break;
        }
    }
    ```

=== "ValleyLib command-based"

    ```java
    @Override
    public Command getAutonomousCommand() {
        return PedroAutoDsl.auto(drive, a -> a
            .follow(scorePreload)
            .waitUntilDriveIdle()
            .action(intake::open)
            .follow(park)
        );
    }
    ```

## A complete migrated OpMode

```java
@Autonomous(name = "Migrated Auto")
public class MigratedAuto extends CommandOpMode {

    private DriveSubsystem drive;      // extends PedroSubsystem
    private Path mainPath;

    @Override
    protected void initialize() {
        drive = new DriveSubsystem(hardwareMap);

        // Starting pose — still set explicitly
        drive.getFollower().setPose(new Pose(72, 8, Math.toRadians(90)));

        // Path building — Pedro 3 builds paths standalone, no follower needed
        mainPath = Paths.path(
                Paths.line(new Pose(71.3, 18.9), new Pose(56.2, 34.2))
                        .linear(Math.toRadians(90), Math.toRadians(180)),
                Paths.line(new Pose(56.2, 34.2), new Pose(15.3, 35.7))
                        .tangent());

        // The "state machine", now a readable script
        Command autoRoutine = PedroAutoDsl.auto(drive, auto -> auto
                .follow(mainPath)
                .waitUntilDriveIdle()
                .action(() -> telemetryBus.put("Status", "Path Complete"))
        );

        scheduler.schedule(autoRoutine);
    }

    @Override
    protected void run() {
        // Live pose telemetry — update() calls are automatic
        Pose pose = drive.getPose();
        telemetryBus.put("X", pose.x());
        telemetryBus.put("Y", pose.y());
        telemetryBus.put("Heading", pose.heading());
    }
}
```

## Non-linear autos: keep the state machine, lose the `switch`

If your `switch` had loops or branches — "cycle until 25 seconds, then park" — a linear DSL isn't the right target. Migrate it to a [`StateMachine`](../core/state-machines.md) command instead: each `case` becomes a `state(...)`, each `!follower.isBusy()` check becomes `transitionOnFinish(...)`, and each timer becomes `transitionAfter(...)`. The State Machines page has a side-by-side migration of exactly this shape.

## Why migrate?

1. **Parallelism** — run an arm movement *while* driving with `.parallel(...)`. No more "if the path is 50% done" state contortions.
2. **Readability** — the routine reads top to bottom like a script.
3. **Reusability** — the same command can serve autonomous *and* a TeleOp button (a "score" macro).
4. **Safety** — subsystem requirements guarantee two commands never fight over the drivetrain, `FollowPathCommand` stops the follower when it is interrupted, and `CommandOpMode.stop()` interrupts everything cleanly.
