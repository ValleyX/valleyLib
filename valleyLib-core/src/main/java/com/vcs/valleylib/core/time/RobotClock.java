package com.vcs.valleylib.core.time;

import java.util.Objects;

/**
 * Global access point for the library's time source.
 * <p>
 * Defaults to the system monotonic clock. Install a {@link ManualClock}
 * with {@link #setClock(Clock)} in tests to control time explicitly, and
 * restore the default with {@link #useSystemClock()} afterwards.
 * <p>
 * Like the scheduler, this is a process-wide setting intended for use from
 * a single thread.
 */
public final class RobotClock {

    private static final Clock SYSTEM_CLOCK = System::nanoTime;

    private static Clock clock = SYSTEM_CLOCK;

    private RobotClock() {}

    /**
     * @return current time from the installed clock, in nanoseconds
     */
    public static long nanos() {
        return clock.nanos();
    }

    /**
     * @return current time from the installed clock, in seconds
     */
    public static double seconds() {
        return clock.seconds();
    }

    /**
     * @return current time from the installed clock, in milliseconds
     */
    public static long millis() {
        return clock.nanos() / 1_000_000L;
    }

    /**
     * Installs a clock for all library timing.
     */
    public static void setClock(Clock newClock) {
        clock = Objects.requireNonNull(newClock, "clock");
    }

    /**
     * Restores the system monotonic clock.
     */
    public static void useSystemClock() {
        clock = SYSTEM_CLOCK;
    }

    /**
     * @return the installed clock
     */
    public static Clock getClock() {
        return clock;
    }

    /**
     * @return whether the system clock is installed (i.e. not simulated)
     */
    public static boolean isSystemClock() {
        return clock == SYSTEM_CLOCK;
    }
}
