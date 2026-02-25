package com.vcs.valleylib.ftc;

import com.vcs.valleylib.core.command.Command;

/**
 * Central place where robot subsystems, commands,
 * and operator bindings are defined.
 *
 * Mirrors WPILib's RobotContainer concept, adapted for FTC.
 */
public abstract class RobotContainer {

    /**
     * Configure driver controls and button bindings.
     */
    public abstract void configureBindings();

    /**
     * @return the autonomous command to run
     */
    public abstract Command getAutonomousCommand();
}