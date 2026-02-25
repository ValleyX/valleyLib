package com.vcs.valleylib.core.command;

import com.vcs.valleylib.core.subsystem.Subsystem;
import java.util.Set;

/**
 * Represents a unit of robot behavior.
 *
 * Commands are scheduled by the CommandScheduler and are responsible for
 * controlling one or more Subsystems for some period of time.
 *
 * Lifecycle:
 *  - initialize(): called once when scheduled
 *  - execute(): called every scheduler cycle while active
 *  - end(): called once when finished or interrupted
 *
 * Commands may declare subsystem requirements to prevent motor/resource conflicts.
 */
public interface Command {

    /**
     * Called once when the command is first scheduled.
     * Use this to reset timers, sensors, or internal state.
     */
    default void initialize() {}

    /**
     * Called repeatedly while the command is scheduled.
     * This is where actuator control logic belongs.
     */
    void execute();

    /**
     * Called once when the command ends.
     *
     * @param interrupted true if the command was canceled or preempted
     */
    default void end(boolean interrupted) {}

    /**
     * Determines when the command should finish.
     *
     * @return true when the command has completed its task
     */
    default boolean isFinished() {
        return false;
    }

    /**
     * Declares which subsystems this command requires exclusive access to.
     *
     * If another command is scheduled that requires the same subsystem,
     * this command will be interrupted.
     *
     * @return set of required subsystems
     */
    default Set<Subsystem> getRequirements() {
        return Set.of();
    }
}