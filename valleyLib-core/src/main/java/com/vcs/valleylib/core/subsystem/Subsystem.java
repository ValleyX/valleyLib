package com.vcs.valleylib.core.subsystem;

import com.vcs.valleylib.core.command.Command;

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

    /**
     * Called every scheduler cycle, regardless of active commands.
     * Use this for sensor updates, state estimation, or safety checks.
     */
    public void periodic() {}

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
}