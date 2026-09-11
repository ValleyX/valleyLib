package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.CommandWrapper;

import java.util.Objects;

/**
 * Gives an existing command a human-readable name for logging and
 * telemetry without changing its behavior.
 */
public class NamedCommand extends CommandWrapper {

    private final String name;

    public NamedCommand(Command inner, String name) {
        super(inner);
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public String getName() {
        return name;
    }
}
