package com.vcs.valleylib.core.command;

import com.vcs.valleylib.core.fsm.StateMachine;
import com.vcs.valleylib.core.scheduler.CommandScheduler;
import com.vcs.valleylib.core.subsystem.Subsystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionalCommandTest {

    private static class Intake extends Subsystem {}
    private static class Transfer extends Subsystem {}

    private enum Mode { A, B }

    @AfterEach
    void tearDown() {
        CommandScheduler.getInstance().reset();
    }

    @Test
    void lifecycleLambdasAreInvokedInOrder() {
        List<String> log = new ArrayList<>();
        Command command = new FunctionalCommand(
                () -> log.add("init"),
                () -> log.add("exec"),
                interrupted -> log.add("end:" + interrupted),
                () -> log.size() >= 2);

        command.initialize();
        command.execute();
        assertTrue(command.isFinished());
        command.end(false);

        assertEquals(List.of("init", "exec", "end:false"), log);
    }

    @Test
    void commandsFactoriesAcceptMultipleRequirements() {
        Intake intake = new Intake();
        Transfer transfer = new Transfer();

        assertEquals(Set.of(intake, transfer),
                Commands.run(() -> {}, intake, transfer).getRequirements());
        assertEquals(Set.of(intake, transfer),
                Commands.runOnce(() -> {}, intake, transfer).getRequirements());
        assertEquals(Set.of(intake),
                Commands.startEnd(() -> {}, () -> {}, intake).getRequirements());
        assertEquals(Set.of(transfer),
                Commands.runEnd(() -> {}, () -> {}, transfer).getRequirements());
        assertTrue(Commands.run(() -> {}).getRequirements().isEmpty());
    }

    @Test
    void runEndRunsEveryCycleAndEndsOnInterrupt() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        List<String> log = new ArrayList<>();

        Command command = Commands.runEnd(() -> log.add("run"), () -> log.add("end"));
        scheduler.schedule(command);
        scheduler.run();
        scheduler.run();
        scheduler.cancel(command);

        assertEquals(List.of("run", "run", "end"), log);
    }

    @Test
    void idleHoldsSubsystemWithoutDoingAnything() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        Intake intake = new Intake();

        Command idle = intake.idle();
        scheduler.schedule(idle);
        scheduler.run();

        assertTrue(scheduler.isScheduled(idle));
        assertEquals(idle, scheduler.requiring(intake));
        assertEquals("Intake.idle", idle.getName());
    }

    @Test
    void factoryCommandsHaveReadableNames() {
        Intake intake = new Intake();

        assertEquals("run", Commands.run(() -> {}).getName());
        assertEquals("startEnd", Commands.startEnd(() -> {}, () -> {}).getName());
        assertEquals("none", Commands.none().getName());
        assertEquals("Intake.run", intake.run(() -> {}).getName());
        assertEquals("Intake.startEnd", intake.startEnd(() -> {}, () -> {}).getName());
        assertEquals("Wait(0.5s)", Commands.waitSeconds(0.5).getName());
    }

    @Test
    void withNameSurvivesDecorators() {
        Command scored = Commands.run(() -> {}).withName("Score").withTimeout(1.0).finallyDo(() -> {});
        assertEquals("Score", scored.getName());
    }

    @Test
    void anonymousCommandsFallBackToGenericName() {
        Command anonymous = new Command() {
            @Override
            public void execute() {}
        };
        assertEquals("Command", anonymous.getName());
        assertEquals("InstantCommand", new InstantCommand(() -> {}).getName());
    }

    @Test
    void stateMachineNameReflectsCurrentState() {
        StateMachine<Mode> fsm = new StateMachine<>(Mode.A)
                .transition(Mode.A, Mode.B, () -> true);

        assertEquals("StateMachine[A]", fsm.getName());
        fsm.initialize();
        fsm.execute();
        assertEquals("StateMachine[B]", fsm.getName());
        assertFalse(fsm.isFinished());
    }
}
