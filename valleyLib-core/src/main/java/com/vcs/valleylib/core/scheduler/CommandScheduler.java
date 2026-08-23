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

    private final Set<Command> scheduledCommands = new LinkedHashSet<>();
    private final Map<Subsystem, Command> requirements = new HashMap<>();
    private final Set<Subsystem> subsystems = new LinkedHashSet<>();
    private final Set<CommandSchedulerListener> listeners = new LinkedHashSet<>();

    // Reused across run() calls to avoid allocating a fresh list every loop,
    // which adds GC pressure on Android at 50+ Hz loop rates.
    private final List<Command> runBuffer = new ArrayList<>();

    private boolean simulationEnabled;

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
     * Adds a command lifecycle listener.
     */
    public void addListener(CommandSchedulerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered command lifecycle listener.
     */
    public void removeListener(CommandSchedulerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Enables/disables simulationPeriodic hooks during run().
     */
    public void setSimulationEnabled(boolean simulationEnabled) {
        this.simulationEnabled = simulationEnabled;
    }

    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    /**
     * Runs only subsystem simulation hooks.
     * Useful for desktop tests where full scheduler execution is not desired.
     */
    public void runSimulationStep() {
        for (Subsystem subsystem : subsystems) {
            subsystem.simulationPeriodic();
        }
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
        for (CommandSchedulerListener listener : listeners) {
            listener.onCommandScheduled(command);
        }
    }

    /**
     * Main scheduler loop.
     * Call this once per OpMode loop.
     */
    public void run() {
        if (simulationEnabled) {
            runSimulationStep();
        }

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

        // Run active commands - iterate over a snapshot to avoid
        // ConcurrentModificationException if commands schedule or cancel
        // others during their execution.
        runBuffer.clear();
        runBuffer.addAll(scheduledCommands);
        for (Command command : runBuffer) {
            if (!scheduledCommands.contains(command)) {
                continue;
            }

            command.execute();

            if (command.isFinished()) {
                scheduledCommands.remove(command);
                command.end(false);
                release(command);
                for (CommandSchedulerListener listener : listeners) {
                    listener.onCommandFinished(command);
                }
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
            for (CommandSchedulerListener listener : listeners) {
                listener.onCommandCanceled(command);
            }
        }
    }

    /**
     * Cancels all running commands.
     * Typically used when an OpMode stops.
     */
    public void cancelAll() {
        List<Command> toCancel = new ArrayList<>(scheduledCommands);
        for (Command command : toCancel) {
            command.end(true);
            for (CommandSchedulerListener listener : listeners) {
                listener.onCommandCanceled(command);
            }
        }
        scheduledCommands.clear();
        requirements.clear();
    }

    /**
     * Returns whether a command instance is currently active in the scheduler.
     *
     * Useful for button-toggle style bindings and diagnostics.
     *
     * @param command command instance to check
     * @return true when the command is in the scheduled set
     */
    public boolean isScheduled(Command command) {
        return scheduledCommands.contains(command);
    }

    /**
     * Returns an unmodifiable view of the currently scheduled commands,
     * in scheduling order. Intended for diagnostics and telemetry.
     */
    public Set<Command> getScheduledCommands() {
        return Collections.unmodifiableSet(scheduledCommands);
    }

    /**
     * Returns the command currently requiring a subsystem, or null when the
     * subsystem is idle. Intended for diagnostics and telemetry.
     */
    public Command requiring(Subsystem subsystem) {
        return requirements.get(subsystem);
    }

    /**
     * Clears scheduler runtime state, including registered subsystems.
     *
     * Intended for test isolation or explicit lifecycle resets between
     * distinct robot modes. Any active command is interrupted.
     */
    public void reset() {
        cancelAll();
        subsystems.clear();
        listeners.clear();
        simulationEnabled = false;
    }

    /**
     * Frees subsystem requirements held by a command.
     */
    private void release(Command command) {
        requirements.entrySet().removeIf(e -> e.getValue() == command);
    }
}
