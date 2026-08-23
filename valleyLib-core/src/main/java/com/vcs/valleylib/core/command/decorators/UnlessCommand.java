package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.CommandWrapper;

import java.util.function.BooleanSupplier;

public class UnlessCommand extends CommandWrapper {

    private final BooleanSupplier condition;
    private boolean skip;

    public UnlessCommand(Command inner, BooleanSupplier condition) {
        super(inner);
        this.condition = condition;
    }

    @Override
    protected void onInitialize() {
        skip = condition.getAsBoolean();
        if (!skip) super.onInitialize();
    }

    @Override
    protected void onExecute() {
        if (!skip) super.onExecute();
    }

    @Override
    protected boolean onIsFinished() {
        return skip || inner.isFinished();
    }

    @Override
    protected void onEnd(boolean interrupted) {
        // When skipped, the inner command never initialized — don't end() it.
        if (!skip) super.onEnd(interrupted);
    }
}