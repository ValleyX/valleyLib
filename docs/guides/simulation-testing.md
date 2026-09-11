# Desktop Simulation & Testing

Because `valleyLib-core` has **zero** Android or FTC SDK dependencies, your command logic can run on a laptop: JUnit tests, desktop simulations, and CI — no robot, no phone, no Android Studio deploy cycle.

## What runs where

| | Desktop JVM | Robot only |
| --- | --- | --- |
| `Command`, groups, decorators | ✅ | |
| `CommandScheduler`, listeners | ✅ | |
| `Subsystem` (+ `simulationPeriodic`) | ✅ | |
| `AutoDsl`, `StateMachine` | ✅ | |
| `RobotClock` / `ManualClock` (controllable time) | ✅ | |
| `CommandXboxLike` (supplier-based input), `Trigger` | ✅ | |
| `Motor`, `CommandOpMode`, Pedro, telemetry | | ✅ (needs FTC SDK) |

The trick is keeping your *logic* in core-only terms and injecting hardware behind interfaces or supplier functions.

## Unit-testing commands

Add `valleyLib-core` to a plain JVM test module and drive the scheduler by hand:

```java
class AutoRoutineTest {

    private final CommandScheduler scheduler = CommandScheduler.getInstance();

    @BeforeEach
    void setUp() {
        scheduler.reset();     // full isolation between tests
    }

    @Test
    void sequenceRunsInOrder() {
        List<String> log = new ArrayList<>();

        Command auto = Commands.sequence(
            Commands.runOnce(() -> log.add("first")),
            Commands.runOnce(() -> log.add("second"))
        );

        scheduler.schedule(auto);
        scheduler.run();   // one tick: first step
        scheduler.run();   // second step

        assertEquals(List.of("first", "second"), log);
    }
}
```

Each `scheduler.run()` call is one loop tick — deterministic and fast. The library's own test suite (`CommandSchedulerTest`, `CommandGroupTest`, `DecoratorSemanticsTest`, `StateMachineTest`, `ClockTest`, `TriggerTest`, ...) uses exactly this pattern; read them for more examples.

## Controlling time

Every time-based behavior in the library — `WaitCommand`, `withTimeout`, `StateMachine.transitionAfter`/`getTimeInState`, `Trigger.debounce`, `TimedCommand`, the `PIDFController` period, and `Motor.Encoder` velocity estimation — reads the clock through `RobotClock` (in `com.vcs.valleylib.core.time`) rather than `System.nanoTime()`. On the robot that is the system clock. In tests, install a `ManualClock` and move time yourself:

```java
class TimingTest {
    private final ManualClock clock = new ManualClock();

    @BeforeEach void setUp()    { RobotClock.setClock(clock); }
    @AfterEach  void tearDown() { RobotClock.useSystemClock(); }

    @Test
    void shooterSpinsUpForHalfASecond() {
        Command spinUp = shooter.runEnd(shooter::spin, shooter::hold).withTimeout(0.5);
        spinUp.initialize();

        clock.advance(0.49);
        spinUp.execute();
        assertFalse(spinUp.isFinished());

        clock.advance(0.01);
        spinUp.execute();
        assertTrue(spinUp.isFinished());   // exactly on the 500 ms boundary
    }
}
```

| `RobotClock` | Purpose |
| ------------ | ------- |
| `nanos()` / `seconds()` / `millis()` | Read the installed clock |
| `setClock(Clock)` | Install any `Clock` (a `long nanos()` functional interface) |
| `useSystemClock()` | Restore the default |
| `isSystemClock()` | Whether time is real or simulated |

| `ManualClock` | Purpose |
| ------------- | ------- |
| `advance(seconds)` / `advanceNanos(n)` | Move forward (never backwards) |
| `set(seconds)` | Jump to an absolute time |

!!! tip "Simulation loops"
    In a desktop simulation, advance the manual clock by your loop period (e.g. `clock.advance(0.02)`) before each `scheduler.run()` — timeouts, dwell transitions, and debounces then behave exactly as they will at 50 Hz on the robot, but you can run thousands of cycles per second.

No test should ever need `Thread.sleep` for library timing. Always restore the system clock in `@AfterEach`; `RobotClock` is process-wide.

## Simulation hooks

Subsystems have a second periodic method that only runs in simulation:

```java
public class SimulatedLift extends Subsystem {
    private double positionTicks = 0;
    private double commandedPower = 0;

    public void setPower(double power) { commandedPower = power; }
    public double getPosition() { return positionTicks; }

    @Override
    public void simulationPeriodic() {
        // Toy physics: 500 ticks/s at full power, 20 ms ticks
        positionTicks += commandedPower * 500 * 0.020;
    }
}
```

Two ways to run the hooks:

=== "Integrated"

    ```java
    scheduler.setSimulationEnabled(true);
    while (running) {
        scheduler.run();   // simulationPeriodic() runs inside each tick
    }
    ```

    Best for end-to-end simulation: commands, default commands, and physics all advance together.

=== "Explicit stepping"

    ```java
    scheduler.runSimulationStep();   // ONLY the simulationPeriodic() hooks
    ```

    Best for tests that want to advance the model without executing commands.

## Simulated input

`CommandXboxLike` takes plain suppliers, so a fake gamepad for tests is trivial:

```java
AtomicBoolean aPressed = new AtomicBoolean(false);

CommandXboxLike pad = new CommandXboxLike(
    aPressed::get, () -> false, () -> false, () -> false,
    () -> false, () -> false,
    () -> false, () -> false, () -> false, () -> false,
    () -> 0, () -> 0, () -> 0, () -> 0, () -> 0, () -> 0);

TriggerManager triggers = new TriggerManager();
triggers.bind(pad.a().onTrue(Commands.runOnce(() -> log.add("fired"))));

aPressed.set(true);
triggers.poll();          // rising edge → command scheduled
scheduler.run();          // command executes
```

## A recommended workflow

1. **Model** mechanisms as core `Subsystem`s with `simulationPeriodic()` physics.
2. **Write** your autonomous with `AutoDsl`/command composition against those subsystems.
3. **Test** the routine on desktop until the sequencing is right.
4. **Swap** the simulated subsystems for `FtcSubsystem` implementations that drive real hardware — the commands don't change.

This catches sequencing bugs, missing requirements, and logic errors in seconds instead of practice-field minutes.
