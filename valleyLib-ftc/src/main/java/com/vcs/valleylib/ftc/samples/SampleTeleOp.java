package com.vcs.valleylib.ftc.samples;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.vcs.valleylib.ftc.opmode.CommandOpMode;

/**
 * A sample TeleOp showing how to use CommandOpMode with a RobotContainer.
 */
@TeleOp(name = "ValleyLib: Sample TeleOp", group = "Samples")
public class SampleTeleOp extends CommandOpMode {

    private SimpleRobot robot;

    @Override
    protected void initialize() {
        // Initialize our robot container
        robot = new SimpleRobot(hardwareMap, gamepad1);
    }

    @Override
    protected void run() {
        // Here you can add loop-specific telemetry
        telemetryBus.put("Drivetrain", "Active");
        telemetryBus.put("Intake Status", "Ready");
    }

    @Override
    protected boolean enableCommandLogging() {
        // Turn on automatic command lifecycle logging to telemetry
        return true;
    }
}
