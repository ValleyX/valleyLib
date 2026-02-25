package com.vcs.valleylib.core.command;

/**
 * A command that runs a single action once and then immediately finishes.
 *
 * Useful for:
 *  - toggles
 *  - setting states
 *  - one-shot hardware actions
 */
public class InstantCommand implements Command {

    private final Runnable action;
    private boolean hasRun = false;

    public InstantCommand(Runnable action) {
        this.action = action;
    }

    @Override
    public void execute() {
        if (!hasRun) {
            action.run();
            hasRun = true;
        }
    }

    @Override
    public boolean isFinished() {
        return hasRun;
    }
}