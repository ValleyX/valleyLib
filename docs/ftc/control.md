# PID & Feedforward Control

`com.vcs.valleylib.ftc.control` contains a family of feedback controllers plus WPILib's `SimpleMotorFeedforward`. These are the same controllers the [`Motor` wrapper](hardware.md) uses internally for velocity and position modes — and you can use them standalone for arms, turrets, heading locks, or anything else.

## The controller family

The controllers form a simple inheritance chain — each one is the more general controller with some gains fixed to zero:

```
PIDFController        u(t) = kP·e + kI·∫e + kD·ė + kF·setpoint
   └── PIDController      (kF = 0)
         └── PDController     (kI = 0)
               └── PController    (kI = kD = 0)
```

Construct whichever matches the gains you actually use:

```java
PIDFController pidf = new PIDFController(kP, kI, kD, kF);
PIDController  pid  = new PIDController(kP, kI, kD);
PDController   pd   = new PDController(kP, kD);
PController    p    = new PController(kP);
```

Each also has an extended constructor taking an initial setpoint and measured value, e.g. `new PIDController(kP, kI, kD, setpoint, measured)`.

## Using a controller

The standard loop pattern:

```java
PIDController heading = new PIDController(0.02, 0, 0.002);
heading.setSetPoint(90);          // target degrees
heading.setTolerance(1.5);        // "close enough" band

while (!heading.atSetPoint()) {
    double output = heading.calculate(imu.getHeading());
    drive.turn(output);
}
drive.stop();
```

`calculate` has three overloads:

| Call | Meaning |
| ---- | ------- |
| `calculate(pv)` | New measurement, existing setpoint |
| `calculate(pv, sp)` | New measurement **and** new setpoint |
| `calculate()` | Re-run with the last measurement |

Timing is handled internally: the controller measures the real elapsed time between `calculate` calls (from `System.nanoTime()`) and uses it for the integral and derivative terms, so irregular loop times don't skew the math.

!!! warning "One controller per loop rate"
    Because the controller keeps its own clock, call `calculate` at a steady cadence (once per OpMode loop) and call `reset()` whenever you start using a controller again after a pause — otherwise the first derivative/integral step sees a huge dt.

## Full `PIDFController` API

### Setpoint & completion

| Method | Description |
| ------ | ----------- |
| `setSetPoint(sp)` / `getSetPoint()` | Target value |
| `setTolerance(pos)` / `setTolerance(pos, vel)` | Acceptable position (and velocity) error |
| `atSetPoint()` | True when both errors are within tolerance |
| `getPositionError()` / `getVelocityError()` | Current e(t) and ė(t) |
| `getTolerance()` | `[positionTolerance, velocityTolerance]` |

### Gains

| Method | Description |
| ------ | ----------- |
| `setPIDF(kp, ki, kd, kf)` | Set all four gains |
| `setP / setI / setD / setF` | Set individually |
| `getP / getI / getD / getF` | Read individually |
| `getCoefficients()` | `[kP, kI, kD, kF]` |
| `setPID(kp, ki, kd)` | (`PIDController` only) set with kF pinned to 0 |

### Integral management & state

| Method | Description |
| ------ | ----------- |
| `setIntegrationBounds(min, max)` | Clamp the accumulated integral (anti-windup; defaults −1..1) |
| `clearTotalError()` | Zero the integral accumulator only |
| `reset()` | Zero integral, previous error, and the internal clock |
| `getPeriod()` | Seconds between the last two `calculate` calls |

!!! tip "Anti-windup matters"
    If your mechanism saturates (output pinned at ±1 while far from target), the integral term can "wind up" and badly overshoot. The default ±1 integration bounds guard against this; tighten them if you see overshoot with a nonzero `kI`.

### About `kF`

In this controller, the feedforward term is `kF * setpoint` — useful for velocity control where required output is roughly proportional to target velocity. For more physical feedforward (static friction + acceleration), combine with `SimpleMotorFeedforward` below.

## `SimpleMotorFeedforward`

`com.vcs.valleylib.ftc.control.wpilibcontroller.SimpleMotorFeedforward` is WPILib's permanent-magnet DC motor feedforward model:

```
output = ks · sgn(v) + kv · v + ka · a
```

- **ks** — static gain: minimum output to overcome friction
- **kv** — velocity gain: output per unit of velocity
- **ka** — acceleration gain: output per unit of acceleration

```java
SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.9, 0.45, 0.1); // ks, kv, ka

double out = ff.calculate(targetVelocity, targetAccel);
double out2 = ff.calculate(targetVelocity);   // acceleration assumed 0
```

The gains' units dictate the output units — characterize your mechanism (e.g., with a ramp test) and the same gains work across battery voltages far better than pure PID.

### Constraint helpers

For trapezoidal-profile planning, the class can invert its own model:

| Method | Answers |
| ------ | ------- |
| `maxAchievableVelocity(maxVoltage, accel)` | Fastest sustainable velocity at a given acceleration |
| `minAchievableVelocity(maxVoltage, accel)` | Most negative velocity at a given acceleration |
| `maxAchievableAcceleration(maxVoltage, velocity)` | Hardest achievable acceleration at a given velocity |
| `minAchievableAcceleration(maxVoltage, velocity)` | Hardest achievable deceleration at a given velocity |

## PID + feedforward together

The recommended pattern — feedforward does the bulk of the work, PID trims the error:

```java
PIDController pid = new PIDController(0.002, 0, 0);
SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.9, 0.45);

double output = ff.calculate(targetVelocity)
              + pid.calculate(measuredVelocity, targetVelocity);
motor.set(output / MAX_OUTPUT);
```

This is exactly what `Motor`'s `VelocityControl` run mode does internally — see [Hardware](hardware.md#run-modes).
