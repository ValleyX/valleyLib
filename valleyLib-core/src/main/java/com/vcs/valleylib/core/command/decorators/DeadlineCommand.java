package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.BaseCommand;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class DeadlineCommand extends BaseCommand {

    private final Command deadline;
    private final Command[] others;
    private final Set<Command> runningOthers = new LinkedHashSet<>();

    public DeadlineCommand(Command deadline, Command... others) {
        this.deadline = deadline;
        this.others = others;
    }

    @Override
    protected void onInitialize() {
        runningOthers.clear();
        runningOthers.addAll(Arrays.asList(others));
        deadline.initialize();
        for (Command c : others) c.initialize();
    }

    @Override
    protected void onExecute() {
        deadline.execute();
        // Others that finish early end naturally and stop executing.
        runningOthers.removeIf(c -> {
            c.execute();
            if (c.isFinished()) {
                c.end(false);
                return true;
            }
            return false;
        });
    }

    @Override
    protected boolean onIsFinished() {
        return deadline.isFinished();
    }

    @Override
    protected void onEnd(boolean interrupted) {
        deadline.end(interrupted || !deadline.isFinished());
        for (Command c : runningOthers) {
            c.end(true);
        }
        runningOthers.clear();
    }

    @Override
    public Set<Subsystem> getRequirements() {
        Set<Subsystem> requirements = new LinkedHashSet<>(deadline.getRequirements());
        Arrays.stream(others).forEach(command -> requirements.addAll(command.getRequirements()));
        return requirements;
    }
}
