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
 *  - scheduler lifecycle (including a full reset per OpMode run)
 *  - trigger polling and command bindings
 *  - telemetry updates during init and play
 *  - safe shutdown
 */
public abstract class CommandOpMode extends OpMode {

    protected CommandScheduler scheduler;
    protected FtcTelemetryBus telemetryBus;
    protected TriggerManager triggers;

    @Override
    public final void init() {
        scheduler = CommandScheduler.getInstance();
        // The scheduler is a process-wide singleton and the Robot Controller
        // app keeps the JVM alive between OpModes. A full reset unregisters
        // the previous OpMode's subsystems and listeners so stale periodic()
        // calls and default commands can never touch dead hardware objects.
        scheduler.reset();
        telemetryBus = new FtcTelemetryBus(
                telemetry,
                PanelsTelemetry.INSTANCE.getTelemetry()
        );
        triggers = TriggerManager.getDefault();
        triggers.clear();   // drop any bindings left over from a previous OpMode
        if (enableCommandLogging()) {
            scheduler.addListener(new FtcCommandLogger(telemetryBus));
        }
        initialize();
        configureBindings();
    }

    @Override
    public final void init_loop() {
        initLoop();
        telemetryBus.update();
    }

    @Override
    public final void start() {
        onStart();
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
        telemetryBus.clear();
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
     * Called repeatedly between init() and start() (the FTC init loop).
     *
     * Use for pre-match work such as vision-based randomization detection;
     * telemetryBus values queued here are flushed automatically. The
     * scheduler does not run until the OpMode starts.
     */
    protected void initLoop() {}

    /**
     * Called once when the driver presses play.
     *
     * Use to schedule match-start commands (e.g. an autonomous routine
     * chosen during initLoop()).
     */
    protected void onStart() {}

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
