package com.vcs.valleylib.core.command;

import androidx.annotation.NonNull;

import com.vcs.valleylib.core.command.decorators.BeforeStartingCommand;
import com.vcs.valleylib.core.command.decorators.DeadlineCommand;
import com.vcs.valleylib.core.command.decorators.FinallyCommand;
import com.vcs.valleylib.core.command.decorators.OnlyWhileCommand;
import com.vcs.valleylib.core.command.decorators.ParallelCommandGroup;
import com.vcs.valleylib.core.command.decorators.RaceCommand;
import com.vcs.valleylib.core.command.decorators.RepeatCommand;
import com.vcs.valleylib.core.command.decorators.SequentialCommandGroup;
import com.vcs.valleylib.core.command.decorators.TimeoutCommand;
import com.vcs.valleylib.core.command.decorators.UnlessCommand;
import com.vcs.valleylib.core.command.decorators.UntilCommand;
import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Represents a unit of robot behavior.
 * <p>
 * Commands are scheduled by the CommandScheduler and are responsible for
 * controlling one or more Subsystems for some period of time.
 * <p>
 * Lifecycle:
 *  - initialize(): called once when scheduled
 *  - execute(): called every scheduler cycle while active
 *  - end(): called once when finished or interrupted
 * <p>
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

    default Command withTimeout(double seconds) {
        return new TimeoutCommand(this, (long) (seconds * 1000));
    }

    default Command until(BooleanSupplier condition) {
        return new UntilCommand(this, condition);
    }

    default Command onlyWhile(BooleanSupplier condition) {
        return new OnlyWhileCommand(this, condition);
    }

    default Command unless(BooleanSupplier condition) {
        return new UnlessCommand(this, condition);
    }

    default Command beforeStarting(Runnable action) {
        return new BeforeStartingCommand(this, action);
    }

    default Command finallyDo(Runnable action) {
        return new FinallyCommand(this, action);
    }

    default Command repeatedly() {
        return new RepeatCommand(this);
    }

    default Command alongWith(@NonNull Command... others) {
        Command[] all = new Command[others.length + 1];
        all[0] = this;
        System.arraycopy(others, 0, all, 1, others.length);
        return new ParallelCommandGroup(all);
    }

    default Command raceWith(@NonNull Command... others) {
        Command[] all = new Command[others.length + 1];
        all[0] = this;
        System.arraycopy(others, 0, all, 1, others.length);
        return new RaceCommand(all);
    }

    default Command deadlineWith(Command... others) {
        return new DeadlineCommand(this, others);
    }

    default Command andThen(Runnable action) {
        return andThen(new InstantCommand(action));
    }

    default Command andThen(@NonNull Command... nextCommands) {
        Command[] all = new Command[nextCommands.length + 1];
        all[0] = this;
        System.arraycopy(nextCommands, 0, all, 1, nextCommands.length);
        return new SequentialCommandGroup(all);
    }
}
