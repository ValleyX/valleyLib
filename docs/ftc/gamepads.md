# Gamepads & Input Shaping

The `com.vcs.valleylib.ftc.input` package wraps the FTC `Gamepad` with WPILib-style trigger factories, configurable deadbands, and response-curve shaping.

## Class overview

| Class | Role |
| ----- | ---- |
| `CommandGamepad` | The class you use — wraps an FTC `Gamepad` in the Xbox layout |
| `CommandXboxLike` | The base class: button trigger factories + axis shaping (works with any suppliers) |
| `GamepadEx` | Backward-compatible alias for `CommandGamepad` (FTCLib naming) |

```java
CommandGamepad driver = new CommandGamepad(gamepad1);
```

## Presets

Two factory presets return a gamepad pre-tuned with sensible deadbands and curves:

```java
// Logitech F310 in XInput mode
CommandGamepad driver = CommandGamepad.forLogitechF310(gamepad1);

// PlayStation-style controllers
CommandGamepad operator = CommandGamepad.forDualShockLike(gamepad2);
```

| Preset | Stick deadband | Stick exponent | Trigger deadband | Trigger exponent |
| ------ | -------------- | -------------- | ---------------- | ---------------- |
| `forLogitechF310` | 0.08 | 1.7 | 0.05 | 1.5 |
| `forDualShockLike` | 0.07 | 1.6 | 0.04 | 1.4 |

## Buttons → Triggers

Every button method returns a [`Trigger`](triggers.md) you can bind commands to:

```java
driver.a().onTrue(claw.closeCommand());
driver.rightBumper().whileTrue(intake.runIn());
```

**Xbox naming:** `a()`, `b()`, `x()`, `y()`, `leftBumper()`, `rightBumper()`, `dpadUp()`, `dpadDown()`, `dpadLeft()`, `dpadRight()`, `start()`, `back()`, `guide()`, `leftStickButton()`, `rightStickButton()`

**PlayStation aliases** (map to the same physical buttons):

| PlayStation | Xbox |
| ----------- | ---- |
| `cross()` / `circle()` / `square()` / `triangle()` | `a()` / `b()` / `x()` / `y()` |
| `l1()` / `r1()` | `leftBumper()` / `rightBumper()` |
| `options()` / `share()` / `ps()` | `start()` / `back()` / `guide()` |
| `l3()` / `r3()` | `leftStickButton()` / `rightStickButton()` |

**Analog triggers as buttons:**

```java
// Fires when the trigger is pulled at least 35% (after shaping)
Trigger shoot = driver.rightTriggerButton(0.35);
Trigger brake = driver.leftTriggerButton(0.5);
```

## Axes

Axis methods return **shaped** values — deadbanded and curved:

```java
double forward = driver.leftY();
double turn    = driver.rightX();
double slow    = driver.leftTrigger();   // 0..1
```

Available: `leftX()`, `leftY()`, `rightX()`, `rightY()`, `leftTrigger()`, `rightTrigger()`.

!!! note "FTC stick convention"
    Like the raw FTC SDK, pushing a stick *up* gives a **negative** Y. Negate in your drive code if you want up = forward.

## How shaping works

Two knobs per input class, set fluently:

```java
CommandGamepad driver = new CommandGamepad(gamepad1)
        .withStickDeadband(0.10)     // ignore small stick noise
        .withStickExponent(2.0)      // stronger curve = finer low-speed control
        .withTriggerDeadband(0.05)
        .withTriggerExponent(1.5);
```

The shaping pipeline for sticks (signed inputs, −1..1):

1. Values inside the deadband become exactly `0`.
2. The remaining range is **renormalized** so output still spans the full 0..1 — no dead zone "jump" at the deadband edge.
3. The magnitude is raised to the exponent (`1.0` = linear; higher = more precision near center, same maximum at full deflection).
4. The original sign is restored.

Triggers use the same pipeline on the clamped 0..1 range without the sign step.

Defaults (plain constructor, no preset): stick deadband `0.08`, trigger deadband `0.05`, both exponents `1.0` (linear).

## Custom layouts with `CommandXboxLike`

`CommandXboxLike` takes plain `BooleanSupplier`/`DoubleSupplier` arguments, so you can build a "gamepad" from *any* input source — a keyboard shim in desktop simulation, a second gamepad's combined state, or a test harness:

```java
CommandXboxLike simPad = new CommandXboxLike(
    () -> keyboard.isDown('a'), () -> false, () -> false, () -> false,
    () -> false, () -> false,
    () -> false, () -> false, () -> false, () -> false,
    () -> joystickModel.x(), () -> joystickModel.y(),
    () -> 0, () -> 0, () -> 0, () -> 0
);
```

Two constructors exist: the 16-argument form above (start/back/guide and stick buttons default to never-pressed) and a full 21-argument form that also takes suppliers for `start`, `back`, `guide`, `leftStickButton`, and `rightStickButton`.

Everything documented above — triggers, aliases, shaping — works identically.
