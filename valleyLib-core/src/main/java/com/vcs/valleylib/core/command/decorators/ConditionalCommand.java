package com.vcs.valleylib.core.command.decorators;

import androidx.annotation.NonNull;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * A command that runs one of two commands based on a condition at initialization.
 */
public class ConditionalCommand implements Command {

    private final BooleanSupplier condition;
    private final Command onTrue;
    private final Command onFalse;

    private Command active;

    public ConditionalCommand(BooleanSupplier condition, Command onTrue, Command onFalse) {
        this.condition = condition;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    @Override
    public void initialize() {
        active = condition.getAsBoolean() ? onTrue : onFalse;
        active.initialize();
    }

    @Override
    public void execute() {
        if (active != null) {
            active.execute();
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (active != null) {
            active.end(interrupted);
        }
    }

    @Override
    public boolean isFinished() {
        return active != null && active.isFinished();
    }

    @NonNull
    @Override
    public Set<Subsystem> getRequirements() {
        Set<Subsystem> requirements = new LinkedHashSet<>();
        requirements.addAll(onTrue.getRequirements());
        requirements.addAll(onFalse.getRequirements());
        return requirements;
    }
}
