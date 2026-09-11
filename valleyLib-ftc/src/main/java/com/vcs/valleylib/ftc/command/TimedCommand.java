package com.vcs.valleylib.ftc.command;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.time.RobotClock;

/**
 * Command that runs for a fixed duration.
 * Preferred over raw timer usage in autos. Timing comes from
 * {@link RobotClock}, so it is deterministic under a manual clock in tests.
 */
public abstract class TimedCommand implements Command {

    private final double durationSeconds;
    private double startSeconds;

    protected TimedCommand(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void initialize() {
        startSeconds = RobotClock.seconds();
        onStart();
    }

    @Override
    public void execute() {
        onLoop(elapsedSeconds());
    }

    @Override
    public boolean isFinished() {
        return elapsedSeconds() >= durationSeconds;
    }

    @Override
    public void end(boolean interrupted) {
        onEnd(interrupted);
    }

    /**
     * @return seconds since this command was initialized
     */
    protected double elapsedSeconds() {
        return RobotClock.seconds() - startSeconds;
    }

    protected abstract void onStart();
    protected abstract void onLoop(double elapsedSeconds);
    protected abstract void onEnd(boolean interrupted);
}
