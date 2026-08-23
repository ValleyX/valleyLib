package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.BaseCommand;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class RaceCommand extends BaseCommand {

    private final Command[] commands;

    public RaceCommand(Command... commands) {
        this.commands = commands;
    }

    @Override
    protected void onInitialize() {
        for (Command c : commands) {
            c.initialize();
        }
    }

    @Override
    protected void onExecute() {
        for (Command c : commands) {
            c.execute();
        }
    }

    @Override
    protected boolean onIsFinished() {
        for (Command c : commands) {
            if (c.isFinished()) return true;
        }
        return false;
    }

    @Override
    protected void onEnd(boolean interrupted) {
        // Commands that won the race ended naturally; the rest are interrupted.
        for (Command c : commands) {
            c.end(interrupted || !c.isFinished());
        }
    }

    @Override
    public Set<Subsystem> getRequirements() {
        Set<Subsystem> requirements = new LinkedHashSet<>();
        Arrays.stream(commands).forEach(command -> requirements.addAll(command.getRequirements()));
        return requirements;
    }
}
