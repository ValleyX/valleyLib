package com.vcs.valleylib.core.command.group;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandGroupRequirementsTest {

    @Test
    void sequentialGroupAggregatesRequirements() {
        TestSubsystem drive = new TestSubsystem();
        TestSubsystem intake = new TestSubsystem();

        SequentialCommandGroup group = new SequentialCommandGroup(
                new RequirementCommand(drive),
                new RequirementCommand(intake)
        );

        assertEquals(Set.of(drive, intake), group.getRequirements());
    }

    @Test
    void parallelGroupAggregatesRequirements() {
        TestSubsystem drive = new TestSubsystem();
        TestSubsystem intake = new TestSubsystem();

        ParallelCommandGroup group = new ParallelCommandGroup(
                new RequirementCommand(drive),
                new RequirementCommand(intake)
        );

        assertEquals(Set.of(drive, intake), group.getRequirements());
    }

    private static final class TestSubsystem extends Subsystem {}

    private static final class RequirementCommand implements Command {
        private final Subsystem subsystem;

        private RequirementCommand(Subsystem subsystem) {
            this.subsystem = subsystem;
        }

        @Override
        public void execute() {}

        @Override
        public Set<Subsystem> getRequirements() {
            return Set.of(subsystem);
        }
    }
}
