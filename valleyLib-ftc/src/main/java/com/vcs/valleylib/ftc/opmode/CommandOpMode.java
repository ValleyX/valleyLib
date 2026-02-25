package com.vcs.valleylib.ftc.opmode;

import com.vcs.valleylib.core.scheduler.CommandScheduler;
import com.vcs.valleylib.ftc.telemetry.FtcTelemetryBus;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

/**
 * Base OpMode for command-based FTC robots.
 *
 * Handles:
 *  - scheduler lifecycle
 *  - telemetry updates
 *  - safe shutdown
 */
public abstract class CommandOpMode extends OpMode {

    protected CommandScheduler scheduler;
    protected FtcTelemetryBus telemetryBus;

    @Override
    public final void init() {
        scheduler = CommandScheduler.getInstance();
        telemetryBus = new FtcTelemetryBus(
                telemetry,
                PanelsTelemetry.get()
        );
        initialize();
    }

    @Override
    public final void loop() {
        scheduler.run();
        telemetryBus.update();
        run();
    }

    @Override
    public void stop() {
        scheduler.cancelAll();
    }

    /**
     * Called once during init().
     * Create subsystems, commands, and bindings here.
     */
    protected abstract void initialize();

    /**
     * Called every loop after scheduler execution.
     * Use for OpMode-specific logic.
     */
    protected abstract void run();
}