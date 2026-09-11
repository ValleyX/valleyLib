package com.vcs.valleylib.core.command;

import com.vcs.valleylib.core.subsystem.Subsystem;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A command assembled from lambdas for each lifecycle method.
 * <p>
 * This is the primitive behind the {@link Commands} and
 * {@link Subsystem} factory methods. Use it directly when a command needs
 * custom logic in several lifecycle phases and requirements on more than
 * one subsystem, but is still too small to deserve its own class.
 *
 * <pre>{@code
 * Command handoff = new FunctionalCommand(
 *         () -> { intake.slow(); transfer.open(); },   // initialize
 *         () -> {},                                    // execute
 *         interrupted -> { intake.stop(); transfer.close(); }, // end
 *         transfer::hasGamePiece,                      // isFinished
 *         intake, transfer)                            // requirements
 *     .withName("Handoff");
 * }</pre>
 */
public class FunctionalCommand implements Command {

    private final Runnable onInitialize;
    private final Runnable onExecute;
    private final Consumer<Boolean> onEnd;
    private final BooleanSupplier isFinished;
    private final Set<Subsystem> requirements;
    private String name = "FunctionalCommand";

    public FunctionalCommand(
            Runnable onInitialize,
            Runnable onExecute,
            Consumer<Boolean> onEnd,
            BooleanSupplier isFinished,
            Subsystem... requirements) {
        this.onInitialize = Objects.requireNonNull(onInitialize, "onInitialize");
        this.onExecute = Objects.requireNonNull(onExecute, "onExecute");
        this.onEnd = Objects.requireNonNull(onEnd, "onEnd");
        this.isFinished = Objects.requireNonNull(isFinished, "isFinished");
        this.requirements = new LinkedHashSet<>(Arrays.asList(requirements));
    }

    @Override
    public void initialize() {
        onInitialize.run();
    }

    @Override
    public void execute() {
        onExecute.run();
    }

    @Override
    public void end(boolean interrupted) {
        onEnd.accept(interrupted);
    }

    @Override
    public boolean isFinished() {
        return isFinished.getAsBoolean();
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return requirements;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets this command's name in place (no wrapper) and returns it.
     */
    @Override
    public Command withName(String name) {
        this.name = Objects.requireNonNull(name, "name");
        return this;
    }
}
