# CommandOpMode

`CommandOpMode` (in `com.vcs.valleylib.ftc.opmode`) is the base OpMode for command-based FTC robots. It owns the scheduler loop, trigger polling, telemetry flushing, and safe shutdown so your OpModes contain only robot-specific code.

```java
@TeleOp(name = "Main TeleOp")
public class MainTeleOp extends CommandOpMode {

    private Robot robot;

    @Override
    protected void initialize() {
        robot = new Robot(hardwareMap, gamepad1);   // subsystems + commands
    }

    @Override
    protected void configureBindings() {
        // Bindings auto-register with the polled TriggerManager — no bind calls.
        robot.driver.a().onTrue(robot.claw.closeCommand());
        robot.driver.b().onTrue(robot.claw.openCommand());
    }

    @Override
    protected void run() {
        telemetryBus.put("Heading", robot.drive.getHeading());
    }
}
```

## Lifecycle

`CommandOpMode` finalizes `init()` and `loop()` and gives you structured hooks instead:

### During `init()`

1. Grabs the global `CommandScheduler` into the protected `scheduler` field and **fully resets it** — canceling leftovers and unregistering the previous OpMode's subsystems and listeners, so stale `periodic()` calls and default commands can never touch dead hardware objects.
2. Creates the `telemetryBus` (`FtcTelemetryBus`) wired to both the Driver Station telemetry and the Panels dashboard.
3. Adopts the shared `TriggerManager.getDefault()` as `triggers` and **clears it**, so bindings from a previous OpMode never leak in.
4. If `enableCommandLogging()` returns true, attaches an [`FtcCommandLogger`](telemetry.md#command-lifecycle-logging) to the scheduler.
5. Calls **your** `initialize()` — create subsystems and commands here.
6. Calls **your** `configureBindings()` — wire triggers here (binding a command auto-registers the trigger; explicit `triggers.bind(...)` is optional).

### During `init_loop()` (between init and play)

1. Calls **your** `initLoop()` — pre-match logic like vision-based randomization detection.
2. Flushes the telemetry bus, so `initLoop()` values show live on the Driver Station and dashboard.

The scheduler does **not** run during the init phase — commands only execute after play.

### On `start()` (driver presses play)

- Calls **your** `onStart()` — the place to schedule match-start commands, e.g. the autonomous routine chosen during `initLoop()`.

### During `loop()`

1. `triggers.poll()` — evaluates every bound trigger; edges schedule/cancel commands.
2. `scheduler.run()` — periodics, default commands, command execution (see [Scheduler](../core/scheduler.md)).
3. **Your** `run()` — OpMode-specific logic and telemetry values.
4. `telemetryBus.update()` — one flush to Driver Station + Panels.

### During `stop()`

- `scheduler.cancelAll()` — every command receives `end(true)`; motors get their stop logic.
- `triggers.clear()` — bindings are dropped.
- `telemetryBus.clear()` — queued telemetry that never flushed is discarded.

## Hooks and fields

| Member | Kind | Purpose |
| ------ | ---- | ------- |
| `initialize()` | abstract hook | Create subsystems, gamepads, commands (runs once in init) |
| `configureBindings()` | optional hook | Bind triggers after `initialize()` |
| `initLoop()` | optional hook | Pre-match logic during the init phase (vision randomization, telemetry) |
| `onStart()` | optional hook | Runs once when the driver presses play — schedule match-start commands |
| `run()` | abstract hook | Per-loop logic after the scheduler runs |
| `enableCommandLogging()` | optional hook | Return `true` to log command lifecycle events to telemetry (default `false`) |
| `scheduler` | protected field | The global `CommandScheduler` |
| `telemetryBus` | protected field | Unified DS + Panels telemetry |
| `triggers` | protected field | The shared `TriggerManager` (`TriggerManager.getDefault()`) polled each loop |

## Autonomous OpModes

For autonomous, schedule your routine in `initialize()` (it will start executing at play), or pick it at match start with the init-phase hooks:

```java
@Autonomous(name = "Left Auto")
public class LeftAuto extends CommandOpMode {

    private Robot robot;
    private boolean propLeft;

    @Override
    protected void initialize() {
        robot = new Robot(hardwareMap, gamepad1);
    }

    @Override
    protected void initLoop() {
        // Runs until the driver presses play — perfect for vision detection
        propLeft = robot.vision.seesPropLeft();
        telemetryBus.put("Prop", propLeft ? "LEFT" : "CENTER");
    }

    @Override
    protected void onStart() {
        scheduler.schedule(propLeft ? robot.leftAuto() : robot.centerAuto());
    }

    @Override
    protected void run() {}
}
```

!!! note "OpMode vs. LinearOpMode"
    `CommandOpMode` extends the iterative `OpMode`, not `LinearOpMode`. Command-based code is inherently non-blocking, so the iterative model is a natural fit: each `loop()` call is one scheduler tick.

!!! note "Scheduler state is handled for you"
    The scheduler is a JVM-wide singleton and the Robot Controller app keeps the JVM alive between OpModes — but `CommandOpMode` fully resets it during `init()`, so every OpMode run starts from a clean slate. Only standalone scheduler users (desktop tests, custom OpMode bases) need to call `scheduler.reset()` themselves.
