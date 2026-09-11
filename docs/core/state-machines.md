# Finite State Machines

Most FTC robot logic is naturally a **finite state machine (FSM)**: a scoring cycle moves *intake → transfer → score → idle*; an autonomous loops *drive to pickup → intake → drive to score → score* until time runs out; a lift is *homing*, *holding*, or *moving*. Traditionally teams write these as a `switch` on an `int pathState` inside `loop()`, with hand-rolled timers and `isBusy()` checks scattered through the cases.

ValleyLib gives you two ways to express state machines, and they compose:

1. **Linear sequences** — `Commands.sequence(...)`, `andThen`, and the [AutoDsl](auto-dsl.md). Perfect when the "machine" is really a straight line of steps. Most autos are.
2. **The `StateMachine` command** — for anything with **loops, branches, timeouts, overrides, or re-entry**. Each state runs a command; transitions are conditions; the whole machine is itself a command you can schedule, bind, decorate, and nest.

This page is about the second: `com.vcs.valleylib.core.fsm.StateMachine`.

## The mental model

An FSM has four ingredients, and each maps to something you already know:

| FSM concept | In ValleyLib |
| ----------- | ------------ |
| **States** | Constants of an `enum` you define (`enum Cycle { IDLE, INTAKE, TRANSFER, SCORE }`) |
| **Behavior while in a state** | A `Command` attached with `state(S, command)` — runs every cycle while the state is active |
| **Entry / exit actions** | `onEnter(S, action)` / `onExit(S, action)` — run once per entry/exit |
| **Transitions** | `transition(from, to, condition)`, `transitionOnFinish(from, to)`, `transitionAfter(from, seconds, to)`, `transitionFromAny(to, condition)` |

Everything else — timing, interrupting the outgoing state's command, sequencing exit/enter hooks, deciding when the machine is done — is handled for you.

```java
enum Cycle { INTAKE, TRANSFER, SCORE, DONE }

StateMachine<Cycle> cycle = new StateMachine<>(Cycle.INTAKE)
        .state(Cycle.INTAKE,   intake.runEnd(intake::in, intake::stop))
        .state(Cycle.TRANSFER, transfer.toOuttakeCommand())
        .state(Cycle.SCORE,    outtake.scoreCommand())
        .transition(Cycle.INTAKE, Cycle.TRANSFER, sensor::hasGamePiece)
        .transitionOnFinish(Cycle.TRANSFER, Cycle.SCORE)
        .transitionOnFinish(Cycle.SCORE, Cycle.DONE)
        .terminal(Cycle.DONE);

driver.rightBumper().onTrue(cycle);   // it's just a command
```

Read it top to bottom: *intake until a piece is sensed; then transfer; when transfer finishes, score; when scoring finishes, done.*

## Building a machine

### Constructor and states

```java
StateMachine<S> fsm = new StateMachine<>(initialState);
```

The initial state is entered every time the machine is initialized (i.e., every time it is scheduled), so the same instance can be re-run.

`state(S, command)` attaches the command that runs *while* the machine is in that state:

- entering the state calls `command.initialize()`,
- every cycle in the state calls `command.execute()`,
- if the command finishes on its own it receives `end(false)` and the state is marked **finished** (see `transitionOnFinish`),
- leaving the state while the command is still running interrupts it with `end(true)`.

A state doesn't need a command. Command-less states are useful as **decision points** and **pass-through states** (they count as finished immediately).

