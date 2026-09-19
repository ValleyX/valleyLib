package com.vcs.valleylib.ftc.pedro;

import com.pedropathing.paths.Path;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;
import java.util.Set;

/**
 * A command that makes a Pedro Pathing follower drive along a Path.
 * <p>
 * The command finishes once the follower has finished the path and settled.
 * While running, no other command with the same subsystem requirement
 * (typically your drive subsystem) can run.
 */
public class FollowPathCommand implements Command {

    private final PedroSubsystem drive;
    private final Path path;
    private final double maxSpeed;

    public FollowPathCommand(PedroSubsystem drive, Path path) {
        this(drive, path, PedroSubsystem.NO_SPEED_LIMIT);
    }

    /**
     * @param maxSpeed fraction of the robot's maximum achievable velocity, or
     *                 {@link PedroSubsystem#NO_SPEED_LIMIT} to use the
     *                 follower's configured speed
     */
    public FollowPathCommand(PedroSubsystem drive, Path path, double maxSpeed) {
        this.drive = drive;
        this.path = path;
        this.maxSpeed = maxSpeed;
    }

    @Override
    public void initialize() {
        // The speed cap rides along as a path modifier, so Pedro restores the
        // follower's configured speed when the path ends or is abandoned.
        drive.getFollower().follow(drive.withSpeedLimit(path, maxSpeed));
    }

    @Override
    public void execute() {
        // Nothing else to run here; follower.update() is handled by the drive subsystem's periodic()
        // This method *must* exist because Command.execute() is not defaulted.
    }

    @Override
    public boolean isFinished() {
        return drive.isIdle();
    }

    @Override
    public void end(boolean interrupted) {
        // Without this, an interrupted follow would keep driving the path:
        // the follower lives in the subsystem and update() runs every cycle
        // regardless of which command holds the drivetrain.
        if (interrupted) {
            drive.stop();
        }
    }

    @Override
    public Set<Subsystem> getRequirements() {
        // The follower needs exclusive access to the drive subsystem
        return Set.of(drive);
    }
}
