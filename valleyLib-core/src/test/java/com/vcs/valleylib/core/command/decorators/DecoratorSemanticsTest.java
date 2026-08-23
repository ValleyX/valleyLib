package com.vcs.valleylib.core.command.decorators;

import com.vcs.valleylib.core.command.Command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies interruption semantics: commands cut short by a decorator or
 * group must see end(true); commands that completed naturally must see
 * end(false), exactly once.
 */
class DecoratorSemanticsTest {

    /** Finishes after a configurable number of execute() calls (never, if negative). */
    private static class ProbeCommand implements Command {
        private final int finishAfter;
        private int executes;
        int endCalls;
        Boolean lastInterrupted;
        boolean initialized;

        ProbeCommand(int finishAfter) {
            this.finishAfter = finishAfter;
        }

        @Override
        public void initialize() {
            initialized = true;
            executes = 0;
        }

        @Override
        public void execute() {
            executes++;
        }

        @Override
        public boolean isFinished() {
            return finishAfter >= 0 && executes >= finishAfter;
        }

        @Override
        public void end(boolean interrupted) {
            endCalls++;
            lastInterrupted = interrupted;
        }

        int getExecutes() {
            return executes;
        }
    }

    private static void runToCompletion(Command command, int maxCycles) {
        command.initialize();
        for (int i = 0; i < maxCycles && !command.isFinished(); i++) {
            command.execute();
        }
        assertTrue(command.isFinished(), "command did not finish within " + maxCycles + " cycles");
        command.end(false);
    }

    @Test
    void raceWinnerEndsNaturallyLosersAreInterrupted() {
        ProbeCommand winner = new ProbeCommand(1);
        ProbeCommand loser = new ProbeCommand(-1);

        runToCompletion(new RaceCommand(winner, loser), 5);

        assertEquals(1, winner.endCalls);
        assertEquals(Boolean.FALSE, winner.lastInterrupted);
        assertEquals(1, loser.endCalls);
        assertEquals(Boolean.TRUE, loser.lastInterrupted);
    }

    @Test
    void timeoutInterruptsUnfinishedInnerCommand() {
        ProbeCommand inner = new ProbeCommand(-1);

        runToCompletion(new TimeoutCommand(inner, 0), 5);

        assertEquals(1, inner.endCalls);
        assertEquals(Boolean.TRUE, inner.lastInterrupted);
    }

    @Test
    void timeoutPassesNaturalFinishThrough() {
        ProbeCommand inner = new ProbeCommand(1);

        runToCompletion(new TimeoutCommand(inner, 10_000), 5);

        assertEquals(1, inner.endCalls);
        assertEquals(Boolean.FALSE, inner.lastInterrupted);
    }

    @Test
    void untilConditionInterruptsInnerCommand() {
        ProbeCommand inner = new ProbeCommand(-1);

        runToCompletion(new UntilCommand(inner, () -> true), 5);

        assertEquals(1, inner.endCalls);
        assertEquals(Boolean.TRUE, inner.lastInterrupted);
    }

    @Test
    void onlyWhileConditionInterruptsInnerCommand() {
        ProbeCommand inner = new ProbeCommand(-1);

        runToCompletion(new OnlyWhileCommand(inner, () -> false), 5);

        assertEquals(1, inner.endCalls);
        assertEquals(Boolean.TRUE, inner.lastInterrupted);
    }

    @Test
    void unlessSkippedCommandIsNeverTouched() {
        ProbeCommand inner = new ProbeCommand(1);

        runToCompletion(new UnlessCommand(inner, () -> true), 5);

        assertFalse(inner.initialized);
        assertEquals(0, inner.endCalls);
    }

    @Test
    void unlessRunsCommandWhenConditionFalse() {
        ProbeCommand inner = new ProbeCommand(1);

        runToCompletion(new UnlessCommand(inner, () -> false), 5);

        assertTrue(inner.initialized);
        assertEquals(1, inner.endCalls);
        assertEquals(Boolean.FALSE, inner.lastInterrupted);
    }

    @Test
    void deadlineStopsExecutingFinishedOthersAndEndsThemOnce() {
        ProbeCommand deadline = new ProbeCommand(3);
        ProbeCommand earlyOther = new ProbeCommand(1);
        ProbeCommand endlessOther = new ProbeCommand(-1);

        runToCompletion(new DeadlineCommand(deadline, earlyOther, endlessOther), 10);

        // The early finisher ended naturally, exactly once, and stopped executing.
        assertEquals(1, earlyOther.endCalls);
        assertEquals(Boolean.FALSE, earlyOther.lastInterrupted);
        assertEquals(1, earlyOther.getExecutes());

        // The endless one was interrupted when the deadline finished.
        assertEquals(1, endlessOther.endCalls);
        assertEquals(Boolean.TRUE, endlessOther.lastInterrupted);

        // The deadline itself finished naturally.
        assertEquals(1, deadline.endCalls);
        assertEquals(Boolean.FALSE, deadline.lastInterrupted);
    }
}
