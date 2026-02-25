package com.vcs.valleylib.core.command;

/**
 * A command that does nothing for a fixed duration.
 *
 * Commonly used in autonomous command sequences.
 */
public class WaitCommand implements Command {

    private final long waitTimeMs;
    private long startTime;

    /**
     * @param seconds duration to wait
     */
    public WaitCommand(double seconds) {
        this.waitTimeMs = (long) (seconds * 1000);
    }

    @Override
    public void initialize() {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void execute() {}

    @Override
    public boolean isFinished() {
        return System.currentTimeMillis() - startTime >= waitTimeMs;
    }
}