package com.vcs.valleylib.core.time;

/**
 * A clock that only moves when told to. Install it with
 * {@link RobotClock#setClock(Clock)} to make time-based commands
 * deterministic in tests and desktop simulation.
 *
 * <pre>{@code
 * ManualClock clock = new ManualClock();
 * RobotClock.setClock(clock);
 *
 * Command wait = Commands.waitSeconds(0.5);
 * wait.initialize();
 * clock.advance(0.49);
 * assertFalse(wait.isFinished());
 * clock.advance(0.01);
 * assertTrue(wait.isFinished());
 * }</pre>
 */
public final class ManualClock implements Clock {

    private long nanos;

    /**
     * Creates a clock starting at zero.
     */
    public ManualClock() {
        this(0);
    }

    /**
     * Creates a clock starting at the given time in seconds.
     */
    public ManualClock(double seconds) {
        set(seconds);
    }

    @Override
    public long nanos() {
        return nanos;
    }

    /**
     * Moves the clock forward.
     *
     * @param seconds amount to advance (must be non-negative)
     */
    public void advance(double seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Cannot advance a clock backwards");
        }
        nanos += (long) (seconds * 1e9);
    }

    /**
     * Moves the clock forward by a number of nanoseconds.
     */
    public void advanceNanos(long deltaNanos) {
        if (deltaNanos < 0) {
            throw new IllegalArgumentException("Cannot advance a clock backwards");
        }
        nanos += deltaNanos;
    }

    /**
     * Sets the absolute time in seconds.
     */
    public void set(double seconds) {
        nanos = (long) (seconds * 1e9);
    }
}
