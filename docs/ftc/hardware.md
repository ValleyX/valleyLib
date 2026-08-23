# Hardware: Motors & Encoders

The `com.vcs.valleylib.ftc.hardware` package provides FTCLib-style wrappers over the FTC SDK's `DcMotor`, adding closed-loop velocity/position control, a smarter encoder, and motor grouping.

| Class | Role |
| ----- | ---- |
| `HardwareDevice` | Minimal device interface: `disable()`, `getDeviceType()` |
| `FtcSubsystem` | Subsystem base with `HardwareMap` access (see [Subsystems](../core/subsystems.md)) |
| `Motor` | The core wrapper: run modes, encoder, PID + feedforward |
| `MotorEx` | Extends `Motor` using `DcMotorEx` for higher-fidelity velocity |
| `MotorGroup` | Leader/follower group that acts as a single `Motor` |

## Creating a motor

```java
// Plain — uses the motor type configured on the Robot Controller
Motor lift = new Motor(hardwareMap, "lift");

// GoBILDA preset — sets CPR/RPM from the 5202-series catalog
Motor drive = new Motor(hardwareMap, "leftFront", Motor.GoBILDA.RPM_312);

// Custom specs — any motor, given counts-per-rev and RPM
Motor custom = new Motor(hardwareMap, "arm", 383.6, 435);
```

### GoBILDA presets

`Motor.GoBILDA` covers the 5202/5203/5204 series:

| Preset | CPR | RPM |
| ------ | --- | --- |
| `RPM_30` | 5264 | 30 |
| `RPM_43` | 3892 | 43 |
| `RPM_60` | 2786 | 60 |
| `RPM_84` | 1993.6 | 84 |
| `RPM_117` | 1425.2 | 117 |
| `RPM_223` | 753.2 | 223 |
| `RPM_312` | 537.6 | 312 |
| `RPM_435` | 383.6 | 435 |
| `RPM_1150` | 145.6 | 1150 |
| `RPM_1620` | 103.6 | 1620 |
| `BARE` | 28 | 6000 |
| `NONE` | — | — (fall back to RC config) |

Each preset exposes `getCPR()`, `getRPM()`, and `getAchievableMaxTicksPerSecond()`.

## Run modes

`Motor.RunMode` selects how `set(double)` is interpreted:

=== "RawPower"

    ```java
    motor.setRunMode(Motor.RunMode.RawPower);
    motor.set(0.75);   // straight power, -1..1
    ```

    Direct passthrough to `DcMotor.setPower`.

=== "VelocityControl"

    ```java
    motor.setRunMode(Motor.RunMode.VelocityControl);
    motor.setVeloCoefficients(0.05, 0, 0);          // PID on velocity
    motor.setFeedforwardCoefficients(0.92, 0.47);   // ks, kv (optionally ka)
    motor.set(0.5);    // target 50% of max achievable velocity
    ```

    Output is treated as a fraction of `ACHIEVABLE_MAX_TICKS_PER_SECOND`, tracked by the internal velocity PID **plus** a `SimpleMotorFeedforward`. A *buffer* (default `0.9`) scales the ceiling so the controller has headroom — change it with `setBuffer(0 < f ≤ 1)`.

=== "PositionControl"

    ```java
    motor.setRunMode(Motor.RunMode.PositionControl);
    motor.setPositionCoefficient(0.05);   // P gain
    motor.setTargetPosition(1200);        // ticks (or setTargetDistance in units)
    motor.setPositionTolerance(15);

    while (!motor.atTargetPosition()) {
        motor.set(0.4);                   // capped correction power
    }
    motor.stopMotor();
    ```

    A P controller on position produces an error term; `set(power)` scales it, so the passed value acts as a max-power cap. Switching into `PositionControl` with no target set defaults the target to the current position (so the motor holds instead of jumping).

!!! note
    `setRunMode(...)` resets the internal velocity and position controllers.

## The `Motor` API

### Output & state

| Method | Description |
| ------ | ----------- |
| `set(double output)` | Set output according to the current run mode |
| `get()` | Current set power (−1..1) |
| `stopMotor()` | Set power to 0 (can `set(...)` again afterwards) |
| `disable()` | Close the underlying device |
| `setInverted(boolean)` / `getInverted()` | Motor direction |
| `setZeroPowerBehavior(BRAKE / FLOAT / UNKNOWN)` | Behavior at zero power |
| `getDeviceType()` | Human-readable device description |

### Encoder & measurement

