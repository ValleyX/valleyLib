package com.vcs.valleylib.core.subsystem;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.scheduler.CommandScheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubsystemFactoryTest {

    @AfterEach
    void tearDown() {
        CommandScheduler.getInstance().reset();
    }

    private static class TestSubsystem extends Subsystem {}

    @Test
    void factoryCommandsRequireTheSubsystem() {
        TestSubsystem subsystem = new TestSubsystem();

        assertEquals(Set.of(subsystem), subsystem.runOnce(() -> {}).getRequirements());
        assertEquals(Set.of(subsystem), subsystem.run(() -> {}).getRequirements());
        assertEquals(Set.of(subsystem), subsystem.startEnd(() -> {}, () -> {}).getRequirements());
        assertEquals(Set.of(subsystem), subsystem.runEnd(() -> {}, () -> {}).getRequirements());
    }

    @Test
    void factoryCommandPreemptsCommandOnSameSubsystem() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        TestSubsystem subsystem = new TestSubsystem();

        Command first = subsystem.run(() -> {});
        Command second = subsystem.run(() -> {});

        scheduler.schedule(first);
        assertSame(first, scheduler.requiring(subsystem));

        scheduler.schedule(second);
        assertFalse(scheduler.isScheduled(first));
        assertTrue(scheduler.isScheduled(second));
        assertSame(second, scheduler.requiring(subsystem));
    }

    @Test
    void runOnceRunsExactlyOnceThenFinishes() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        TestSubsystem subsystem = new TestSubsystem();
        AtomicInteger runs = new AtomicInteger();

        Command command = subsystem.runOnce(runs::incrementAndGet);
        scheduler.schedule(command);
        scheduler.run();
        scheduler.run();

        assertEquals(1, runs.get());
        assertFalse(scheduler.isScheduled(command));
        assertNull(scheduler.requiring(subsystem));
    }

    @Test
    void startEndRunsEndOnInterruption() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        TestSubsystem subsystem = new TestSubsystem();
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger ends = new AtomicInteger();

        Command command = subsystem.startEnd(starts::incrementAndGet, ends::incrementAndGet);
        scheduler.schedule(command);
        scheduler.run();
        scheduler.cancel(command);

        assertEquals(1, starts.get());
        assertEquals(1, ends.get());
    }

    @Test
    void runEndRunsEveryCycleAndEndsOnInterruption() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        TestSubsystem subsystem = new TestSubsystem();
        AtomicInteger runs = new AtomicInteger();
        AtomicInteger ends = new AtomicInteger();

        Command command = subsystem.runEnd(runs::incrementAndGet, ends::incrementAndGet);
        scheduler.schedule(command);
        scheduler.run();
        scheduler.run();
        scheduler.cancel(command);

        assertEquals(2, runs.get());
        assertEquals(1, ends.get());
    }

    @Test
    void schedulerExposesScheduledCommandsView() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        TestSubsystem subsystem = new TestSubsystem();

        Command command = subsystem.run(() -> {});
        scheduler.schedule(command);

        assertTrue(scheduler.getScheduledCommands().contains(command));

        scheduler.cancel(command);
        assertFalse(scheduler.getScheduledCommands().contains(command));
    }
}