!!! tip "Use the subsystem factories"
    State commands built with [`subsystem.run(...)`, `runEnd(...)`, `startEnd(...)`](subsystems.md#command-factories) carry their subsystem's requirement, and the machine's requirements are the **union** of every state command's requirements. Scheduling the machine therefore preempts anything using those subsystems (e.g., the default drive command), and cancelling the machine releases them all.

### Entry and exit actions

```java
.onEnter(Cycle.SCORE, () -> led.setColor(GREEN))
.onExit(Cycle.SCORE,  () -> led.setColor(OFF))
```

`onEnter` runs *before* the state's command is initialized; `onExit` runs *after* the state's command has ended. `onExit` also runs if the machine itself ends (finishes or is cancelled) while in that state, so it is safe for cleanup.

### Transitions

| Method | Fires when |
| ------ | ---------- |
| `transition(from, to, condition)` | `condition` is true while in `from` |
| `transitionOnFinish(from, to)` | the `from` state's command has finished (immediately for command-less states) |
| `transitionAfter(from, seconds, to)` | the machine has been in `from` for at least `seconds` |
| `transitionFromAny(to, condition)` | `condition` is true, from **any** state except `to` itself |

Conditions are plain `BooleanSupplier`s: sensor reads, gamepad `Trigger`s (a `Trigger` *is* a `BooleanSupplier`), timers, lambdas over your own fields — anything.

```java
.transition(Cycle.INTAKE, Cycle.TRANSFER, sensor::hasGamePiece)   // sensor
.transition(Cycle.IDLE,   Cycle.INTAKE,   driver.a())             // button
.transitionAfter(Cycle.SCORE, 0.4, Cycle.IDLE)                    // dwell time
.transitionOnFinish(Cycle.TRANSFER, Cycle.SCORE)                  // step done
.transitionFromAny(Cycle.IDLE, driver.back())                     // override
```

### Terminal states

```java
.terminal(Cycle.DONE, Cycle.ABORTED)
```

The machine's `isFinished()` returns true when it is in a terminal state **and** that state's command (if any) has finished. A terminal state with a command — say `PARK` with a drive command — lets the machine end cleanly after the final action completes. A command-less terminal state finishes the machine the same cycle it's entered.

Machines with no terminal states run until interrupted — the right shape for TeleOp mechanism cycles bound with `whileTrue`/`toggleOnTrue` or set as a default command.

### Observing transitions

```java
.onTransition((from, to) -> telemetryBus.put("cycle", from + " -> " + to))
```

Listeners fire after every transition (not on the initial entry). This is the one-line way to get a live state trace on the Driver Station and Panels.

## How a cycle runs

Every scheduler cycle in which the machine is active does exactly this:

```text
execute():
  1. if the current state's command is running:
        command.execute()
        if command.isFinished():  command.end(false); mark state finished
  2. find the first transition whose condition is true:
        a. global transitions (transitionFromAny), in declaration order,
           skipping any whose target is the current state
        b. the current state's transitions, in declaration order
  3. if one fired:
        interrupt the outgoing command (end(true)) if still running
        run onExit(from)
        enter `to`: run onEnter(to), initialize its command
        notify onTransition listeners
```

Rules worth internalizing:

- **At most one transition per cycle.** A chain of pass-through states takes one cycle per hop. This keeps behavior deterministic and easy to trace.
- **Declaration order is priority.** When several conditions are true, the first declared wins. Put the most important transitions first.
- **Global transitions beat local ones.** `transitionFromAny` is evaluated first, so aborts and driver overrides always win.
- **The incoming state's command starts executing on the *next* cycle.** Its `initialize()` runs immediately on entry.
- **Self-transitions restart the state.** `transition(S, S, cond)` exits and re-enters `S`, restarting its command — a clean "retry" idiom.

## Runtime API

| Method | Returns |
| ------ | ------- |
| `getState()` | The active state (`null` before the machine starts) |
| `getPreviousState()` | The state before the most recent transition |
| `getTimeInState()` | Seconds since the active state was entered |
| `isIn(S)` | Whether the machine is in that state |
| `in(S)` | A `BooleanSupplier` that tracks `isIn(S)` — for triggers and decorators |
| `isStateCommandFinished()` | Whether the active state's command has finished |
| `forceState(S)` | Immediately transition to `S`, bypassing conditions (exit/enter hooks and listeners still run) |

`in(S)` is how the rest of your robot reacts to the machine's state:

```java
// Rumble while scoring
new Trigger(cycle.in(Cycle.SCORE)).whileTrue(rumbleCommand);

// Only allow manual arm control when the cycle is idle
armManual.onlyWhile(cycle.in(Cycle.IDLE));
```

## Worked example 1: a TeleOp scoring cycle

A complete mechanism state machine with sensor transitions, dwell times, a driver override, and telemetry:

```java
public class ScoringCycle {

    public enum State { IDLE, INTAKE, TRANSFER, SCORE, EJECT }

    public static StateMachine<State> build(IntakeSubsystem intake,
                                            TransferSubsystem transfer,
                                            OuttakeSubsystem outtake,
                                            CommandGamepad operator,
                                            FtcTelemetryBus telemetry) {
        return new StateMachine<>(State.IDLE)
                // ---- behavior per state ----
                .state(State.INTAKE,   intake.runEnd(intake::in, intake::stop))
                .state(State.TRANSFER, transfer.handoffCommand())          // finishes when done
                .state(State.SCORE,    outtake.scoreCommand())             // finishes when done
                .state(State.EJECT,    intake.runEnd(intake::out, intake::stop))

                // ---- entry / exit hooks ----
                .onEnter(State.SCORE, () -> operator.rumble(0.2))
                .onExit(State.SCORE,  outtake::retract)

                // ---- transitions ----
                .transition(State.IDLE,       State.INTAKE,   operator.a())
                .transition(State.INTAKE,     State.TRANSFER, intake::hasGamePiece)
                .transitionAfter(State.INTAKE, 4.0, State.IDLE)             // give up after 4 s
                .transitionOnFinish(State.TRANSFER, State.SCORE)
                .transitionOnFinish(State.SCORE, State.IDLE)
                .transition(State.IDLE,       State.EJECT,    operator.b())
                .transitionAfter(State.EJECT, 0.75, State.IDLE)
                .transitionFromAny(State.IDLE, operator.back())             // panic button

                // ---- observability ----
                .onTransition((from, to) -> telemetry.put("cycle", from + " -> " + to));
    }
}
```

Run it as the mechanisms' **default command** so it's always alive in TeleOp:

```java
StateMachine<ScoringCycle.State> cycle =
        ScoringCycle.build(intake, transfer, outtake, operator, telemetryBus);

// The machine requires intake, transfer, and outtake (union of its state commands),
// so it can be the default for any one of them:
intake.setDefaultCommand(cycle);
```

Because the machine holds all three subsystems, any other command that requires one of them (a manual override bound to a button, say) preempts the whole cycle — and when that command ends, the cycle resumes from `IDLE`. Exactly the behavior you want.

## Worked example 2: a looping autonomous

Linear autos belong in the [AutoDsl](auto-dsl.md). But "cycle as many times as the clock allows, then park" is a **loop with a time-based exit** — a state machine. With Pedro Pathing:

```java
public enum Auto { SCORE_PRELOAD, TO_PICKUP, INTAKE, TO_SCORE, SCORE, PARK, DONE }

StateMachine<Auto> auto = new StateMachine<>(Auto.SCORE_PRELOAD)
        .state(Auto.SCORE_PRELOAD, outtake.scoreCommand())
        .state(Auto.TO_PICKUP,     drive.follow(toPickup, 0.9)
                                        .deadlineWith(intake.runEnd(intake::in, intake::stop)))
        .state(Auto.INTAKE,        intake.runEnd(intake::in, intake::stop))
        .state(Auto.TO_SCORE,      drive.follow(toScore, 0.9)
                                        .alongWith(lift.toHighCommand()))
        .state(Auto.SCORE,         outtake.scoreCommand())
        .state(Auto.PARK,          drive.follow(toPark, 1.0))

        .transitionOnFinish(Auto.SCORE_PRELOAD, Auto.TO_PICKUP)
        .transitionOnFinish(Auto.TO_PICKUP,     Auto.INTAKE)
        .transition(Auto.INTAKE, Auto.TO_SCORE, intake::hasGamePiece)
        .transitionAfter(Auto.INTAKE, 1.5, Auto.TO_SCORE)          // don't wait forever
        .transitionOnFinish(Auto.TO_SCORE, Auto.SCORE)

        // The loop: after scoring, go again — unless it's time to park.
        .transition(Auto.SCORE, Auto.PARK, () -> matchTimer.seconds() > 24)
        .transitionOnFinish(Auto.SCORE, Auto.TO_PICKUP)

        // Safety net from anywhere
        .transitionFromAny(Auto.PARK, () -> matchTimer.seconds() > 27)

        .terminal(Auto.DONE)
        .transitionOnFinish(Auto.PARK, Auto.DONE)
        .onTransition((from, to) -> telemetryBus.put("auto", to.name()));

scheduler.schedule(auto.withTimeout(29.5));
```

Notice the ordering trick on `SCORE`: the park check is declared **before** the loop-back, so when both are true, parking wins.

Compare with the traditional `switch` version of the same routine — the FSM has no `pathState` integer, no manual `follower.update()`, no `isBusy()` polling, no timer bookkeeping, and each state's behavior is a reusable command that TeleOp can bind too.

## Worked example 3: a subsystem with internal states

Not every FSM needs a `StateMachine` command. A mechanism that must **always** manage itself — a lift that homes, then holds position, then moves on request — often belongs *inside the subsystem*, driven from `periodic()`:

```java
public class LiftSubsystem extends FtcSubsystem {

    public enum Mode { HOMING, HOLDING, MOVING }

    private Mode mode = Mode.HOMING;
    private double target;

    @Override
    public void periodic() {
        switch (mode) {
            case HOMING:
                motor.set(-0.3);
                if (limitSwitch.isPressed()) {
                    motor.stopAndResetEncoder();
                    target = 0;
                    mode = Mode.HOLDING;
                }
                break;
            case MOVING:
                motor.set(0.8);
                if (motor.atTargetPosition()) mode = Mode.HOLDING;
                break;
            case HOLDING:
                motor.set(0.2);   // position-control hold
                break;
        }
    }

    public Command toHeightCommand(double ticks) {
        return runOnce(() -> { motor.setTargetPosition((int) ticks); mode = Mode.MOVING; })
                .andThen(Commands.waitUntil(() -> mode == Mode.HOLDING));
    }

    public boolean isHoming() { return mode == Mode.HOMING; }
}
```

Use this pattern when the states are an *implementation detail of one mechanism* and should keep running regardless of which commands are scheduled. Use `StateMachine` when the states *coordinate multiple subsystems* or represent robot-level behavior. The two layer cleanly: a `StateMachine` state can call `lift.toHeightCommand(...)`.

## Nesting and composition

A `StateMachine` is a `Command`, so:

- **Nest machines** — a state's command can be another `StateMachine` (a sub-machine for a complex scoring sequence inside a larger match-flow machine). The inner machine finishing (via its terminal state) is exactly what `transitionOnFinish` on the outer state waits for.
- **Decorate** — `cycle.withTimeout(28)`, `cycle.until(driver.start())`, `cycle.finallyDo(robot::stopAll)`.
- **Sequence** — `Commands.sequence(scorePreload, cycleMachine, parkMachine)`.
- **Bind** — `operator.y().toggleOnTrue(cycle)` to turn the whole machine on and off; `whileTrue` to hold-to-run.
- **Race** — `cycle.raceWith(Commands.waitUntil(endgame))`.

## Driver overrides and recovery

Two tools, for two situations:

- **Declarative override** — `transitionFromAny(IDLE, driver.back())`. Lives in the machine's definition, evaluated first every cycle, visible in the transition log. Prefer this.
- **Imperative override** — `fsm.forceState(IDLE)` from anywhere (a button command, a fault handler). Runs the normal exit/enter sequence and notifies listeners, but bypasses conditions. Use for recovery paths that don't belong in the machine's normal design.

## Testing state machines on the desktop

`StateMachine` lives in `valleyLib-core`, so machines built from core commands run in plain JUnit — no robot needed. Drive the lifecycle by hand or through the scheduler:

```java
@Test
void intakeHandsOffWhenPieceDetected() {
    AtomicBoolean hasPiece = new AtomicBoolean(false);

    StateMachine<Cycle> fsm = new StateMachine<>(Cycle.INTAKE)
            .state(Cycle.INTAKE, Commands.run(() -> {}))
            .transition(Cycle.INTAKE, Cycle.TRANSFER, hasPiece::get);

    fsm.initialize();
    fsm.execute();
    assertEquals(Cycle.INTAKE, fsm.getState());

    hasPiece.set(true);
    fsm.execute();
    assertEquals(Cycle.TRANSFER, fsm.getState());
}
```

Each `execute()` is one scheduler cycle, so you can assert the exact cycle a transition happens on. Time-based transitions (`transitionAfter`) and `getTimeInState()` read `RobotClock`, so install a `ManualClock` to test dwell times exactly:

```java
ManualClock clock = new ManualClock();
RobotClock.setClock(clock);

StateMachine<Cycle> fsm = new StateMachine<>(Cycle.SCORE)
        .transitionAfter(Cycle.SCORE, 0.5, Cycle.IDLE);
fsm.initialize();

clock.advance(0.49);  fsm.execute();  assertEquals(Cycle.SCORE, fsm.getState());
clock.advance(0.01);  fsm.execute();  assertEquals(Cycle.IDLE,  fsm.getState());

RobotClock.useSystemClock();
```

See [Desktop Simulation & Testing](../guides/simulation-testing.md#controlling-time).

## Migrating a `switch` state machine

=== "Traditional"

    ```java
    int pathState = 0;
    ElapsedTime timer = new ElapsedTime();

    public void loop() {
        follower.update();
        switch (pathState) {
            case 0:
                follower.followPath(toPickup);
                intake.in();
                pathState = 1;
                break;
            case 1:
                if (!follower.isBusy()) {
                    timer.reset();
                    pathState = 2;
                }
                break;
            case 2:
                if (intake.hasGamePiece() || timer.seconds() > 1.5) {
                    intake.stop();
                    follower.followPath(toScore);
                    pathState = 3;
                }
                break;
            case 3:
                if (!follower.isBusy()) { outtake.score(); pathState = 0; }
                break;
        }
    }
    ```

=== "ValleyLib StateMachine"

    ```java
    enum S { TO_PICKUP, INTAKE, TO_SCORE, SCORE }

    StateMachine<S> auto = new StateMachine<>(S.TO_PICKUP)
        .state(S.TO_PICKUP, drive.follow(toPickup)
                                 .deadlineWith(intake.runEnd(intake::in, intake::stop)))
        .state(S.INTAKE,    intake.runEnd(intake::in, intake::stop))
        .state(S.TO_SCORE,  drive.follow(toScore))
        .state(S.SCORE,     outtake.scoreCommand())
        .transitionOnFinish(S.TO_PICKUP, S.INTAKE)
        .transition(S.INTAKE, S.TO_SCORE, intake::hasGamePiece)
        .transitionAfter(S.INTAKE, 1.5, S.TO_SCORE)
        .transitionOnFinish(S.TO_SCORE, S.SCORE)
        .transitionOnFinish(S.SCORE, S.TO_PICKUP);

    scheduler.schedule(auto);
    ```

The mapping is mechanical:

| Traditional | StateMachine |
| ----------- | ------------ |
| `int pathState` + magic numbers | An `enum` |
| `case N:` body that starts something | `state(N, command)` — or `onEnter(N, action)` for a one-shot |
| `if (!follower.isBusy())` | `transitionOnFinish(...)` (the follow command finishes when the follower is idle) |
| `if (sensor...)` guard | `transition(from, to, sensor::...)` |
| `timer.reset()` in one case, `timer.seconds() > x` in the next | `transitionAfter(from, x, to)` — no timer variable |
| `pathState = N` | The `to` argument |
| `follower.update()` in `loop()` | `PedroSubsystem.periodic()` — automatic |
| Cleanup you had to remember (`intake.stop()`) | `runEnd`/`startEnd` end actions and `onExit` — automatic on every exit path, including aborts |

## Patterns and anti-patterns

**Do**

- Name states for *what the robot is doing*, not step numbers (`INTAKE`, not `STEP_2`).
- Attach cleanup to the state (`runEnd`, `startEnd`, `onExit`) so it runs on **every** exit, including overrides and cancellation.
- Add a timeout transition to any state that waits on a sensor — sensors fail.
- Put abort/override transitions in `transitionFromAny` so they always win.
- Log transitions with `onTransition` during development; it's the fastest way to debug a stuck machine.
- Keep the machine's definition in one place (a static `build(...)` method or a `RobotContainer` field).

**Don't**

- Don't call hardware directly from transition conditions — conditions should be pure reads. Actuate in state commands and hooks.
- Don't rely on two transitions firing in one cycle; only one ever does.
- Don't build a new `StateMachine` instance every loop or every button press — build once, schedule many times (it resets to the initial state on each `initialize()`).
- Don't reach for `forceState` when a `transitionFromAny` expresses the same intent declaratively.
- Don't use `StateMachine` for a straight line of steps — `Commands.sequence` / AutoDsl reads better and needs no enum.

## API cheat sheet

```java
new StateMachine<>(S initial)

// configuration (fluent, all return the machine)
.state(S, Command)
.onEnter(S, Runnable)             .onExit(S, Runnable)
.transition(S from, S to, BooleanSupplier)
.transitionOnFinish(S from, S to)
.transitionAfter(S from, double seconds, S to)
.transitionFromAny(S to, BooleanSupplier)
.terminal(S...)
.onTransition((from, to) -> ...)

// runtime
.getState()  .getPreviousState()  .getTimeInState()
.isIn(S)     .in(S)               .isStateCommandFinished()
.forceState(S)

// it's a Command
scheduler.schedule(fsm);  trigger.onTrue(fsm);  fsm.withTimeout(28);
```
