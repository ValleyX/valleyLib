package com.vcs.valleylib.core.command;

import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.HashSet;
import java.util.Set;

public abstract class CommandWrapper extends BaseCommand {

    protected final Command inner;

    protected CommandWrapper(Command inner) {
        this.inner = inner;
    }

    @Override
    protected void onInitialize() {
        inner.initialize();
    }

    @Override
    protected void onExecute() {
        inner.execute();
    }

    @Override
    protected void onEnd(boolean interrupted) {
        inner.end(interrupted);
    }

    @Override
    protected boolean onIsFinished() {
        return inner.isFinished();
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return new HashSet<>(inner.getRequirements());
    }
}