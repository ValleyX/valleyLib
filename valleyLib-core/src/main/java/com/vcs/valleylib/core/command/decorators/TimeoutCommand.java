package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.CommandWrapper;
import com.vcs.valleylib.core.time.RobotClock;

public class TimeoutCommand extends CommandWrapper {

    private final long timeoutNanos;
    private long startNanos;

    public TimeoutCommand(Command inner, long timeoutMillis) {
        super(inner);
        this.timeoutNanos = timeoutMillis * 1_000_000L;
    }

    @Override
    protected void onInitialize() {
        startNanos = RobotClock.nanos();
        super.onInitialize();
    }

    @Override
    protected boolean onIsFinished() {
        return inner.isFinished() ||
                RobotClock.nanos() - startNanos >= timeoutNanos;
    }

    @Override
    protected void onEnd(boolean interrupted) {
        // A command cut short by the timeout is interrupted, not finished.
        inner.end(interrupted || !inner.isFinished());
    }
}