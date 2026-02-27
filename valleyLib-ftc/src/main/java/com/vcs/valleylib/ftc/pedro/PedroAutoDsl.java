package com.vcs.valleylib.ftc.pedro;

import com.pedropathing.paths.PathChain;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.Commands;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Command-first autonomous builder tailored for Pedro pathing.
 */
public final class PedroAutoDsl {

    private PedroAutoDsl() {}

    @androidx.annotation.NonNull
    public static Command auto(PedroSubsystem drive, @androidx.annotation.NonNull Consumer<Builder> block) {
        Builder builder = new Builder(drive);
        block.accept(builder);
        return builder.build();
    }

    public static final class Builder {

        private final PedroSubsystem drive;
        private final List<Command> timeline = new ArrayList<>();

        private Builder(PedroSubsystem drive) {
            this.drive = drive;
        }

        public Builder follow(PathChain path) {
            timeline.add(drive.follow(path));
            return this;
        }

        public Builder follow(PathChain path, double maxPower) {
            timeline.add(drive.follow(path, maxPower));
            return this;
        }

        public Builder action(Runnable runnable) {
            timeline.add(Commands.runOnce(runnable));
            return this;
        }

        public Builder waitSeconds(double seconds) {
            timeline.add(Commands.waitSeconds(seconds));
            return this;
        }

        public Builder waitUntilDriveIdle() {
            timeline.add(drive.waitUntilIdle());
            return this;
        }

        public Builder command(Command command) {
            timeline.add(command);
            return this;
        }

        public Builder parallel(Command... commands) {
            timeline.add(Commands.parallel(commands));
            return this;
        }

        @androidx.annotation.NonNull
        @Contract(" -> new")
        public Command build() {
            return Commands.sequence(timeline.toArray(new Command[0]));
        }
    }
}
