package com.vcs.valleylib.ftc.samples.auto;

import com.pedropathing.paths.PathChain;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.Commands;
import com.vcs.valleylib.ftc.pedro.PedroAutoDsl;
import com.vcs.valleylib.ftc.pedro.PedroCommands;
import com.vcs.valleylib.ftc.pedro.PedroSubsystem;
import com.vcs.valleylib.ftc.samples.hardware.SampleIntakeHardware;

/**
 * Example autonomous routines that teams can copy as a starting point.
 *
 * These samples are intentionally path-first and use Pedro pathing command helpers.
 */
public final class SampleAutos {

    private SampleAutos() {}

    public static Command simpleTaxi(PedroSubsystem drive, PathChain taxiPath) {
        return PedroAutoDsl.auto(drive, auto -> auto
                .action(() -> System.out.println("auto:start"))
                .follow(taxiPath, 0.75)
                .waitUntilDriveIdle()
                .action(() -> System.out.println("auto:done")));
    }

    public static Command taxiAndCycle(
            PedroSubsystem drive,
            SampleIntakeHardware intake,
            PathChain taxiPath,
            PathChain cyclePath
    ) {
        return PedroAutoDsl.auto(drive, auto -> auto
                .follow(taxiPath, 0.8)
                .parallel(
                        PedroCommands.follow(drive, cyclePath, 0.9),
                        Commands.startEnd(intake::intakeIn, intake::stop)
                )
                .waitUntilDriveIdle()
                .action(intake::stop));
    }
}
