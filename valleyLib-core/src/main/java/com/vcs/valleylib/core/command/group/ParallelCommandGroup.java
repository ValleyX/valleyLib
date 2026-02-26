package com.vcs.valleylib.core.command.group;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs multiple commands simultaneously.
 *
 * The group finishes when ALL commands have finished.
 */
public class ParallelCommandGroup implements Command {

    private final List<Command> allCommands;
    private final Set<Command> activeCommands = new HashSet<>();

    public ParallelCommandGroup(Command... commands) {
        this.allCommands = List.of(commands);
    }

    @Override
    public void initialize() {
        activeCommands.clear();
        activeCommands.addAll(allCommands);
        for (Command command : activeCommands) {
            command.initialize();
        }
    }

    @Override
    public void execute() {
        activeCommands.removeIf(command -> {
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
            for (Command command : activeCommands) {
                command.end(true);
            }
        }
        activeCommands.clear();
    }

    @Override
    public boolean isFinished() {
        return activeCommands.isEmpty();
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return allCommands.stream()
                .flatMap(command -> command.getRequirements().stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
