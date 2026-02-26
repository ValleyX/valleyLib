package com.vcs.valleylib.core.command.group;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runs multiple commands simultaneously.
 *
 * The group finishes when ALL commands have finished.
 */
public class ParallelCommandGroup implements Command {

    private final Set<Command> commands = new HashSet<>();

    public ParallelCommandGroup(Command... commands) {
        this.commands.addAll(java.util.List.of(commands));
    }

    @Override
    public void initialize() {
        for (Command c : commands) {
            c.initialize();
        }
    }

    @Override
    public void execute() {
        commands.removeIf(command -> {
            command.execute();
            if (command.isFinished()) {
                command.end(false);
                return true;
            }
            return false;
        });
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            for (Command command : commands) {
                command.end(true);
            }
            commands.clear();
        }
    }

    @Override
    public Set<Subsystem> getRequirements() {
        Set<Subsystem> requirements = new LinkedHashSet<>();
        for (Command command : commands) {
            requirements.addAll(command.getRequirements());
        }
        return requirements;
    }

    @Override
    public boolean isFinished() {
        return commands.isEmpty();
    }
}
