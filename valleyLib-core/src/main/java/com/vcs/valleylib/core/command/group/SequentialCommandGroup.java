package com.vcs.valleylib.core.command.group;

import com.vcs.valleylib.core.command.Command;

import java.util.ArrayDeque;
import java.util.Queue;

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
    public boolean isFinished() {
        return current == null;
    }
}