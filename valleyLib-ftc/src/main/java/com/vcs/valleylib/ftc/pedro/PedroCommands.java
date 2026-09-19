package com.vcs.valleylib.ftc.pedro;

import com.pedropathing.paths.Path;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.Commands;

/**
 * Static command factories for Pedro pathing workflows.
 */
public final class PedroCommands {

    private PedroCommands() {}

    public static Command follow(PedroSubsystem drive, Path path) {
        return drive.follow(path);
    }

    public static Command follow(PedroSubsystem drive, Path path, double maxSpeed) {
        return drive.follow(path, maxSpeed);
    }

    public static Command waitUntilIdle(PedroSubsystem drive) {
        return drive.waitUntilIdle();
    }

    /**
     * Follow all paths in sequence.
     */
    public static Command followSequence(PedroSubsystem drive, Path... paths) {
        Command[] commands = new Command[paths.length];
        for (int i = 0; i < paths.length; i++) {
            commands[i] = drive.follow(paths[i]);
        }
        return Commands.sequence(commands);
    }
}
