package com.vcs.valleylib.core.command.group;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs commands one after another in sequence.
 *
 * The next command does not start until the current one finishes.
 */
public class SequentialCommandGroup implements Command {

    private final List<Command> commands;
    private Queue<Command> queue;
    private Command current;

    public SequentialCommandGroup(Command... commands) {
        this.commands = List.of(commands);
    }

    @Override
    public void initialize() {
        queue = new ArrayDeque<>(commands);
        current = queue.poll();
        if (current != null) {
            current.initialize();
        }
    }

    @Override
    public void execute() {
        if (current == null) {
            return;
        }

        current.execute();
        if (current.isFinished()) {
            current.end(false);
            current = queue.poll();
            if (current != null) {
                current.initialize();
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted && current != null) {
            current.end(true);
        }
        current = null;
        if (queue != null) {
            queue.clear();
        }
    }

    @Override
    public boolean isFinished() {
        return current == null;
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return commands.stream()
                .flatMap(command -> command.getRequirements().stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
