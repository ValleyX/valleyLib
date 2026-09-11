package com.vcs.valleylib.core.command;

import com.vcs.valleylib.core.time.RobotClock;

/**
 * A command that does nothing for a fixed duration.
 *
 * Commonly used in autonomous command sequences. Timing comes from
 * {@link RobotClock}, so it is deterministic under a manual clock in tests.
 */
public class WaitCommand implements Command {

    private final long waitNanos;
    private long startNanos;

    /**
     * @param seconds duration to wait
     */
    public WaitCommand(double seconds) {
        this.waitNanos = (long) (seconds * 1e9);
    }

    @Override
    public void initialize() {
        startNanos = RobotClock.nanos();
    }

    @Override
    public void execute() {}

    @Override
    public boolean isFinished() {
        return RobotClock.nanos() - startNanos >= waitNanos;
    }

    @Override
    public String getName() {
        return "Wait(" + (waitNanos / 1e9) + "s)";
    }
}
