package com.vcs.valleylib.ftc.samples.hardware;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.vcs.valleylib.ftc.hardware.FtcSubsystem;

/**
 * Sample intake/roller hardware subsystem for command demonstrations.
 */
public class SampleIntakeHardware extends FtcSubsystem {

    private final CRServo intakeServo;

    public SampleIntakeHardware(HardwareMap hardwareMap) {
        super(hardwareMap);
        intakeServo = hardwareMap.get(CRServo.class, "intake");
    }

    public void intakeIn() {
        intakeServo.setPower(1.0);
    }

    public void intakeOut() {
        intakeServo.setPower(-1.0);
    }

    public void stop() {
        intakeServo.setPower(0.0);
    }
}
