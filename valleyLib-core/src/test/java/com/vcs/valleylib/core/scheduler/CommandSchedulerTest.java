package com.vcs.valleylib.core.scheduler;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandSchedulerTest {

    private CommandScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = CommandScheduler.getInstance();
        scheduler.cancelAll();
    }

    @Test
    void defaultCommandOnlyRunsWhenSubsystemIsIdle() {
        TestSubsystem subsystem = new TestSubsystem();
        CountingCommand defaultCommand = new CountingCommand(subsystem);
        CountingCommand activeCommand = new CountingCommand(subsystem);

        subsystem.setDefaultCommand(defaultCommand);
        scheduler.registerSubsystem(subsystem);

        // First run: default command should be scheduled and executed once.
        scheduler.run();
        assertEquals(1, defaultCommand.executeCount);

        // Schedule a conflicting command that uses the same subsystem.
        scheduler.schedule(activeCommand);
        scheduler.run();

        // Default command should not execute while subsystem is busy.
        assertEquals(1, defaultCommand.executeCount);
        assertEquals(1, activeCommand.executeCount);
    }

    private static final class TestSubsystem extends Subsystem {}

    private static final class CountingCommand implements Command {
        private final Subsystem subsystem;
        int executeCount;

        private CountingCommand(Subsystem subsystem) {
            this.subsystem = subsystem;
        }

        @Override
        public void execute() {
            executeCount++;
        }

        @Override
        public Set<Subsystem> getRequirements() {
            return Set.of(subsystem);
        }
    }
}
