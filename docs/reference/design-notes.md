# Design Notes

Notes on the library's architecture decisions, guarantees, and known trade-offs — useful for contributors and for teams who want to understand *why* things work the way they do.

## Architecture principles

- **Two-module split.** `valleyLib-core` is pure Java with no Android/FTC dependencies, so command logic is desktop-testable. `valleyLib-ftc` contains everything that touches the FTC SDK, Panels, or Pedro Pathing.
- **Singleton scheduler.** One global `CommandScheduler` mirrors WPILib's model and keeps subsystem self-registration ergonomic. The cost is shared state across OpModes — eliminated by `CommandOpMode`, which fully resets the scheduler on every `init()` and cancels/clears everything on `stop()`.
- **Interface-first commands.** `Command` is an interface with rich defaults (decorators), so any class can be a command; `BaseCommand`/`CommandWrapper` are optional conveniences, not requirements.
- **Familiar vocabularies.** API names deliberately match WPILib (`Trigger`, `RobotContainer`, `Commands.sequence`), FTCLib (`Motor`, `MotorEx`, `GamepadEx`), and NextFTC-style DSL aliases (`doInstant`, `waitFor`, `ifElse`) to flatten the learning curve for migrating teams.

## Scheduler guarantees

- **Idempotent scheduling** — scheduling an already-running command is a no-op (no duplicate `initialize()`).
- **Default-command conflict handling** — default commands schedule only when the subsystem is truly idle (no active requirement owner).
- **Reusable command groups** — sequential and parallel groups reset internal state in `initialize()`, so the same instance can be rerun.
- **Interruption propagation** — a group's `end(true)` interrupts its currently running children.
- **Honest interruption semantics in decorators** — a command cut short by `withTimeout`, `until`, `onlyWhile`, a lost race, or a completed deadline receives `end(true)`; natural finishes receive `end(false)`, exactly once; a command skipped by `unless` is never initialized or ended.
- **Requirement aggregation** — groups expose the union of child requirements, so conflict enforcement works through nested compositions.
- **Requirement-carrying subsystem factories** — `Subsystem.runOnce/run/startEnd/runEnd` build inline commands that require their subsystem, closing the gap left by the requirement-free `Commands` factories.
- **Snapshot iteration, reused buffer** — `run()` iterates a snapshot of the scheduled set (commands may schedule/cancel others mid-loop) and reuses the buffer between loops to avoid per-cycle allocation on Android.
- **Single-threaded by design** — all scheduler interaction belongs on the OpMode loop thread; there is no internal locking.

## Input binding model

Binding a command to a `Trigger` auto-registers the trigger with `TriggerManager.getDefault()`, the shared registry `CommandOpMode` polls each loop (and clears on both OpMode init and stop, so bindings never leak across runs). Explicit `bind`/`bindAll` calls remain supported and idempotent — a trigger registered twice is still polled once. `Trigger` implements `BooleanSupplier`, so triggers slot directly into decorators like `onlyWhile(...)`.

## Performance notes

- The scheduler's hot loop allocates nothing in steady state.
- `FtcTelemetryBus` batches per-loop values in one insertion-ordered map and flushes once; `clear()` discards a pending batch without I/O.
- `Motor.Encoder` estimates velocity and acceleration incrementally on read, with separate timestamps so interleaved position/velocity reads don't corrupt each other's deltas.

## Testing

The library carries a JUnit 5 suite in `valleyLib-core` (scheduler semantics, group semantics, factories, AutoDsl) and `valleyLib-ftc` (trigger bindings, which run on the desktop JVM because the input layer is supplier-based). Run it with:

```bash
./gradlew :valleyLib-core:test :valleyLib-ftc:testReleaseUnitTest
```

Both suites, the Android AAR assembly, and a strict docs build run in CI (`.github/workflows/ci.yml`) on every push to `master` and every pull request.

## Known limitations & future work

- **Thread-safety** — deliberate non-goal; document-and-enforce single-thread usage rather than lock.
- **Per-command timing telemetry** — `getScheduledCommands()`/`requiring(...)` enable live command lists; built-in per-command timing metrics are a candidate for a future release.
- **Wall-clock timing in `WaitCommand`/`TimeoutCommand`** — fine on-robot; in unit tests prefer condition-based waits.
