# Triggers & Button Bindings

A `Trigger` (in `com.vcs.valleylib.ftc.input`) turns any boolean condition into a declarative event source for scheduling commands — the same model as WPILib's `Trigger` and NextFTC's binding pipelines. Most triggers come from [gamepad buttons](gamepads.md), but any `BooleanSupplier` works:

```java
Trigger fromButton = driver.a();
Trigger fromSensor = new Trigger(() -> distance.getDistance(DistanceUnit.CM) < 10);
```

A `Trigger` is itself a `BooleanSupplier` — read it with `getAsBoolean()`, or pass it straight into command decorators:

```java
Command spinWhileHeld = spinFlywheel.onlyWhile(driver.rightBumper());
```

## Binding types

Each binding method attaches a command and returns the same trigger (so bindings chain):

| Binding | Behavior |
| ------- | -------- |
| `onTrue(cmd)` | Schedule once on the rising edge (false → true) |
| `onFalse(cmd)` | Schedule once on the falling edge (true → false) |
| `onChange(cmd)` | Schedule once on **any** edge |
| `whileTrue(cmd)` | Schedule on rising edge, **cancel** on falling edge |
| `whileFalse(cmd)` | Schedule on falling edge, cancel on rising edge |
| `toggleOnTrue(cmd)` | Rising edge: schedule if not running, cancel if running |

```java
driver.a().onTrue(claw.closeCommand());
driver.rightBumper().whileTrue(Commands.startEnd(intake::in, intake::stop));
driver.y().toggleOnTrue(flywheel.spinCommand());
```

!!! tip "`whileTrue` + `startEnd` is the hold-to-run idiom"
    `Commands.startEnd(start, stop)` never finishes on its own, so `whileTrue` runs `start` on press and `stop` fires via `end(true)` on release. Exactly what you want for intakes and rollers.

!!! note "Toggle uses command identity"
    `toggleOnTrue` checks `scheduler.isScheduled(command)` — reuse the **same command instance**, don't build a new one per binding.

## Composition

Triggers compose into richer conditions before you bind:

```java
Trigger shootReady = driver.rightTriggerButton(0.35)
        .and(driver.leftBumper())      // both held
        .debounce(0.05);               // stable for 50 ms

shootReady.whileTrue(shooter.feedAndShoot());
```

| Method | Result |
| ------ | ------ |
| `and(other)` | True when both are true (accepts a `Trigger` or `BooleanSupplier`) |
| `or(other)` | True when either is true |
| `negate()` | Logical inverse |
| `debounce(seconds)` | True only after the condition has stayed true continuously for the duration — filters bounce and brief blips |

Composition returns a **new** trigger; bindings you added earlier stay on the original.

## Polling: TriggerManager

Bindings only fire when the trigger is **polled**. `TriggerManager` is the registry that does this — and registration is now **automatic**: the moment you call a binding method (`onTrue`, `whileTrue`, ...), the trigger registers itself with the shared default manager, `TriggerManager.getDefault()`, which `CommandOpMode` polls once per loop *before* the scheduler runs.

So bindings are one-liners — no `bind` calls needed:

```java
@Override
protected void configureBindings() {
    driver.a().onTrue(claw.closeCommand());
    driver.b().onTrue(claw.openCommand());
    shootReady.whileTrue(shooter.feedAndShoot());
}
```

| Method | Purpose |
| ------ | ------- |
| `TriggerManager.getDefault()` | The shared manager triggers auto-register with; polled by `CommandOpMode` |
| `bind(trigger)` | Register one trigger explicitly (idempotent — double-binding never double-polls) |
| `bindAll(triggers...)` | Register several at once |
| `poll()` | Evaluate every binding once (called by `CommandOpMode`) |
| `clear()` | Drop all registrations (`CommandOpMode` does this on init *and* stop, so bindings never leak between OpModes) |

!!! note "Standalone managers"
    You can still construct your own `TriggerManager` and `bind(...)` triggers to it explicitly — useful in desktop tests or if you want separate driver/operator polling groups. Explicit `bind`/`bindAll` calls remain fully supported and are harmless alongside auto-registration.

## Sensor and state triggers

Since a trigger is just a polled condition, you can bind automation to robot state:

```java
// Auto-stop intake when a game piece is captured
new Trigger(sensor::hasGamePiece)
        .debounce(0.1)
        .onTrue(Commands.runOnce(intake::stop));

// Rumble-free endgame reminder on the dashboard
new Trigger(() -> matchTimer.remaining() < 30)
        .onTrue(Commands.runOnce(() -> telemetryBus.put("!", "ENDGAME")));
```

Like button triggers, these auto-register the moment a command is bound.

## Common pitfalls

- **New command instance each loop** — breaks `toggleOnTrue` and `whileTrue` cancel logic; create commands once, in `initialize()`/`configureBindings()`.
- **Command state not reset** — commands rescheduled by triggers must reset internal state in `initialize()`.
- **Missing requirements** — a button-scheduled command without requirements won't preempt the default command; the two will fight. Use the [subsystem command factories](../core/subsystems.md#command-factories) (`intake.startEnd(...)`) or declare requirements in your command class.
- **Binding outside an OpMode's lifecycle** — `CommandOpMode` clears the default manager during `init()`, so create bindings in `initialize()`/`configureBindings()` (or later), not before the OpMode starts.
