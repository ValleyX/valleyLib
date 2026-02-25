package com.vcs.valleylib.ftc.hardware;

import com.vcs.valleylib.core.subsystem.Subsystem;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Base class for FTC hardware-backed subsystems.
 *
 * Provides direct access to the HardwareMap while keeping
 * command logic hardware-agnostic.
 */
public abstract class FtcSubsystem extends Subsystem {

    protected final HardwareMap hardwareMap;

    protected FtcSubsystem(HardwareMap hardwareMap) {
        this.hardwareMap = hardwareMap;
    }
}