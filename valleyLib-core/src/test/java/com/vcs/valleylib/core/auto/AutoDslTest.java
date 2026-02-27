package com.vcs.valleylib.core.auto;

import com.vcs.valleylib.core.command.Command;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoDslTest {

    @Test
    void autoBuilderCreatesSequentialTimeline() {
        List<String> events = new ArrayList<>();

        Command auto = AutoDsl.auto(builder -> builder
                .run(() -> events.add("start"))
                .command(new OneShotCommand(() -> events.add("middle")))
                .run(() -> events.add("end"))
        );

        runToCompletion(auto);

        assertEquals(List.of("start", "middle", "end"), events);
    }


    @Test
    void autoBuilderSupportsCompatibilityAliases() {
        List<String> events = new ArrayList<>();
        AtomicBoolean branch = new AtomicBoolean(false);

        Command auto = AutoDsl.auto(builder -> builder
                .doInstant(() -> events.add("start"))
                .add(new OneShotCommand(() -> events.add("added")))
                .waitFor(0.0)
                .ifElse(branch::get,
                        new OneShotCommand(() -> events.add("if-true")),
                        new OneShotCommand(() -> events.add("if-false")))
        );

        runToCompletion(auto);

        assertEquals(List.of("start", "added", "if-false"), events);
    }

    @Test
    void autoBuilderSupportsConditionalsAndMarkers() {
        List<String> events = new ArrayList<>();
        AtomicBoolean branch = new AtomicBoolean(true);

        Command auto = AutoDsl.auto(builder -> builder
                .marker("init", events::add)
                .when(() -> true, new OneShotCommand(() -> events.add("when-true")))
                .either(branch::get,
                        new OneShotCommand(() -> events.add("branch-true")),
                        new OneShotCommand(() -> events.add("branch-false")))
        );

        runToCompletion(auto);
        assertEquals(List.of("init", "when-true", "branch-true"), events);
    }

    private static void runToCompletion(Command command) {
        command.initialize();
        while (!command.isFinished()) {
            command.execute();
        }
        command.end(false);
    }

    private static class OneShotCommand implements Command {

        private final Runnable action;
        private boolean done;

        private OneShotCommand(Runnable action) {
            this.action = action;
        }

        @Override
        public void initialize() {
            done = false;
        }

        @Override
        public void execute() {
            if (!done) {
                action.run();
                done = true;
            }
        }

        @Override
        public boolean isFinished() {
            return done;
        }
    }
}
