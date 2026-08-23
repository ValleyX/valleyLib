# The Command Scheduler

`CommandScheduler` (in `com.vcs.valleylib.core.scheduler`) is the central runtime of the command-based system. It is a **singleton** — everything in the library talks to `CommandScheduler.getInstance()`.

Its responsibilities:

- schedule and run commands
- enforce subsystem requirements (mutual exclusion)
- run subsystem `periodic()` methods
- manage default commands
- notify lifecycle listeners
- drive optional simulation hooks

If you use [`CommandOpMode`](../ftc/command-opmode.md), you never call `run()` yourself — the OpMode does it once per loop.

## What happens in `run()`

Each call to `scheduler.run()` performs, in order:

1. **Simulation hooks** — if simulation is enabled, every subsystem's `simulationPeriodic()` runs.
2. **Subsystem periodics** — every registered subsystem's `periodic()` runs, regardless of what commands are active.
3. **Default commands** — for each subsystem that is idle (no command holds its requirement), its default command (if any) is scheduled.
4. **Command execution** — every scheduled command gets one `execute()` call; commands whose `isFinished()` returns true are ended (`end(false)`), their requirements released, and listeners notified.

The execution step iterates over a snapshot, so commands may safely schedule or cancel other commands from inside `execute()`. The snapshot buffer is reused between loops to avoid per-cycle allocation (important at 50+ Hz on Android).

## Scheduling and canceling

```java
CommandScheduler scheduler = CommandScheduler.getInstance();

scheduler.schedule(myCommand);   // starts it (calls initialize())
scheduler.cancel(myCommand);     // interrupts it (calls end(true))
scheduler.cancelAll();           // interrupts everything (OpMode stop)
scheduler.isScheduled(myCommand); // is this instance currently active?
```

Scheduling rules:

- **Idempotent** — scheduling a command that is already running is a no-op (no duplicate `initialize()`).
- **Preemption** — if the new command requires a subsystem that another command holds, the old command is canceled first, then the new one takes the requirement.

`isScheduled(...)` is what powers toggle-style button bindings and is useful for diagnostics.

## Introspection

Two read-only queries support debugging and telemetry dashboards:

```java
scheduler.getScheduledCommands();   // unmodifiable view, in scheduling order
scheduler.requiring(drive);         // command holding a subsystem, or null if idle
```

```java
// Example: live command list on the dashboard
telemetryBus.put("active", scheduler.getScheduledCommands().stream()
        .map(c -> c.getClass().getSimpleName())
        .collect(Collectors.joining(", ")));
```

## Subsystem registration

```java
scheduler.registerSubsystem(mySubsystem);
```

You normally don't call this — the `Subsystem` base constructor registers itself automatically. Registration is what enables `periodic()` calls and default-command management.

## Lifecycle listeners

`CommandSchedulerListener` is an observer interface for command lifecycle events. All methods are default no-ops, so implement only what you need:

```java
scheduler.addListener(new CommandSchedulerListener() {
    @Override
    public void onCommandScheduled(Command command) { /* ... */ }

    @Override
    public void onCommandFinished(Command command) { /* ... */ }

    @Override
    public void onCommandCanceled(Command command) { /* ... */ }
});

scheduler.removeListener(listener);
```

The FTC module ships a ready-made listener, [`FtcCommandLogger`](../ftc/telemetry.md#command-lifecycle-logging), that mirrors these events to telemetry.

## Simulation support

```java
scheduler.setSimulationEnabled(true);   // run simulationPeriodic() inside run()
scheduler.isSimulationEnabled();
scheduler.runSimulationStep();          // run ONLY the simulation hooks
```

`runSimulationStep()` is useful in desktop tests where you want to advance a physics model without executing the full scheduler. See [Desktop Simulation & Testing](../guides/simulation-testing.md).

## Resetting between OpModes and tests

```java
scheduler.reset();
```

`reset()` clears **all** runtime state: it cancels every command (interrupting them), unregisters all subsystems, removes all listeners, and disables simulation. Because the scheduler is a JVM-wide singleton and FTC apps keep the JVM alive between OpModes, [`CommandOpMode`](../ftc/command-opmode.md) calls `reset()` automatically at the start of every `init()` — so each OpMode run begins with a clean scheduler. Outside `CommandOpMode`, it is the tool for isolating unit tests:

```java
@BeforeEach
void setUp() {
    CommandScheduler.getInstance().reset();
}
```

## Threading model

The scheduler is **single-threaded by design**: call `schedule`, `run`, and `cancel` from the same thread (the OpMode loop). There is no internal locking — using it from multiple threads is unsupported.

## API summary

| Method | Description |
| ------ | ----------- |
| `getInstance()` | The global scheduler singleton |
| `schedule(command)` | Start a command, preempting requirement conflicts |
| `run()` | One full scheduler iteration (call once per loop) |
| `cancel(command)` | Interrupt a specific command |
| `cancelAll()` | Interrupt every running command |
| `isScheduled(command)` | Whether a command instance is active |
| `getScheduledCommands()` | Unmodifiable view of active commands |
| `requiring(subsystem)` | The command holding a subsystem, or null |
| `registerSubsystem(subsystem)` | Enable periodic + default-command handling |
| `addListener(l)` / `removeListener(l)` | Lifecycle observers |
| `setSimulationEnabled(b)` / `isSimulationEnabled()` | Toggle sim hooks in `run()` |
| `runSimulationStep()` | Run only `simulationPeriodic()` hooks |
| `reset()` | Clear all scheduler state (tests / mode transitions) |
