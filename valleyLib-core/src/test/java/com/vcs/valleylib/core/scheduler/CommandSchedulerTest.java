package com.vcs.valleylib.core.scheduler;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandSchedulerTest {

    @AfterEach
    void tearDown() {
        CommandScheduler.getInstance().cancelAll();
    }

    @Test
    void scheduleDoesNotReinitializeAlreadyScheduledCommand() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        CountingCommand command = new CountingCommand(Set.of());

        scheduler.schedule(command);
        scheduler.schedule(command);

        assertEquals(1, command.initializeCalls);
    }

    @Test
    void defaultCommandWaitsUntilSubsystemIsIdle() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        TestSubsystem subsystem = new TestSubsystem();
        scheduler.registerSubsystem(subsystem);

        CountingCommand defaultCommand = new CountingCommand(Set.of(subsystem));
        subsystem.setDefaultCommand(defaultCommand);

        CountingCommand activeCommand = new CountingCommand(Set.of(subsystem));
        scheduler.schedule(activeCommand);
        scheduler.run();

        assertEquals(0, defaultCommand.initializeCalls);

        scheduler.cancel(activeCommand);
        scheduler.run();

        assertEquals(1, defaultCommand.initializeCalls);
    }

    private static class TestSubsystem extends Subsystem {}

    private static class CountingCommand implements Command {

        private final Set<Subsystem> requirements;
        private int initializeCalls;

        private CountingCommand(Set<Subsystem> requirements) {
            this.requirements = requirements;
        }

        @Override
        public void initialize() {
            initializeCalls++;
        }

        @Override
        public void execute() {}

        @Override
        public Set<Subsystem> getRequirements() {
            return requirements;
        }
    }
}
