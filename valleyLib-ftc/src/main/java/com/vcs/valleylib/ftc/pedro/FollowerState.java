package com.vcs.valleylib.ftc.pedro;

import com.pedropathing.follower.Follower;

/**
 * Questions the command layer asks a Pedro follower, in one place.
 */
public final class FollowerState {

    private FollowerState() {}

    /**
     * True once the follower is done with the path it was given.
     * <p>
     * The follower's own {@code isBusy()} is not enough on its own. It is
     * cleared only while the follower holds the end of a path, so a follower
     * configured with {@code holdEnd = false} — or one that was stopped, or
     * handed to manual control — stays "busy" forever after its last path.
     * Mode is the reliable signal; busy only refines the holding case, where
     * it means "has settled on the end pose".
     */
    public static boolean isIdle(Follower follower) {
        if (follower.following()) {
            return false;
        }
        return !follower.holding() || !follower.isBusy();
    }
}
