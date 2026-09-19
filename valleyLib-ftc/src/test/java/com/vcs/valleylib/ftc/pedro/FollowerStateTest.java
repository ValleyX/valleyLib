package com.vcs.valleylib.ftc.pedro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.api.Paths;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathTracker;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the "is the drive done?" rule against a real Pedro follower.
 *
 * The follower's localizer and drivetrain are null on purpose: these tests
 * only drive its mode transitions and never call update(), which is the only
 * thing that touches them.
 */
class FollowerStateTest {

    private static final Path PATH = Paths.line(new Pose(0, 0), new Pose(24, 0)).constant(0);

    @Test
    void followingIsNeverIdle() {
        FakeAlgorithm algorithm = new FakeAlgorithm();
        Follower follower = follower(algorithm);

        follower.follow(PATH);

        assertFalse(FollowerState.isIdle(follower));
    }

    @Test
    void holdingIsIdleOnlyOnceSettled() {
        FakeAlgorithm algorithm = new FakeAlgorithm();
        Follower follower = follower(algorithm);

        follower.follow(PATH);
        follower.hold(new Pose(24, 0, 0));

        assertFalse(FollowerState.isIdle(follower), "still settling on the end pose");

        algorithm.busy = false;
        assertTrue(FollowerState.isIdle(follower));
    }

    @Test
    void stoppedIsIdleEvenWhileBusy() {
        // The regression this guards: a follower with holdEnd = false goes
        // straight to IDLE at the end of a path and never clears its busy
        // flag, so a bare !isBusy() check waits forever.
        FakeAlgorithm algorithm = new FakeAlgorithm();
        Follower follower = follower(algorithm);

        follower.follow(PATH);
        follower.stop();

        assertTrue(algorithm.isBusy(), "precondition: the busy flag is still set");
        assertTrue(FollowerState.isIdle(follower));
    }

    @Test
    void manualControlIsIdleEvenWhileBusy() {
        FakeAlgorithm algorithm = new FakeAlgorithm();
        Follower follower = follower(algorithm);

        follower.follow(PATH);
        follower.manual(0.5, 0, 0);

        assertTrue(algorithm.isBusy(), "precondition: the busy flag is still set");
        assertTrue(FollowerState.isIdle(follower));
    }

    private static Follower follower(Algorithm algorithm) {
        return new Follower(null, null, algorithm);
    }

    /** Reports busy the way Foresight does: set on reset, cleared only on demand. */
    private static final class FakeAlgorithm implements Algorithm {

        private boolean busy;

        @Override
        public void reset() {
            busy = true;
        }

        @Override
        public boolean isBusy() {
            return busy;
        }

        @Override
        public DrivePowers calculatePath(
                Drivetrain drivetrain, PathTracker pathTracker, MotionState state, double deltaTime) {
            return DrivePowers.zero();
        }

        @Override
        public DrivePowers calculateHold(
                Drivetrain drivetrain, Pose target, MotionState state, boolean useScaling, double deltaTime) {
            return DrivePowers.zero();
        }

        @Override
        public double completion() {
            return 0;
        }

        @Override
        public Pose closestPose() {
            return Pose.zero();
        }

        @Override
        public Vector2D closestTangent() {
            return Vector2D.zero();
        }

        @Override
        public Vector2D closestNormal() {
            return Vector2D.zero();
        }

        @Override
        public double curvature() {
            return 0;
        }

        @Override
        public double remainingDistance() {
            return 0;
        }

        @Override
        public boolean atParametricEnd() {
            return false;
        }

        @Override
        public Map<String, Object> debug() {
            return Map.of();
        }
    }
}
