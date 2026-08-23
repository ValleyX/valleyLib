# Telemetry & Command Logging

## FtcTelemetryBus

`FtcTelemetryBus` (in `com.vcs.valleylib.ftc.telemetry`) is a unified telemetry pipe. Data you `put(...)` once is mirrored to **both**:

- the FTC **Driver Station** telemetry, and
- the **Panels** dashboard (`TelemetryManager`).

No more writing every value twice.

```java
telemetryBus.put("Lift height", lift.getHeight());
telemetryBus.put("Heading", drive.getHeading());
// ... once per loop:
telemetryBus.update();
```

| Method | Description |
| ------ | ----------- |
| `put(key, value)` | Queue a key/value pair for the current loop |
| `update()` | Flush everything queued to both outputs, then clear the queue |
| `clear()` | Discard queued data **without** sending it (e.g., on phase changes or stop) |

Inside a [`CommandOpMode`](command-opmode.md) you get a ready-made `telemetryBus` field, and `update()` is called for you at the end of every loop — just `put` values from `run()`, subsystems, or commands. On `stop()`, the OpMode clears any unflushed queue.

Keys keep their insertion order (backed by a `LinkedHashMap`), so your dashboard layout is stable.

### Standalone use

Outside `CommandOpMode` you can construct one yourself:

```java
FtcTelemetryBus bus = new FtcTelemetryBus(
        telemetry,                                  // DS telemetry from OpMode
        PanelsTelemetry.INSTANCE.getTelemetry()     // Panels manager
);
```

## Command lifecycle logging

`FtcCommandLogger` (in `com.vcs.valleylib.ftc.logging`) is a [`CommandSchedulerListener`](../core/scheduler.md#lifecycle-listeners) that writes command lifecycle events to a telemetry bus:

| Event | Telemetry key | Value |
| ----- | ------------- | ----- |
| Command scheduled | `cmd/scheduled` | Command class name |
| Command finished | `cmd/finished` | Command class name |
| Command canceled | `cmd/canceled` | Command class name |

This gives you live visibility into what the scheduler is doing — invaluable when a binding "isn't working" or an auto stalls.

### Enabling it

The one-liner, in any `CommandOpMode`:

```java
@Override
protected boolean enableCommandLogging() {
    return true;
}
```

Or attach manually (any scheduler, any bus):

```java
scheduler.addListener(new FtcCommandLogger(telemetryBus));
```

### Custom listeners

For richer logging — timestamps, match-log files, filtering — implement the listener yourself:

```java
scheduler.addListener(new CommandSchedulerListener() {
    @Override
    public void onCommandScheduled(Command command) {
        telemetryBus.put("sched@" + timer.seconds(),
                command.getClass().getSimpleName());
    }
});
```

All three listener methods are default no-ops, so override only what you need.
