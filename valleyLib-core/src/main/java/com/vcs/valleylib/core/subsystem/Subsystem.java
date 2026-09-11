package com.vcs.valleylib.core.subsystem;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.FunctionalCommand;
import com.vcs.valleylib.core.scheduler.CommandScheduler;

/**
 * Base class for all robot subsystems.
 *
 * A subsystem represents a cohesive piece of hardware:
 * drivetrain, shooter, intake, arm, etc.
 *
 * Subsystems:
 *  - expose high-level actions (not raw motor power everywhere)
 *  - run periodic background logic
 *  - own a default command for idle behavior
 *  - build requirement-carrying commands via the factory methods below
 */
public abstract class Subsystem {

    private static final Runnable NO_OP = () -> {};

    private Command defaultCommand;

    protected Subsystem() {
        CommandScheduler.getInstance().registerSubsystem(this);
    }

    /**
     * Called every scheduler cycle, regardless of active commands.
     * Use this for sensor updates, state estimation, or safety checks.
     */
    public void periodic() {}

    /**
     * Called in simulation-enabled scheduler runs.
     *
     * Override this to run desktop-only sensor/motion simulation hooks.
     */
    public void simulationPeriodic() {}

    /**
     * Sets the command that should run whenever no other command
     * is using this subsystem.
     *
     * Example: field-centric drive during TeleOp.
     *
     * @param command default command
     */
    public void setDefaultCommand(Command command) {
        this.defaultCommand = command;
    }

    /**
     * @return the subsystem's default command, or null if none
     */
    public Command getDefaultCommand() {
        return defaultCommand;
    }

    /**
     * A short name for this subsystem, used in command names and
     * telemetry. Defaults to the class's simple name.
     */
    public String getName() {
        return getClass().getSimpleName();
    }

    // ------------------------------------------------------------------
    // Command factories (WPILib-style)
    //
    // Unlike the Commands factory class, commands built here REQUIRE this
    // subsystem, so they participate in scheduler conflict resolution.
    // ------------------------------------------------------------------

    /**
     * Returns a command that runs the action once and finishes,
     * requiring this subsystem.
     */
    public Command runOnce(Runnable action) {
        return new FunctionalCommand(NO_OP, action, interrupted -> {}, () -> true, this)
                .withName(getName() + ".runOnce");
    }

    /**
     * Returns a command that runs the action every cycle and never finishes
     * on its own, requiring this subsystem. Ideal for default commands.
     */
    public Command run(Runnable action) {
        return new FunctionalCommand(NO_OP, action, interrupted -> {}, () -> false, this)
                .withName(getName() + ".run");
    }

    /**
     * Returns a command that runs {@code onStart} when scheduled and
     * {@code onEnd} when it ends (finished or interrupted), requiring this
     * subsystem. Ideal for whileTrue bindings.
     */
    public Command startEnd(Runnable onStart, Runnable onEnd) {
        return new FunctionalCommand(onStart, NO_OP, interrupted -> onEnd.run(), () -> false, this)
                .withName(getName() + ".startEnd");
    }

    /**
     * Returns a command that runs {@code action} every cycle and
     * {@code onEnd} when it ends (finished or interrupted), requiring this
     * subsystem.
     */
    public Command runEnd(Runnable action, Runnable onEnd) {
        return new FunctionalCommand(NO_OP, action, interrupted -> onEnd.run(), () -> false, this)
                .withName(getName() + ".runEnd");
    }

    /**
     * Returns a command that holds this subsystem and does nothing —
     * useful to explicitly idle a mechanism inside a parallel group or as
     * a placeholder default command.
     */
    public Command idle() {
        return new FunctionalCommand(NO_OP, NO_OP, interrupted -> {}, () -> false, this)
                .withName(getName() + ".idle");
    }
}
