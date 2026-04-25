package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.CommandWrapper;

public class RepeatCommand extends CommandWrapper {

    public RepeatCommand(Command inner) {
        super(inner);
    }

    @Override
    protected void onExecute() {
        inner.execute();
        if (inner.isFinished()) {
            inner.end(false);
            inner.initialize();
        }
    }

    @Override
    protected boolean onIsFinished() {
        return false;
    }

}