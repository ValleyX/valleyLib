package com.vcs.valleylib.core.time;

/**
 * A monotonic time source.
 * <p>
 * The library never reads {@code System.nanoTime()} directly; it goes
 * through {@link RobotClock}, which delegates to the installed Clock. On
 * the robot that is the system clock. In desktop tests and simulation a
 * {@link ManualClock} can be installed to make every timeout, wait,
 * debounce, and dwell-time transition deterministic.
 */
@FunctionalInterface
public interface Clock {

    /**
     * @return monotonic time in nanoseconds
     */
    long nanos();

    /**
     * @return monotonic time in seconds
     */
    default double seconds() {
        return nanos() / 1e9;
    }
}
