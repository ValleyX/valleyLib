package com.vcs.valleylib.ftc.samples;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.Commands;
import com.vcs.valleylib.ftc.RobotContainer;
import com.vcs.valleylib.ftc.input.CommandGamepad;
import com.vcs.valleylib.ftc.samples.hardware.SampleDriveHardware;
import com.vcs.valleylib.ftc.samples.hardware.SampleIntakeHardware;

/**
 * A concrete example of a RobotContainer.
 *
 * This centralizes all subsystem instantiation and button bindings.
 */
public class SimpleRobot extends RobotContainer {

    public final SampleDriveHardware drive;
    public final SampleIntakeHardware intake;
    public final CommandGamepad driver;

    public SimpleRobot(HardwareMap hardwareMap, Gamepad gamepad1) {
        // Subsystems register themselves with the scheduler automatically
        drive = new SampleDriveHardware(hardwareMap);
        intake = new SampleIntakeHardware(hardwareMap);

        // Input handling
        driver = CommandGamepad.forLogitechF310(gamepad1);

        configureBindings();
    }

    @Override
    public void configureBindings() {
        // Set default drive command (Tank Drive)
        drive.setDefaultCommand(Commands.run(() ->
                drive.tankDrive(driver.leftY(), driver.rightY())
        ));

        // Intake control on buttons
        driver.a().whileTrue(Commands.startEnd(intake::intakeIn, intake::stop));
        driver.b().whileTrue(Commands.startEnd(intake::intakeOut, intake::stop));

        // High-level command composition
        driver.rightBumper().onTrue(
                Commands.runOnce(intake::intakeIn)
                        .andThen(Commands.waitSeconds(0.5))
                        .andThen(intake::stop)
        );
    }

    @Override
    public Command getAutonomousCommand() {
        // Example simple autonomous: drive forward then stop
        return Commands.run(() -> drive.tankDrive(0.5, 0.5))
                .withTimeout(2.0)
                .finallyDo(drive::stop);
    }
}
