package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.CommandWrapper;

import java.util.function.BooleanSupplier;

public class UntilCommand extends CommandWrapper {

    private final BooleanSupplier condition;

    public UntilCommand(Command inner, BooleanSupplier condition) {
        super(inner);
        this.condition = condition;
    }

    @Override
    protected boolean onIsFinished() {
        return inner.isFinished() || condition.getAsBoolean();
    }

    @Override
    protected void onEnd(boolean interrupted) {
        // A command cut short by the condition is interrupted, not finished.
        inner.end(interrupted || !inner.isFinished());
    }
}