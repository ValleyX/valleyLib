# Migrating from Traditional Pedro Pathing to ValleyLib

This guide explains how to convert a traditional state-machine-based Pedro Pathing autonomous into a ValleyLib command-based autonomous.

## Direct Mapping: Step-by-Step

Using the standard Pedro Pathing example as a baseline:

### 1. The Setup (Fields and Init)
*   **Traditional**: `Follower follower`, `TelemetryManager panelsTelemetry`, `Paths paths`.
*   **ValleyLib**: These are encapsulated.
    *   `follower` lives inside your **`PedroSubsystem`**.
    *   `panelsTelemetry` is managed by **`FtcTelemetryBus`** inside `CommandOpMode`.
    *   `paths` (the `PathChain` objects) are typically defined in your `initialize()` or as constants.

### 2. Initialization (`init`)
*   **Traditional**: `Constants.createFollower(hardwareMap)`, `follower.setStartingPose(...)`, `paths = new Paths(follower)`.
*   **ValleyLib**:
    *   Creating the follower happens when you instantiate your **`PedroSubsystem`**.
    *   `setStartingPose` is still called in your OpMode's **`initialize()`**.
    *   Path building happens in `initialize()` using `drive.getFollower().pathBuilder()`.

### 3. The Main Loop (`loop`)
*   **Traditional**:
    *   `follower.update()`: **Automatic** in ValleyLib (called by `PedroSubsystem.periodic()`).
    *   `pathState = autonomousPathUpdate()`: **Replaced** by the Command Scheduler running your autonomous command.
    *   `telemetry.update()`: **Automatic** in `CommandOpMode`.

### 4. The Logic (`autonomousPathUpdate`)
*   **Traditional**: A complex `switch(pathState)` machine that checks `!follower.isBusy()`.
*   **ValleyLib**: A fluent **`Command`** chain.
    *   Moving to the next state is handled by **`.andThen()`** or the **`PedroAutoDsl`** sequence.
    *   Waiting for a path to finish is handled by **`.waitUntilDriveIdle()`**.

## Migration Example

### Traditional State Machine
```java
public void loop() {
    follower.update();
    switch(pathState) {
        case 0:
            follower.followPath(scorePreload);
            pathState = 1;
            break;
        case 1:
            if(!follower.isBusy()) {
                intake.open();
                follower.followPath(park);
                pathState = 2;
            }
            break;
    }
}
```

### ValleyLib Command-Based
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

## Why Migrate?
1. **Parallelism**: Easily run an arm movement *while* driving using `.parallel()`. No more complex "if path is 50% done" state logic.
2. **Readability**: The autonomous routine reads like a script from top to bottom.
3. **Reusability**: Commands can be reused between Autonomous and TeleOp (e.g., a "Score" button in TeleOp can use the same command as Auto).
4. **Safety**: Subsystem requirements ensure that two different commands never try to move the drivetrain at the same time.
