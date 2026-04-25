package com.vcs.valleylib.ftc.opmode;

import com.bylazar.telemetry.PanelsTelemetry;
import com.vcs.valleylib.core.scheduler.CommandScheduler;
import com.vcs.valleylib.ftc.input.TriggerManager;
import com.vcs.valleylib.ftc.logging.FtcCommandLogger;
import com.vcs.valleylib.ftc.telemetry.FtcTelemetryBus;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

/**
 * Base OpMode for command-based FTC robots.
 *
 * Handles:
 *  - scheduler lifecycle
 *  - trigger polling and command bindings
 *  - telemetry updates
 *  - safe shutdown
 */
public abstract class CommandOpMode extends OpMode {

    protected CommandScheduler scheduler;
    protected FtcTelemetryBus telemetryBus;
    protected TriggerManager triggers;

    @Override
    public final void init() {
        scheduler = CommandScheduler.getInstance();
        telemetryBus = new FtcTelemetryBus(
                telemetry,
                PanelsTelemetry.INSTANCE.getTelemetry()
        );
        triggers = new TriggerManager();
        if (enableCommandLogging()) {
            scheduler.addListener(new FtcCommandLogger(telemetryBus));
        }
        initialize();
        configureBindings();
    }

    @Override
    public final void loop() {
        triggers.poll();
        scheduler.run();
        run();
        telemetryBus.update();
    }

    @Override
    public void stop() {
        scheduler.cancelAll();
        triggers.clear();
    }

    /**
     * Called once during init().
     * Create subsystems and commands here.
     */
    protected abstract void initialize();

    /**
     * Called once during init() after initialize().
     * Configure trigger/button bindings here.
     */
    protected void configureBindings() {}

    /**
     * Called every loop after scheduler execution.
     * Use for OpMode-specific logic and telemetry values.
     */
    protected abstract void run();

    /**
     * Override to emit command lifecycle events to telemetry automatically.
     */
    protected boolean enableCommandLogging() {
        return false;
    }
}