| Method | Description |
| ------ | ----------- |
| `getCurrentPosition()` | Position in ticks (via the wrapper encoder) |
| `getDistance()` / `getRate()` | Position / velocity in **units** (see distance-per-pulse) |
| `getCorrectedVelocity()` | Velocity corrected for the SDK's 16-bit overflow |
| `resetEncoder()` | Zero the wrapper encoder without stopping the motor |
| `stopAndResetEncoder()` | Hardware reset via `STOP_AND_RESET_ENCODER` |
| `setDistancePerPulse(dpp)` | Scale ticks into real units (returns the `Encoder`) |
| `getCPR()` / `getMaxRPM()` | Motor specs (preset or RC config) |

### Control configuration

| Method | Description |
| ------ | ----------- |
| `setVeloCoefficients(kp, ki, kd)` | Velocity PID gains |
| `setFeedforwardCoefficients(ks, kv[, ka])` | Feedforward gains |
| `setPositionCoefficient(kp)` | Position P gain |
| `setTargetPosition(ticks)` / `setTargetDistance(units)` | Position target |
| `setPositionTolerance(tol)` / `atTargetPosition()` | Completion check |
| `setBuffer(fraction)` | Velocity-mode headroom (default 0.9) |
| `getVeloCoefficients()` / `getPositionCoefficient()` / `getFeedforwardCoefficients()` | Read back gains |

## The `Encoder` inner class

Every `Motor` owns a `motor.encoder` — a software wrapper over the SDK's tick count that adds:

| Method | Description |
| ------ | ----------- |
| `getPosition()` | Ticks (direction- and reset-adjusted); also refreshes the velocity estimate |
| `getDistance()` / `getRate()` | Position / velocity scaled by distance-per-pulse |
| `getRevolutions()` | Position in full motor revolutions |
| `getRawVelocity()` | SDK-reported velocity; also refreshes the acceleration estimate |
| `getCorrectedVelocity()` | Velocity with 16-bit overflow compensation |
| `getAcceleration()` | Estimated acceleration (ticks/s²) |
| `reset()` | Software zero (no motor stop needed) |
| `setDirection(FORWARD / REVERSE)` | Reading direction, independent of motor direction |
| `setDistancePerPulse(dpp)` | Units per tick |

!!! info "Why 'corrected' velocity?"
    The FTC SDK reports velocity as a 16-bit value, which overflows above ±32767 ticks/s (easily reached by high-RPM motors). `getCorrectedVelocity()` reconstructs the true value using the wrapper's own velocity estimate.

## `MotorEx`

`MotorEx` extends `Motor` but talks to `DcMotorEx` directly, and closes its velocity loop on the **overflow-corrected** velocity. It adds:

```java
MotorEx shooter = new MotorEx(hardwareMap, "shooter", Motor.GoBILDA.BARE);

shooter.setRunMode(Motor.RunMode.VelocityControl);
shooter.setVelocity(2200);                          // ticks per second
shooter.setVelocity(Math.PI * 2, AngleUnit.RADIANS); // or angular rate
double v = shooter.getVelocity();                   // ticks per second
double a = shooter.getAcceleration();               // ticks per second²
```

Prefer `MotorEx` whenever the motor has an encoder plugged in — it's a drop-in replacement.

## `MotorGroup`

`MotorGroup` links motors that drive one mechanism (e.g., a two-motor lift) into a single object that **is a `Motor`** — pass it anywhere a `Motor` goes.

```java
MotorGroup lift = new MotorGroup(
    new Motor(hardwareMap, "liftLeft", Motor.GoBILDA.RPM_312),   // leader
    new Motor(hardwareMap, "liftRight", Motor.GoBILDA.RPM_312)   // follower(s)
);

lift.setRunMode(Motor.RunMode.PositionControl);
lift.setTargetPosition(1400);
lift.set(0.6);
```

Semantics:

- The **leader** (first constructor argument) does all closed-loop math; followers mirror the leader's output power.
- Group-wide operations: `set`, `setInverted`, `setZeroPowerBehavior`, `setDistancePerPulse`, `disable`, `stopMotor`.
- Leader-delegated operations: run mode, targets, tolerances, gains, encoder resets, `atTargetPosition`, `getVelocity`.
- Introspection: `getSpeeds()`, `getVelocities()`, `getPositions()` return per-motor lists; the group is also `Iterable<Motor>`.

!!! warning
    Motors in a group should be mechanically linked and identically geared. If one motor must spin opposite the other, call `setInverted(true)` on that individual `Motor` **before** constructing the group.
