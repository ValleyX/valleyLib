package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.CommandWrapper;

import java.util.function.BooleanSupplier;

public class OnlyWhileCommand extends CommandWrapper {

    private final BooleanSupplier condition;

    public OnlyWhileCommand(Command inner, BooleanSupplier condition) {
        super(inner);
        this.condition = condition;
    }

    @Override
    protected boolean onIsFinished() {
        return !condition.getAsBoolean() || inner.isFinished();
    }

    @Override
    protected void onEnd(boolean interrupted) {
        // A command cut short by the condition is interrupted, not finished.
        inner.end(interrupted || !inner.isFinished());
    }
}