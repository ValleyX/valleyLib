package com.vcs.valleylib.ftc.pedro;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.algorithm.Foresight;
import com.pedropathing.config.ConfigVar;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.InstantCommand;
import com.vcs.valleylib.core.command.WaitUntilCommand;
import com.vcs.valleylib.ftc.hardware.FtcSubsystem;

/**
 * Base subsystem for robots using Pedro Pathing.
 * <p>
 * Owns the Follower and ensures update() is called
 * consistently through the command scheduler.
 */
public abstract class PedroSubsystem extends FtcSubsystem {

    /**
     * Speed value meaning "leave the follower's configured speed alone".
     * <p>
     * Pedro expresses a path speed cap as a fraction of the robot's maximum
     * achievable velocity, and its own "no limit" sentinel is infinity.
     */
    public static final double NO_SPEED_LIMIT = Double.POSITIVE_INFINITY;

    protected final Follower follower;

    protected PedroSubsystem(HardwareMap hardwareMap, Follower follower) {
        super(hardwareMap);
        this.follower = follower;
    }

    @Override
    public void periodic() {
        follower.update();
    }

    public Follower getFollower() {
        return follower;
    }

    /**
     * Command helper to follow a path at the follower's configured speed.
     */
    public Command follow(Path path) {
        return new FollowPathCommand(this, path);
    }

    /**
     * Command helper to follow a path with a capped speed.
     *
     * @param maxSpeed fraction of the robot's maximum achievable velocity (0 &lt; maxSpeed)
     */
    public Command follow(Path path, double maxSpeed) {
        return new FollowPathCommand(this, path, maxSpeed);
    }

    /**
     * Command helper that finishes once the follower has settled after its path.
     */
    public Command waitUntilIdle() {
        return new WaitUntilCommand(this::isIdle);
    }

    /**
     * Stops the follower. The drivetrain is left with zero power until
     * something else takes it over.
     */
    public void stop() {
        follower.stop();
    }

    /**
     * Stops the follower as a one-shot command.
     */
    public Command stopCommand() {
        return new InstantCommand(this::stop);
    }

    /**
     * Sets the follower's default path speed cap as a one-shot command.
     * <p>
     * This changes the follower's configuration globally. To cap a single path
     * instead, use {@link #follow(Path, double)}.
     *
     * @param maxSpeed fraction of the robot's maximum achievable velocity, or
     *                 {@link #NO_SPEED_LIMIT} to remove the cap
     */
    public Command setMaxSpeed(double maxSpeed) {
        return new InstantCommand(() -> {
            ConfigVar<Double> cap = maxPathSpeed();
            if (cap != null) {
                cap.set(maxSpeed);
            }
        });
    }

    /**
     * True once the follower is done with the path it was given.
     *
     * @see FollowerState#isIdle(Follower)
     */
    public boolean isIdle() {
        return FollowerState.isIdle(follower);
    }

    /**
     * Returns the path with a speed cap attached, or the path unchanged when no
     * cap applies.
     * <p>
     * Pedro applies the cap when the path starts and reverts it when the path
     * ends, so the follower's configured speed survives the command.
     */
    public Path withSpeedLimit(Path path, double maxSpeed) {
        if (maxSpeed == NO_SPEED_LIMIT) {
            return path;
        }
        if (!(maxSpeed > 0)) {
            throw new IllegalArgumentException("maxSpeed must be positive, was " + maxSpeed);
        }
        ConfigVar<Double> cap = maxPathSpeed();
        return cap == null ? path : path.with(cap.at(maxSpeed));
    }

    /**
     * The follower's path speed cap, or null when the follower runs an
     * algorithm that has no such setting.
     */
    protected ConfigVar<Double> maxPathSpeed() {
        Algorithm algorithm = follower.algorithm();
        return algorithm instanceof Foresight ? ((Foresight) algorithm).config.maxPathSpeed : null;
    }

    /**
     * The follower's current field pose.
     */
    public Pose getPose() {
        return follower.pose();
    }
}
