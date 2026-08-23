# Command Decorators

Every `Command` carries a set of **decorator** methods that wrap it in new behavior and return a new command. Decorators chain fluently, letting you build complex behavior without writing a single class:

```java
driveCommand
    .until(robot::atTarget)                 // stop early when at target
    .andThen(intakeCommand.withTimeout(1))  // then intake for up to 1 s
    .finallyDo(() -> drive.stop());         // always stop the drive at the end
```

## Ending a command early

### `withTimeout(double seconds)`

Interrupts the command if it hasn't finished within the given time.

```java
Command safeShoot = shootCommand.withTimeout(2.0);
```

### `until(BooleanSupplier condition)`

Ends the command as soon as the condition becomes true (checked every cycle).

```java
Command driveToLine = driveForward.until(colorSensor::seesLine);
```

### `onlyWhile(BooleanSupplier condition)`

The inverse of `until` — the command runs only while the condition stays true, and ends the moment it turns false. (A gamepad `Trigger` is a `BooleanSupplier`, so it can be passed directly.)

```java
Command spinWhileHeld = spinFlywheel.onlyWhile(driver.rightBumper());
```

## Conditional execution

### `unless(BooleanSupplier condition)`

Skips the command entirely if the condition is true at the moment it starts.

```java
Command scoreIfLoaded = score.unless(() -> !sensor.hasGamePiece());
```

## Adding behavior around a command

### `beforeStarting(Runnable action)`

Runs an action once before the command initializes.

```java
Command aimed = shoot.beforeStarting(turret::lockTarget);
```

### `finallyDo(Runnable action)`

Runs an action once when the command ends, **whether it finished or was interrupted**. This is the right place to stop motors defensively.

```java
Command safeDrive = drivePath.finallyDo(drive::stop);
```

### `andThen(Command... next)` / `andThen(Runnable action)`

Runs commands in sequence. The `Runnable` overload wraps the action in an `InstantCommand` for you.

```java
Command auto = driveOut.andThen(dropPixel).andThen(park::brake);
```

## Repetition

### `repeatedly()`

Restarts the command every time it finishes — it never ends on its own (interrupt it, or combine with `until`/`withTimeout`).

```java
Command blink = flashLed.repeatedly().withTimeout(5.0);
```

## Running commands together

### `alongWith(Command... others)`

Runs this command in **parallel** with the others; the group finishes when *all* commands have finished.

```java
Command deploy = raiseLift.alongWith(extendArm, openClaw);
```

### `raceWith(Command... others)`

Runs in parallel; the group finishes as soon as *any* command finishes (all others are interrupted).

```java
Command driveOrTimeout = followPath.raceWith(Commands.waitSeconds(4));
```

### `deadlineWith(Command... others)`

Runs in parallel; the group finishes when **this** command (the deadline) finishes — the others are interrupted at that point.

```java
// Intake runs only for as long as the path takes:
Command cycle = followPath.deadlineWith(runIntake);
```

## Decorator cheat sheet

| Decorator | Group semantics | Ends when |
| --------- | --------------- | --------- |
| `withTimeout(t)` | wraps | inner finishes **or** `t` elapses |
| `until(cond)` | wraps | inner finishes **or** `cond` true |
| `onlyWhile(cond)` | wraps | inner finishes **or** `cond` false |
| `unless(cond)` | wraps | skipped entirely if `cond` true at start |
| `beforeStarting(r)` | wraps | inner finishes (runs `r` first) |
| `finallyDo(r)` | wraps | inner finishes (runs `r` at end, always) |
| `repeatedly()` | wraps | never (restarts on finish) |
| `andThen(...)` | sequential | last command finishes |
| `alongWith(...)` | parallel | **all** finish |
| `raceWith(...)` | parallel | **first** finishes |
| `deadlineWith(...)` | parallel | **the deadline** finishes |

## Interruption semantics

Decorators preserve honest `end(interrupted)` signals for the wrapped command:

- If the **inner command finishes naturally**, it receives `end(false)`.
- If a decorator **cuts it short** — a timeout elapses, an `until`/`onlyWhile` condition trips, it loses a `raceWith`, or a `deadlineWith` deadline completes first — the inner command receives `end(true)`, exactly as if the scheduler had canceled it.
- A command **skipped** by `unless` is never initialized and never receives `end(...)` at all.

This means cleanup logic keyed on the `interrupted` flag (stop motors, log an abort) behaves identically whether the command was canceled by the scheduler or by a decorator.

## Under the hood

Each decorator returns a small wrapper class from `com.vcs.valleylib.core.command.decorators`:

`TimeoutCommand`, `UntilCommand`, `OnlyWhileCommand`, `UnlessCommand`, `BeforeStartingCommand`, `FinallyCommand`, `RepeatCommand`, `SequentialCommandGroup`, `ParallelCommandGroup`, `RaceCommand`, `DeadlineCommand`, and `ConditionalCommand`.

You can instantiate these directly (see [Command Groups](command-groups.md)), but the fluent methods read better and are the recommended style. To build your *own* decorator, extend [`CommandWrapper`](commands.md#commandwrapper-decorating-existing-commands).
