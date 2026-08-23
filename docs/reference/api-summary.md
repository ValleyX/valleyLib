# API Summary

A one-page index of every public type in ValleyLib, grouped by package, with links to the relevant guide.

## valleyLib-core

### `com.vcs.valleylib.core.command`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `Command` | interface | The core behavior unit: lifecycle methods + decorator defaults | [Commands](../core/commands.md) |
| `Commands` | final class | Static factories: `none`, `runOnce`, `run`, `startEnd`, `waitSeconds`, `waitUntil`, `sequence`, `parallel`, `race`, `either`, `deadline` | [Command Groups](../core/command-groups.md) |
| `InstantCommand` | class | Runs an action once, finishes immediately | [Commands](../core/commands.md#instantcommand) |
| `WaitCommand` | class | Waits a fixed number of seconds | [Commands](../core/commands.md#waitcommand) |
| `WaitUntilCommand` | class | Finishes when a condition becomes true | [Commands](../core/commands.md#waituntilcommand) |
| `BaseCommand` | abstract class | Managed lifecycle template with `justFinished()` / `wasInterrupted()` | [Commands](../core/commands.md#basecommand-a-managed-template) |
| `CommandWrapper` | abstract class | Delegating base for building custom decorators | [Commands](../core/commands.md#commandwrapper-decorating-existing-commands) |

### `com.vcs.valleylib.core.command.decorators`

| Type | Summary | Docs |
| ---- | ------- | ---- |
| `SequentialCommandGroup` | Commands in order; ends when the last ends | [Command Groups](../core/command-groups.md) |
| `ParallelCommandGroup` | All together; ends when **all** end | [Command Groups](../core/command-groups.md) |
| `RaceCommand` | All together; ends when **any** ends | [Command Groups](../core/command-groups.md) |
| `DeadlineCommand` | All together; ends when the **deadline** ends | [Command Groups](../core/command-groups.md) |
| `ConditionalCommand` | Runs one of two commands based on a condition | [Command Groups](../core/command-groups.md) |
| `TimeoutCommand` | Interrupts the inner command after a duration (`withTimeout`) | [Decorators](../core/decorators.md) |
| `UntilCommand` | Ends when a condition becomes true (`until`) | [Decorators](../core/decorators.md) |
| `OnlyWhileCommand` | Runs only while a condition holds (`onlyWhile`) | [Decorators](../core/decorators.md) |
| `UnlessCommand` | Skips the inner command if a condition is true (`unless`) | [Decorators](../core/decorators.md) |
| `BeforeStartingCommand` | Runs an action before initialization (`beforeStarting`) | [Decorators](../core/decorators.md) |
| `FinallyCommand` | Runs an action when the command ends (`finallyDo`) | [Decorators](../core/decorators.md) |
| `RepeatCommand` | Restarts the inner command whenever it finishes (`repeatedly`) | [Decorators](../core/decorators.md) |

### `com.vcs.valleylib.core.scheduler`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `CommandScheduler` | singleton | Schedules/runs commands, enforces requirements, default commands, sim hooks, introspection (`getScheduledCommands`, `requiring`) | [Scheduler](../core/scheduler.md) |
| `CommandSchedulerListener` | interface | Observer for scheduled/finished/canceled events | [Scheduler](../core/scheduler.md#lifecycle-listeners) |

### `com.vcs.valleylib.core.subsystem`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `Subsystem` | abstract class | Hardware unit base: `periodic()`, `simulationPeriodic()`, default commands, and requirement-carrying factories (`runOnce`, `run`, `startEnd`, `runEnd`) | [Subsystems](../core/subsystems.md) |

### `com.vcs.valleylib.core.auto`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `AutoDsl` / `AutoDsl.AutoBuilder` | builder | Declarative autonomous sequences: `command/add`, `run/doInstant`, `waitSeconds/waitFor`, `when`, `either/ifElse`, `parallel`, `race`, `deadline`, `marker` | [AutoDsl](../core/auto-dsl.md) |

## valleyLib-ftc

### `com.vcs.valleylib.ftc`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `RobotContainer` | abstract class | Central home for subsystems, bindings, and the auto command | [RobotContainer](../ftc/robot-container.md) |

### `com.vcs.valleylib.ftc.opmode`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `CommandOpMode` | abstract OpMode | Runs triggers + scheduler + telemetry each loop; safe shutdown | [CommandOpMode](../ftc/command-opmode.md) |

### `com.vcs.valleylib.ftc.input`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `CommandGamepad` | class | FTC gamepad wrapper (Xbox layout) + `forLogitechF310` / `forDualShockLike` presets | [Gamepads](../ftc/gamepads.md) |
| `CommandXboxLike` | class | Supplier-based base: all buttons incl. start/back/guide/stick clicks, PS aliases, deadband + curve shaping | [Gamepads](../ftc/gamepads.md) |
| `GamepadEx` | class | Backward-compatible alias for `CommandGamepad` | [Gamepads](../ftc/gamepads.md) |
| `Trigger` | class | Condition → command bindings (`onTrue`, `whileTrue`, `toggleOnTrue`, ...) + `and/or/negate/debounce`; implements `BooleanSupplier`; auto-registers on bind | [Triggers](../ftc/triggers.md) |
| `TriggerManager` | class | Polled binding registry; `getDefault()` shared instance, `bind`, `bindAll`, `poll`, `clear` | [Triggers](../ftc/triggers.md#polling-triggermanager) |

### `com.vcs.valleylib.ftc.hardware`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `HardwareDevice` | interface | `disable()`, `getDeviceType()` | [Hardware](../ftc/hardware.md) |
| `FtcSubsystem` | abstract class | `Subsystem` + `HardwareMap` access | [Subsystems](../core/subsystems.md) |
| `Motor` | class | DcMotor wrapper: run modes, GoBILDA presets, encoder, PID+FF velocity/position control | [Hardware](../ftc/hardware.md) |
| `Motor.Encoder` | inner class | Position/velocity/acceleration, distance-per-pulse, overflow correction | [Hardware](../ftc/hardware.md#the-encoder-inner-class) |
| `Motor.GoBILDA` / `Motor.RunMode` / `Motor.Direction` / `Motor.ZeroPowerBehavior` | enums | Motor specs and mode selectors | [Hardware](../ftc/hardware.md) |
| `MotorEx` | class | `DcMotorEx`-backed motor with `setVelocity` (ticks/s or angular) | [Hardware](../ftc/hardware.md#motorex) |
| `MotorGroup` | class | Leader/follower motor group; acts as a single `Motor`; iterable | [Hardware](../ftc/hardware.md#motorgroup) |

### `com.vcs.valleylib.ftc.control`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `PIDFController` | class | Full PIDF loop with tolerances, anti-windup, real-time dt | [Control](../ftc/control.md) |
| `PIDController` | class | PIDF with kF = 0, plus `setPID` | [Control](../ftc/control.md) |
| `PDController` | class | PID with kI = 0 | [Control](../ftc/control.md) |
| `PController` | class | PD with kD = 0 | [Control](../ftc/control.md) |
| `wpilibcontroller.SimpleMotorFeedforward` | class | `ks·sgn(v) + kv·v + ka·a` + achievable-velocity/accel helpers | [Control](../ftc/control.md#simplemotorfeedforward) |

### `com.vcs.valleylib.ftc.command`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `TimedCommand` | abstract class | Fixed-duration command with `onStart`/`onLoop`/`onEnd` hooks | [Commands](../core/commands.md#timedcommand-valleylib-ftc) |

### `com.vcs.valleylib.ftc.telemetry` / `.logging`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `FtcTelemetryBus` | class | One `put(...)` → Driver Station + Panels; `update()`, `clear()` | [Telemetry](../ftc/telemetry.md) |
| `FtcCommandLogger` | class | Scheduler listener that logs lifecycle events to telemetry | [Telemetry](../ftc/telemetry.md#command-lifecycle-logging) |

### `com.vcs.valleylib.ftc.pedro`

| Type | Kind | Summary | Docs |
| ---- | ---- | ------- | ---- |
| `PedroSubsystem` | abstract class | Owns the `Follower`; auto-updates it in `periodic()`; command factories | [Pedro](../pedro/overview.md) |
| `FollowPathCommand` | class | Follows one `PathChain`; requires the drive subsystem | [Pedro](../pedro/overview.md#followpathcommand) |
| `PedroCommands` | final class | Static factories: `follow`, `waitUntilIdle`, `followSequence` | [Pedro](../pedro/overview.md#pedrocommands) |
| `PedroAutoDsl` / `.Builder` | builder | Path-first autonomous DSL | [PedroAutoDsl](../pedro/auto-dsl.md) |

### `com.vcs.valleylib.ftc.samples`

| Type | Summary | Docs |
| ---- | ------- | ---- |
| `SampleTeleOp`, `SimpleRobot`, `hardware.SampleDriveHardware`, `hardware.SampleIntakeHardware`, `auto.SampleAutos`, `auto.PedroMigrationSample` | Copy-ready starter code | [Samples](../guides/samples.md) |
