package com.vcs.valleylib.core.command.group;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandGroupTest {

    @Test
    void sequentialGroupCanBeReinitialized() {
        CountingCommand first = new CountingCommand(1, Set.of());
        CountingCommand second = new CountingCommand(1, Set.of());
        SequentialCommandGroup group = new SequentialCommandGroup(first, second);

        runToCompletion(group);
        runToCompletion(group);

        assertEquals(2, first.initializeCalls);
        assertEquals(2, second.initializeCalls);
    }

    @Test
    void parallelGroupEndsActiveCommandsOnInterrupt() {
        CountingCommand neverFinishes = new CountingCommand(Integer.MAX_VALUE, Set.of());
        ParallelCommandGroup group = new ParallelCommandGroup(neverFinishes);

        group.initialize();
        group.execute();
        group.end(true);

        assertEquals(1, neverFinishes.interruptedEndCalls);
    }

    @Test
    void groupsAggregateRequirements() {
        TestSubsystem subsystemA = new TestSubsystem();
        TestSubsystem subsystemB = new TestSubsystem();

        CountingCommand cmdA = new CountingCommand(1, Set.of(subsystemA));
        CountingCommand cmdB = new CountingCommand(1, Set.of(subsystemB));

        SequentialCommandGroup sequential = new SequentialCommandGroup(cmdA, cmdB);
        ParallelCommandGroup parallel = new ParallelCommandGroup(cmdA, cmdB);

        assertEquals(Set.of(subsystemA, subsystemB), sequential.getRequirements());
        assertEquals(Set.of(subsystemA, subsystemB), parallel.getRequirements());
    }

    private static void runToCompletion(Command command) {
        command.initialize();
        while (!command.isFinished()) {
            command.execute();
        }
        command.end(false);
    }

    private static class TestSubsystem extends Subsystem {}

    private static class CountingCommand implements Command {

        private final int loopsToFinish;
        private final Set<Subsystem> requirements;

        private int executeLoops;
        private int initializeCalls;
        private int interruptedEndCalls;

        private CountingCommand(int loopsToFinish, Set<Subsystem> requirements) {
            this.loopsToFinish = loopsToFinish;
            this.requirements = requirements;
        }

        @Override
        public void initialize() {
            initializeCalls++;
            executeLoops = 0;
        }

        @Override
        public void execute() {
            executeLoops++;
        }

        @Override
        public void end(boolean interrupted) {
            if (interrupted) {
                interruptedEndCalls++;
            }
        }

        @Override
        public boolean isFinished() {
            return executeLoops >= loopsToFinish;
        }

        @Override
        public Set<Subsystem> getRequirements() {
            return requirements;
        }
    }
}
