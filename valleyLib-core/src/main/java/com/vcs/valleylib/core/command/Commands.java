package com.vcs.valleylib.core.command;

import androidx.annotation.NonNull;

import com.vcs.valleylib.core.command.decorators.ConditionalCommand;
import com.vcs.valleylib.core.command.decorators.DeadlineCommand;
import com.vcs.valleylib.core.command.decorators.ParallelCommandGroup;
import com.vcs.valleylib.core.command.decorators.RaceCommand;
import com.vcs.valleylib.core.command.decorators.SequentialCommandGroup;
import com.vcs.valleylib.core.subsystem.Subsystem;

import org.jetbrains.annotations.Contract;

import java.util.function.BooleanSupplier;

/**
 * Static factories for common command construction patterns.
 * <p>
 * The single-argument factories build commands with no subsystem
 * requirements — glue such as waits, markers, and grouping. Every
 * behavior factory also has an overload taking {@code Subsystem...}
 * requirements for commands that actuate hardware; when only one
 * subsystem is involved, the equivalent factory methods on
 * {@link Subsystem} read more naturally.
 */
public final class Commands {

    private static final Runnable NO_OP = () -> {};

    private Commands() {}

    /**
     * A command that does nothing and finishes immediately.
     */
    @NonNull
    @Contract(value = " -> new", pure = true)
    public static Command none() {
        return new InstantCommand(NO_OP).withName("none");
    }

    /**
     * Runs the action once, then finishes.
     */
    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Command runOnce(Runnable action) {
        return new InstantCommand(action);
    }

    /**
     * Runs the action once, then finishes, requiring the given subsystems.
     */
    @NonNull
    public static Command runOnce(Runnable action, Subsystem... requirements) {
        return new FunctionalCommand(NO_OP, action, interrupted -> {}, () -> true, requirements)
                .withName("runOnce");
    }

    /**
     * Runs the action every cycle and never finishes on its own.
     */
    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Command run(Runnable action) {
        return run(action, new Subsystem[0]);
    }

    /**
     * Runs the action every cycle and never finishes on its own, requiring
     * the given subsystems. Ideal for default commands.
     */
    @NonNull
    public static Command run(Runnable action, Subsystem... requirements) {
        return new FunctionalCommand(NO_OP, action, interrupted -> {}, () -> false, requirements)
                .withName("run");
    }

    /**
     * Runs {@code onStart} when scheduled and {@code onEnd} when it ends
     * (finished or interrupted); never finishes on its own.
     */
    @NonNull
    @Contract(value = "_, _ -> new", pure = true)
    public static Command startEnd(Runnable onStart, Runnable onEnd) {
        return startEnd(onStart, onEnd, new Subsystem[0]);
    }

    /**
     * Runs {@code onStart} when scheduled and {@code onEnd} when it ends,
     * requiring the given subsystems. Ideal for whileTrue bindings.
     */
    @NonNull
    public static Command startEnd(Runnable onStart, Runnable onEnd, Subsystem... requirements) {
        return new FunctionalCommand(onStart, NO_OP, interrupted -> onEnd.run(), () -> false, requirements)
                .withName("startEnd");
    }

    /**
     * Runs {@code action} every cycle and {@code onEnd} when it ends
     * (finished or interrupted); never finishes on its own.
     */
    @NonNull
    public static Command runEnd(Runnable action, Runnable onEnd) {
        return runEnd(action, onEnd, new Subsystem[0]);
    }

    /**
     * Runs {@code action} every cycle and {@code onEnd} when it ends,
     * requiring the given subsystems.
     */
    @NonNull
    public static Command runEnd(Runnable action, Runnable onEnd, Subsystem... requirements) {
        return new FunctionalCommand(NO_OP, action, interrupted -> onEnd.run(), () -> false, requirements)
                .withName("runEnd");
    }

    /**
     * Does nothing for a fixed duration.
     */
    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Command waitSeconds(double seconds) {
        return new WaitCommand(seconds);
    }

    /**
     * Finishes once the condition becomes true.
     */
    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Command waitUntil(BooleanSupplier condition) {
        return new WaitUntilCommand(condition);
    }

    @NonNull
    @Contract("_ -> new")
    public static Command sequence(Command... commands) {
        return new SequentialCommandGroup(commands);
    }

    @NonNull
    @Contract("_ -> new")
    public static Command parallel(Command... commands) {
        return new ParallelCommandGroup(commands);
    }

    @NonNull
    @Contract("_ -> new")
    public static Command race(Command... commands) {
        return new RaceCommand(commands);
    }

    @NonNull
    @Contract("_, _, _ -> new")
    public static Command either(Command onTrue, Command onFalse, BooleanSupplier condition) {
        return new ConditionalCommand(condition, onTrue, onFalse);
    }

    @NonNull
    @Contract("_, _ -> new")
    public static Command deadline(Command deadline, Command... others) {
        return new DeadlineCommand(deadline, others);
    }
}
