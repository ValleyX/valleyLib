# Changelog

All notable changes to ValleyLib. Versions correspond to Git tags consumed through JitPack.

## Unreleased

### Added
- **`StateMachine`** command (`core.fsm`): enum-keyed finite state machine with per-state commands, `onEnter`/`onExit` hooks, condition / finish / timeout / global transitions, terminal states, transition listeners, `forceState`, and an allocation-free hot path. See [Finite State Machines](core/state-machines.md).
- **Controllable time** (`core.time`): `Clock`, `RobotClock`, and `ManualClock`. Every timed behavior (`WaitCommand`, `withTimeout`, `StateMachine`, `Trigger.debounce`, `TimedCommand`, `PIDFController`, `Motor.Encoder`) now reads `RobotClock`, making it deterministic under a manual clock in tests.
- **`FunctionalCommand`**: lambda-per-phase command with requirements; the primitive behind all factories.
- **Command names**: `Command.getName()` / `withName(...)`, `NamedCommand`. Factory commands are named (`Intake.startEnd`, `Wait(0.5s)`, `StateMachine[SCORE]`); decorators preserve the inner name; `FtcCommandLogger` logs names.
- `Commands.runEnd(...)` and `Subsystem...` requirement overloads on `Commands.runOnce/run/startEnd/runEnd`.
- `Subsystem.idle()` and `Subsystem.getName()`.
- GitHub Pages deployment of the documentation site on every push to `master`.

### Changed
- `TimedCommand` no longer depends on Pedro Pathing's `Timer`.
- Removed the orphaned `app/` Android application module and template `Example*Test` stubs.

## 1.0.7 — 2026-08-23

### Fixed
- Decorator interruption semantics: commands cut short by `withTimeout`, `until`, `onlyWhile`, a lost `raceWith`, or a completed `deadlineWith` now receive `end(true)`; natural finishes receive `end(false)` exactly once. Fixes `RaceCommand`, `TimeoutCommand`, `UntilCommand`, `OnlyWhileCommand`, `UnlessCommand`, and `DeadlineCommand`.
- `CommandOpMode.init()` fully resets the scheduler, closing a cross-OpMode leak of subsystems, default commands, and listeners.
- JUnit 5 discovery in the FTC module (its tests had never run).
- `PIDFController.setSetPoint` NaN/Infinity velocity error before the first `calculate()`.
- `Motor.Encoder` velocity/acceleration timestamp corruption.

### Added
- Subsystem command factories: `runOnce`, `run`, `startEnd`, `runEnd` (requirement-carrying).
- Trigger auto-registration with `TriggerManager.getDefault()`; `Trigger` implements `BooleanSupplier`; idempotent `bind()`.
- Full gamepad button coverage (`start`, `back`, `guide`, stick clicks) with PlayStation aliases; cast-free fluent configuration.
- `CommandOpMode.initLoop()` and `onStart()` hooks with init-phase telemetry flushing.
- `CommandScheduler.getScheduledCommands()` / `requiring()`; allocation-free run loop.
- `FtcTelemetryBus.clear()`.
- Complete MkDocs documentation site; CI workflow.

## 1.0.6

- Last release before the documentation site. Usable.

## 1.0.2

- Usable but deprecated.

## 1.0.0 – 1.0.5 (except 1.0.2)

- Packaging defects; do not use.
