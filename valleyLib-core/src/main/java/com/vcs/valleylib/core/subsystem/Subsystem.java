package com.vcs.valleylib.core.subsystem;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.scheduler.CommandScheduler;

import java.util.Set;

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
 */
public abstract class Subsystem {

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
        return new Command() {
            private boolean hasRun;

            @Override
            public void initialize() {
                hasRun = false;
            }

            @Override
            public void execute() {
                if (!hasRun) {
                    action.run();
                    hasRun = true;
                }
            }

            @Override
            public boolean isFinished() {
                return hasRun;
            }

            @Override
            public Set<Subsystem> getRequirements() {
                return Set.of(Subsystem.this);
            }
        };
    }

    /**
     * Returns a command that runs the action every cycle and never finishes
     * on its own, requiring this subsystem. Ideal for default commands.
     */
    public Command run(Runnable action) {
        return new Command() {
            @Override
            public void execute() {
                action.run();
            }

            @Override
            public Set<Subsystem> getRequirements() {
                return Set.of(Subsystem.this);
            }
        };
    }

    /**
     * Returns a command that runs {@code onStart} when scheduled and
     * {@code onEnd} when it ends (finished or interrupted), requiring this
     * subsystem. Ideal for whileTrue bindings.
     */
    public Command startEnd(Runnable onStart, Runnable onEnd) {
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

            @Override
            public Set<Subsystem> getRequirements() {
                return Set.of(Subsystem.this);
            }
        };
    }

    /**
     * Returns a command that runs {@code action} every cycle and
     * {@code onEnd} when it ends (finished or interrupted), requiring this
     * subsystem.
     */
    public Command runEnd(Runnable action, Runnable onEnd) {
        return new Command() {
            @Override
            public void execute() {
                action.run();
            }

            @Override
            public void end(boolean interrupted) {
                onEnd.run();
            }

            @Override
            public Set<Subsystem> getRequirements() {
                return Set.of(Subsystem.this);
            }
        };
    }
}
