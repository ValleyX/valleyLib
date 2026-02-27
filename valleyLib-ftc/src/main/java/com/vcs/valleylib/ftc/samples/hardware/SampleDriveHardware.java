package com.vcs.valleylib.ftc.samples.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.vcs.valleylib.ftc.hardware.FtcSubsystem;

/**
 * Sample drivetrain wrapper showing one way to structure hardware-backed subsystems.
 */
public class SampleDriveHardware extends FtcSubsystem {

    private final DcMotor leftFront;
    private final DcMotor rightFront;
    private final DcMotor leftRear;
    private final DcMotor rightRear;

    public SampleDriveHardware(HardwareMap hardwareMap) {
        super(hardwareMap);
        leftFront = hardwareMap.get(DcMotor.class, "leftFront");
        rightFront = hardwareMap.get(DcMotor.class, "rightFront");
        leftRear = hardwareMap.get(DcMotor.class, "leftRear");
        rightRear = hardwareMap.get(DcMotor.class, "rightRear");

        leftFront.setDirection(DcMotorSimple.Direction.REVERSE);
        leftRear.setDirection(DcMotorSimple.Direction.REVERSE);

        setAllModes(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        setAllZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    public void tankDrive(double leftPower, double rightPower) {
        leftFront.setPower(leftPower);
        leftRear.setPower(leftPower);
        rightFront.setPower(rightPower);
        rightRear.setPower(rightPower);
    }

    public void stop() {
        tankDrive(0.0, 0.0);
    }

    private void setAllModes(DcMotor.RunMode mode) {
        leftFront.setMode(mode);
        rightFront.setMode(mode);
        leftRear.setMode(mode);
        rightRear.setMode(mode);
    }

    private void setAllZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        leftFront.setZeroPowerBehavior(behavior);
        rightFront.setZeroPowerBehavior(behavior);
        leftRear.setZeroPowerBehavior(behavior);
        rightRear.setZeroPowerBehavior(behavior);
    }
}
