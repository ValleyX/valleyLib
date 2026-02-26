package com.vcs.valleylib.core.command.group;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Runs commands one after another in sequence.
 *
 * The next command does not start until the current one finishes.
 */
public class SequentialCommandGroup implements Command {

    private final Queue<Command> commands = new ArrayDeque<>();
    private Command current;

    public SequentialCommandGroup(Command... commands) {
        this.commands.addAll(java.util.List.of(commands));
    }

    @Override
    public void initialize() {
        current = commands.poll();
        if (current != null) current.initialize();
    }

    @Override
    public void execute() {
        if (current == null) return;

        current.execute();
        if (current.isFinished()) {
            current.end(false);
            current = commands.poll();
            if (current != null) current.initialize();
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted && current != null) {
            current.end(true);
        }
    }

    @Override
    public Set<Subsystem> getRequirements() {
        Set<Subsystem> requirements = new LinkedHashSet<>();
        if (current != null) {
            requirements.addAll(current.getRequirements());
        }
        for (Command command : commands) {
            requirements.addAll(command.getRequirements());
        }
        return requirements;
    }

    @Override
    public boolean isFinished() {
        return current == null;
    }
}
