# Changelog

All notable changes to ValleyLib. Versions correspond to Git tags consumed through JitPack.

## 2.0.0 — 2026-09-19

**The first ValleyLib release for [BIOBUZZ](https://www.firstinspires.org/programs/ftc/game-and-season), the 2026-2027 FIRST Tech Challenge season.** Start the season here: 1.0.8 and earlier target the previous season's Pedro Pathing and FTC SDK.

Moves the `pedro` package to **Pedro Pathing 3**. Pedro 3 is a breaking rewrite of the pathing API, so this release is breaking too — see the [Pedro 2 → Pedro 3 table](pedro/migration.md#first-pedro-2-pedro-3) for the full mapping. ValleyLib 1.0.8 remains the release for teams staying on Pedro 2.x.

### Changed
- **FTC SDK 11.1.0 → 12.0.0**, the BIOBUZZ-season SDK. It stays `compileOnly`, so your TeamCode project still supplies the SDK at build time — but ValleyLib now compiles against the same season's API you do.
- **Dependencies**: `com.pedropathing:ftc:2.0.6` → `com.pedropathing:core:3.0.1` + `com.pedropathing:revhub:3.0.1` (Pedro 3 split the platform-agnostic follower from the REV-hub hardware layer). Both resolve from Maven Central, so the `maven.pedropathing.com` repository is no longer needed.
- **`PathChain` → `Path`** throughout `PedroSubsystem`, `FollowPathCommand`, `PedroCommands`, and `PedroAutoDsl`.
- **`maxPower` → `maxSpeed`**, and it means what it says. Pedro 3 removed `Follower.setMaxPower`; a cap is now `maxPathSpeed`, a fraction of the robot's maximum achievable *velocity*. `follow(path, maxSpeed)` attaches it to the path as a Pedro modifier, so Pedro reverts it when the path ends — an interrupted path can no longer leave the robot permanently slow. `setMaxPower(p)` is now `setMaxSpeed(p)`.
- **`follow(path)` no longer forces full power.** It attaches no cap at all, leaving whatever your Pedro configuration sets.
- `PedroSubsystem.stop()` is a plain action; `stopCommand()` is the command form. (The old `setMaxPower` was the only command factory with this shape, and `drive::stop` in `finallyDo(...)` silently built a command instead of stopping.)
- `PedroMigrationSample` and `SampleAutos` rebuilt on the Pedro 3 `Paths` API.

### Fixed
- **An interrupted `FollowPathCommand` now stops the follower.** Because the follower lives in the subsystem and `periodic()` updates it every cycle, a preempted path used to keep driving — a teleop path binding that lost its requirement carried on to the end of its path.
- **`waitUntilIdle()` no longer hangs.** Pedro 3 clears the follower's busy flag only while it holds the end of a path; with `holdEnd = false`, or after `stop()` or `manual(...)`, the flag never clears and the old `!follower.isBusy()` check waited forever. The rule now also reads the follower's mode.

### Added
- `FollowerState.isIdle(follower)` — the "is the drive done?" rule in one place, with tests against a real Pedro 3 `Follower`.
- `PedroSubsystem.isIdle()`, `getPose()`, `withSpeedLimit(path, maxSpeed)`.

### Removed
- `com.pedropathing:telemetry:1.0.0` from the `api` dependencies. It is a Pedro 2-era artifact that ValleyLib never referenced. Pedro 3's companion is `com.pedropathing:tuning`; add it to your TeamCode project directly if you want Pedro's tuners.

## 1.0.8 — 2026-09-11

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
