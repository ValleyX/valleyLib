package com.vcs.valleylib.core.command;

import androidx.annotation.NonNull;

import com.vcs.valleylib.core.command.decorators.DeadlineCommand;
import com.vcs.valleylib.core.command.decorators.ParallelCommandGroup;
import com.vcs.valleylib.core.command.decorators.RaceCommand;
import com.vcs.valleylib.core.command.decorators.SequentialCommandGroup;

import org.jetbrains.annotations.Contract;

import java.util.function.BooleanSupplier;

/**
 * Static factories for common command construction patterns.
 */
public final class Commands {

    private Commands() {}

    @NonNull
    @Contract(value = " -> new", pure = true)
    public static Command none() {
        return new InstantCommand(() -> {});
    }

    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Command runOnce(Runnable action) {
        return new InstantCommand(action);
    }

    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Command run(Runnable action) {
        return new Command() {
            @Override
            public void execute() {
                action.run();
            }
        };
    }

    @NonNull
    @Contract(value = "_, _ -> new", pure = true)
    public static Command startEnd(Runnable onStart, Runnable onEnd) {
        return new Command() {
            @Override
            public void initialize() {
                onStart.run();
            }

            @Override
            public void execute() {}

            @Override
            public void end(boolean interrupted) {
                onEnd.run();
            }
        };
    }

    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static Command waitSeconds(double seconds) {
        return new WaitCommand(seconds);
    }

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
    @Contract("_, _ -> new")
    public static Command deadline(Command deadline, Command... others) {
        return new DeadlineCommand(deadline, others);
    }
}
