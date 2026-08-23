# Migrating from Traditional Pedro Pathing

This guide converts a traditional state-machine Pedro Pathing autonomous into a ValleyLib command-based one, piece by piece. A runnable version of this migration ships with the library as `PedroMigrationSample`.

## Direct mapping

### 1. Fields and setup

| Traditional | ValleyLib |
| ----------- | --------- |
| `Follower follower` field | Lives inside your [`PedroSubsystem`](overview.md) |
| `TelemetryManager panelsTelemetry` | Managed by [`FtcTelemetryBus`](../ftc/telemetry.md) inside `CommandOpMode` |
| `Paths paths` / `PathChain` fields | Defined in `initialize()` or as class members |
| `int pathState` | **Gone** — the scheduler tracks progress |

### 2. Initialization (`init`)

| Traditional | ValleyLib |
| ----------- | --------- |
| `follower = Constants.createFollower(hardwareMap);` | Happens when you construct your `PedroSubsystem` |
| `follower.setStartingPose(new Pose(...));` | Still yours: `drive.getFollower().setStartingPose(...)` in `initialize()` |
| `paths = new Paths(follower);` | Build chains via `drive.getFollower().pathBuilder()` in `initialize()` |

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

## Before and after

=== "Traditional state machine"

    ```java
    public void loop() {
        follower.update();
        switch (pathState) {
            case 0:
                follower.followPath(scorePreload);
                pathState = 1;
                break;
            case 1:
                if (!follower.isBusy()) {
                    intake.open();
                    follower.followPath(park);
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
    private PathChain mainChain;

    @Override
    protected void initialize() {
        drive = new DriveSubsystem(hardwareMap);

        // Starting pose — still set explicitly
        drive.getFollower().setStartingPose(new Pose(72, 8, Math.toRadians(90)));

        // Path building — same Pedro pathBuilder API
        mainChain = drive.getFollower().pathBuilder()
                .addPath(new BezierLine(new Pose(71.3, 18.9), new Pose(56.2, 34.2)))
                .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(180))
                .addPath(new BezierLine(new Pose(56.2, 34.2), new Pose(15.3, 35.7)))
                .setTangentHeadingInterpolation()
                .build();

        // The "state machine", now a readable script
        Command autoRoutine = PedroAutoDsl.auto(drive, auto -> auto
                .follow(mainChain)
                .waitUntilDriveIdle()
                .action(() -> telemetryBus.put("Status", "Path Complete"))
        );

        scheduler.schedule(autoRoutine);
    }

    @Override
    protected void run() {
        // Live pose telemetry — update() calls are automatic
        telemetryBus.put("X", drive.getFollower().getPose().getX());
        telemetryBus.put("Y", drive.getFollower().getPose().getY());
        telemetryBus.put("Heading", drive.getFollower().getPose().getHeading());
    }
}
```

## Why migrate?

1. **Parallelism** — run an arm movement *while* driving with `.parallel(...)`. No more "if the path is 50% done" state contortions.
2. **Readability** — the routine reads top to bottom like a script.
3. **Reusability** — the same command can serve autonomous *and* a TeleOp button (a "score" macro).
4. **Safety** — subsystem requirements guarantee two commands never fight over the drivetrain, and `CommandOpMode.stop()` interrupts everything cleanly.
