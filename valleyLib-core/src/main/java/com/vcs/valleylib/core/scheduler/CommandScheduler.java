package com.vcs.valleylib.core.scheduler;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.*;

/**
 * Central runtime for the command-based system.
 *
 * Responsibilities:
 *  - schedules and runs commands
 *  - enforces subsystem requirements (mutual exclusion)
 *  - runs subsystem periodic methods
 *  - manages default commands
 *
 * This class is a singleton by design.
 */
public final class CommandScheduler {

    private static CommandScheduler instance;

    private final Set<Command> scheduledCommands = new HashSet<>();
    private final Map<Subsystem, Command> requirements = new HashMap<>();
    private final Set<Subsystem> subsystems = new HashSet<>();

    private CommandScheduler() {}

    /**
     * @return the single global scheduler instance
     */
    public static CommandScheduler getInstance() {
        if (instance == null) {
            instance = new CommandScheduler();
        }
        return instance;
    }

    /**
     * Registers a subsystem so its periodic method and default command
     * can be managed by the scheduler.
     *
     * @param subsystem subsystem to register
     */
    public void registerSubsystem(Subsystem subsystem) {
        subsystems.add(subsystem);
    }

    /**
     * Schedules a command for execution.
     *
     * Any existing commands that conflict on subsystem requirements
     * will be interrupted.
     *
     * @param command command to schedule
     */
    public void schedule(Command command) {
        if (scheduledCommands.contains(command)) {
            return;
        }

        for (Subsystem req : command.getRequirements()) {
            if (requirements.containsKey(req)) {
                cancel(requirements.get(req));
            }
        }

        for (Subsystem req : command.getRequirements()) {
            requirements.put(req, command);
        }

        command.initialize();
        scheduledCommands.add(command);
    }

    /**
     * Main scheduler loop.
     * Call this once per OpMode loop.
     */
    public void run() {
        // Run subsystem background logic
        for (Subsystem subsystem : subsystems) {
            subsystem.periodic();
        }

        // Schedule default commands if subsystem is idle
        for (Subsystem subsystem : subsystems) {
            Command defaultCmd = subsystem.getDefaultCommand();
            if (defaultCmd != null
                    && !scheduledCommands.contains(defaultCmd)
                    && !requirements.containsKey(subsystem)) {
                schedule(defaultCmd);
            }
        }

        // Run active commands
        Iterator<Command> iterator = scheduledCommands.iterator();
        while (iterator.hasNext()) {
            Command command = iterator.next();
            command.execute();

            if (command.isFinished()) {
                command.end(false);
                release(command);
                iterator.remove();
            }
        }
    }

    /**
     * Cancels a specific command.
     *
     * @param command command to cancel
     */
    public void cancel(Command command) {
        if (scheduledCommands.remove(command)) {
            command.end(true);
            release(command);
        }
    }

    /**
     * Cancels all running commands.
     * Typically used when an OpMode stops.
     */
    public void cancelAll() {
        for (Command command : scheduledCommands) {
            command.end(true);
        }
        scheduledCommands.clear();
        requirements.clear();
    }

    /**
     * Frees subsystem requirements held by a command.
     */
    private void release(Command command) {
        requirements.entrySet().removeIf(e -> e.getValue() == command);
    }
}
