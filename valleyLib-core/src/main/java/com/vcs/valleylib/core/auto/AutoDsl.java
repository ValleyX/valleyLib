package com.vcs.valleylib.core.auto;

import androidx.annotation.NonNull;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.InstantCommand;
import com.vcs.valleylib.core.command.WaitCommand;
import com.vcs.valleylib.core.command.decorators.DeadlineCommand;
import com.vcs.valleylib.core.command.decorators.ParallelCommandGroup;
import com.vcs.valleylib.core.command.decorators.RaceCommand;
import com.vcs.valleylib.core.command.decorators.SequentialCommandGroup;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Small autonomous builder DSL on top of command composition.
 */
public final class AutoDsl {

    private AutoDsl() {}

    @androidx.annotation.NonNull
    public static Command auto(@NonNull Consumer<AutoBuilder> block) {
        AutoBuilder builder = new AutoBuilder();
        block.accept(builder);
        return builder.build();
    }

    public static final class AutoBuilder {

        private final List<Command> steps = new ArrayList<>();

        public AutoBuilder command(Command command) {
            steps.add(command);
            return this;
        }

        /**
         * Compatibility alias for teams that prefer add-step naming.
         */
        public AutoBuilder add(Command command) {
            return command(command);
        }

        public AutoBuilder run(Runnable action) {
            steps.add(new InstantCommand(action));
            return this;
        }

        /**
         * Compatibility alias for instant-action naming used in some DSLs.
         */
        public AutoBuilder doInstant(Runnable action) {
            return run(action);
        }

        /**
         * Adds a timeline marker callback. Useful for telemetry and profiling.
         */
        public AutoBuilder marker(String label, Consumer<String> sink) {
            steps.add(new InstantCommand(() -> sink.accept(label)));
            return this;
        }

        public AutoBuilder waitSeconds(double seconds) {
            steps.add(new WaitCommand(seconds));
            return this;
        }

        /**
         * Compatibility alias for waitSeconds.
         */
        public AutoBuilder waitFor(double seconds) {
            return waitSeconds(seconds);
        }

        @Contract("_, _ -> this")
        public AutoBuilder when(BooleanSupplier condition, @NonNull Command command) {
            steps.add(command.unless(() -> !condition.getAsBoolean()));
            return this;
        }

        public AutoBuilder either(BooleanSupplier condition, Command onTrue, Command onFalse) {
            steps.add(new ConditionalCommand(condition, onTrue, onFalse));
            return this;
        }

        /**
         * Compatibility alias for either(...).
         */
        public AutoBuilder ifElse(BooleanSupplier condition, Command onTrue, Command onFalse) {
            return either(condition, onTrue, onFalse);
        }

        public AutoBuilder parallel(Command... commands) {
            steps.add(new ParallelCommandGroup(commands));
            return this;
        }

        public AutoBuilder race(Command... commands) {
            steps.add(new RaceCommand(commands));
            return this;
        }

        public AutoBuilder deadline(Command deadline, Command... others) {
            steps.add(new DeadlineCommand(deadline, others));
            return this;
        }

        @NonNull
        @Contract(" -> new")
        public Command build() {
            return new SequentialCommandGroup(steps.toArray(new Command[0]));
        }
    }

    private static class ConditionalCommand implements Command {

        private final BooleanSupplier condition;
        private final Command onTrue;
        private final Command onFalse;

        private Command active;

        private ConditionalCommand(BooleanSupplier condition, Command onTrue, Command onFalse) {
            this.condition = condition;
            this.onTrue = onTrue;
            this.onFalse = onFalse;
        }

        @Override
        public void initialize() {
            active = condition.getAsBoolean() ? onTrue : onFalse;
            active.initialize();
        }

        @Override
        public void execute() {
            active.execute();
        }

        @Override
        public void end(boolean interrupted) {
            active.end(interrupted);
        }

        @Override
        public boolean isFinished() {
            return active.isFinished();
        }

        @NonNull
        @Override
        public java.util.Set<com.vcs.valleylib.core.subsystem.Subsystem> getRequirements() {
            java.util.Set<com.vcs.valleylib.core.subsystem.Subsystem> requirements = new java.util.LinkedHashSet<>();
            requirements.addAll(onTrue.getRequirements());
            requirements.addAll(onFalse.getRequirements());
            return requirements;
        }
    }
}
