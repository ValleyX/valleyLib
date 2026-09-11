package com.vcs.valleylib.ftc.logging;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.scheduler.CommandSchedulerListener;
import com.vcs.valleylib.ftc.telemetry.FtcTelemetryBus;

/**
 * Command scheduler listener that writes lifecycle events to telemetry.
 * <p>
 * Uses {@link Command#getName()}, so factory-built commands
 * ({@code intake.startEnd(...)}, {@code Commands.run(...)}) and named
 * commands ({@code cmd.withName("Score")}) log readably instead of as
 * anonymous class names.
 */
public class FtcCommandLogger implements CommandSchedulerListener {

    private final FtcTelemetryBus telemetryBus;

    public FtcCommandLogger(FtcTelemetryBus telemetryBus) {
        this.telemetryBus = telemetryBus;
    }

    @Override
    public void onCommandScheduled(Command command) {
        telemetryBus.put("cmd/scheduled", command.getName());
    }

    @Override
    public void onCommandFinished(Command command) {
        telemetryBus.put("cmd/finished", command.getName());
    }

    @Override
    public void onCommandCanceled(Command command) {
        telemetryBus.put("cmd/canceled", command.getName());
    }
}
