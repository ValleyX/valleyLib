package com.vcs.valleylib.core.fsm;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.scheduler.CommandScheduler;
import com.vcs.valleylib.core.subsystem.Subsystem;
import com.vcs.valleylib.core.time.ManualClock;
import com.vcs.valleylib.core.time.RobotClock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateMachineTest {

    private enum Cycle { IDLE, INTAKE, TRANSFER, SCORE, DONE }

    /** Finishes after a configurable number of execute() calls (never, if negative). */
    private static class ProbeCommand implements Command {
        private final int finishAfter;
        private int executes;
        int initializeCalls;
        int endCalls;
        Boolean lastInterrupted;

        ProbeCommand(int finishAfter) {
            this.finishAfter = finishAfter;
        }

        @Override
        public void initialize() {
            initializeCalls++;
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
    }

    private static class TestSubsystem extends Subsystem {}

    @AfterEach
    void tearDown() {
        CommandScheduler.getInstance().reset();
    }

    @Test
    void initializeEntersInitialStateAndStartsItsCommand() {
        ProbeCommand idle = new ProbeCommand(-1);
        List<String> log = new ArrayList<>();

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.IDLE)
                .state(Cycle.IDLE, idle)
                .onEnter(Cycle.IDLE, () -> log.add("enter"));

        assertNull(fsm.getState());
        fsm.initialize();

        assertEquals(Cycle.IDLE, fsm.getState());
        assertEquals(List.of("enter"), log);
        assertEquals(1, idle.initializeCalls);
    }

    @Test
    void conditionTransitionInterruptsRunningCommandAndRunsExitEnter() {
        AtomicBoolean hasPiece = new AtomicBoolean(false);
        ProbeCommand intake = new ProbeCommand(-1);
        ProbeCommand transfer = new ProbeCommand(-1);
        List<String> log = new ArrayList<>();

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.INTAKE)
                .state(Cycle.INTAKE, intake)
                .onExit(Cycle.INTAKE, () -> log.add("exit-intake"))
                .state(Cycle.TRANSFER, transfer)
                .onEnter(Cycle.TRANSFER, () -> log.add("enter-transfer"))
                .transition(Cycle.INTAKE, Cycle.TRANSFER, hasPiece::get)
                .onTransition((from, to) -> log.add(from + "->" + to));

        fsm.initialize();
        fsm.execute();
        assertEquals(Cycle.INTAKE, fsm.getState());

        hasPiece.set(true);
        fsm.execute();

        assertEquals(Cycle.TRANSFER, fsm.getState());
        assertEquals(Cycle.INTAKE, fsm.getPreviousState());
        assertEquals(1, intake.endCalls);
        assertEquals(Boolean.TRUE, intake.lastInterrupted);
        assertEquals(1, transfer.initializeCalls);
        assertEquals(List.of("exit-intake", "enter-transfer", "INTAKE->TRANSFER"), log);
    }

    @Test
    void transitionOnFinishWaitsForStateCommand() {
        ProbeCommand transfer = new ProbeCommand(2);
        ProbeCommand score = new ProbeCommand(-1);

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.TRANSFER)
                .state(Cycle.TRANSFER, transfer)
                .state(Cycle.SCORE, score)
                .transitionOnFinish(Cycle.TRANSFER, Cycle.SCORE);

        fsm.initialize();
        fsm.execute();
        assertEquals(Cycle.TRANSFER, fsm.getState());
        assertFalse(fsm.isStateCommandFinished());

        fsm.execute();
        assertEquals(Cycle.SCORE, fsm.getState());
        assertEquals(1, transfer.endCalls);
        assertEquals(Boolean.FALSE, transfer.lastInterrupted);
    }

    @Test
    void stateWithoutCommandIsPassThroughForTransitionOnFinish() {
        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.IDLE)
                .transitionOnFinish(Cycle.IDLE, Cycle.INTAKE);

        fsm.initialize();
        assertTrue(fsm.isStateCommandFinished());
        fsm.execute();
        assertEquals(Cycle.INTAKE, fsm.getState());
    }

    @Test
    void transitionAfterUsesTimeInState() {
        ManualClock clock = new ManualClock();
        RobotClock.setClock(clock);
        try {
            StateMachine<Cycle> fsm = new StateMachine<>(Cycle.SCORE)
                    .transitionAfter(Cycle.SCORE, 0.5, Cycle.IDLE);

            fsm.initialize();
            clock.advance(0.49);
            fsm.execute();
            assertEquals(Cycle.SCORE, fsm.getState());
            assertEquals(0.49, fsm.getTimeInState(), 1e-9);

            clock.advance(0.01);
            fsm.execute();
            assertEquals(Cycle.IDLE, fsm.getState());
            assertEquals(0.0, fsm.getTimeInState(), 1e-9);
        } finally {
            RobotClock.useSystemClock();
        }
    }

    @Test
    void globalTransitionsWinOverStateTransitionsAndSkipCurrentState() {
        AtomicBoolean abort = new AtomicBoolean(false);
        List<String> log = new ArrayList<>();

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.INTAKE)
                .transition(Cycle.INTAKE, Cycle.TRANSFER, () -> true)
                .transitionFromAny(Cycle.IDLE, abort::get)
                .onTransition((from, to) -> log.add(from + "->" + to));

        fsm.initialize();
        abort.set(true);
        fsm.execute();
        assertEquals(Cycle.IDLE, fsm.getState());

        // Still held: must not re-enter IDLE every cycle.
        fsm.execute();
        fsm.execute();
        assertEquals(List.of("INTAKE->IDLE"), log);
    }

    @Test
    void atMostOneTransitionPerCycle() {
        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.IDLE)
                .transition(Cycle.IDLE, Cycle.INTAKE, () -> true)
                .transition(Cycle.INTAKE, Cycle.TRANSFER, () -> true);

        fsm.initialize();
        fsm.execute();
        assertEquals(Cycle.INTAKE, fsm.getState());
        fsm.execute();
        assertEquals(Cycle.TRANSFER, fsm.getState());
    }

    @Test
    void transitionsEvaluateInDeclarationOrder() {
        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.IDLE)
                .transition(Cycle.IDLE, Cycle.INTAKE, () -> true)
                .transition(Cycle.IDLE, Cycle.SCORE, () -> true);

        fsm.initialize();
        fsm.execute();
        assertEquals(Cycle.INTAKE, fsm.getState());
    }

    @Test
    void selfTransitionRestartsStateCommand() {
        AtomicBoolean retry = new AtomicBoolean(false);
        ProbeCommand intake = new ProbeCommand(-1);

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.INTAKE)
                .state(Cycle.INTAKE, intake)
                .transition(Cycle.INTAKE, Cycle.INTAKE, retry::get);

        fsm.initialize();
        retry.set(true);
        fsm.execute();

        assertEquals(Cycle.INTAKE, fsm.getState());
        assertEquals(2, intake.initializeCalls);
        assertEquals(1, intake.endCalls);
        assertEquals(Boolean.TRUE, intake.lastInterrupted);
    }

    @Test
    void terminalStateFinishesMachineAfterItsCommandCompletes() {
        ProbeCommand park = new ProbeCommand(2);

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.SCORE)
                .transition(Cycle.SCORE, Cycle.DONE, () -> true)
                .state(Cycle.DONE, park)
                .terminal(Cycle.DONE);

        fsm.initialize();
        fsm.execute();                       // SCORE -> DONE, park initialized
        assertEquals(Cycle.DONE, fsm.getState());
        assertFalse(fsm.isFinished());

        fsm.execute();
        assertFalse(fsm.isFinished());
        fsm.execute();                       // park finishes
        assertTrue(fsm.isFinished());
        assertEquals(Boolean.FALSE, park.lastInterrupted);
    }

    @Test
    void commandlessTerminalStateFinishesImmediately() {
        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.SCORE)
                .transition(Cycle.SCORE, Cycle.DONE, () -> true)
                .terminal(Cycle.DONE);

        fsm.initialize();
        assertFalse(fsm.isFinished());
        fsm.execute();
        assertTrue(fsm.isFinished());
    }

    @Test
    void endingMachineInterruptsStateCommandAndRunsExit() {
        ProbeCommand intake = new ProbeCommand(-1);
        List<String> log = new ArrayList<>();

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.INTAKE)
                .state(Cycle.INTAKE, intake)
                .onExit(Cycle.INTAKE, () -> log.add("exit"));

        fsm.initialize();
        fsm.execute();
        fsm.end(true);

        assertEquals(1, intake.endCalls);
        assertEquals(Boolean.TRUE, intake.lastInterrupted);
        assertEquals(List.of("exit"), log);
    }

    @Test
    void reinitializingRestartsFromInitialState() {
        ProbeCommand idle = new ProbeCommand(-1);

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.IDLE)
                .state(Cycle.IDLE, idle)
                .transition(Cycle.IDLE, Cycle.INTAKE, () -> true);

        fsm.initialize();
        fsm.execute();
        assertEquals(Cycle.INTAKE, fsm.getState());
        fsm.end(true);

        fsm.initialize();
        assertEquals(Cycle.IDLE, fsm.getState());
        assertNull(fsm.getPreviousState());
        assertEquals(2, idle.initializeCalls);
    }

    @Test
    void forceStateBypassesConditions() {
        ProbeCommand intake = new ProbeCommand(-1);
        List<String> log = new ArrayList<>();

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.INTAKE)
                .state(Cycle.INTAKE, intake)
                .onEnter(Cycle.IDLE, () -> log.add("enter-idle"))
                .onTransition((from, to) -> log.add(from + "->" + to));

        fsm.initialize();
        fsm.forceState(Cycle.IDLE);

        assertEquals(Cycle.IDLE, fsm.getState());
        assertEquals(Boolean.TRUE, intake.lastInterrupted);
        assertEquals(List.of("enter-idle", "INTAKE->IDLE"), log);
    }

    @Test
    void inSupplierTracksState() {
        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.IDLE)
                .transition(Cycle.IDLE, Cycle.SCORE, () -> true);
        var inScore = fsm.in(Cycle.SCORE);

        assertFalse(inScore.getAsBoolean());
        fsm.initialize();
        fsm.execute();
        assertTrue(inScore.getAsBoolean());
        assertTrue(fsm.isIn(Cycle.SCORE));
    }

    @Test
    void requirementsAreUnionOfStateCommands() {
        TestSubsystem drive = new TestSubsystem();
        TestSubsystem intake = new TestSubsystem();

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.INTAKE)
                .state(Cycle.INTAKE, intake.run(() -> {}))
                .state(Cycle.SCORE, drive.run(() -> {}));

        assertEquals(Set.of(drive, intake), fsm.getRequirements());
    }

    @Test
    void runsUnderTheSchedulerAndPreemptsConflictingCommands() {
        CommandScheduler scheduler = CommandScheduler.getInstance();
        TestSubsystem drive = new TestSubsystem();
        Command manualDrive = drive.run(() -> {});
        scheduler.schedule(manualDrive);

        StateMachine<Cycle> fsm = new StateMachine<>(Cycle.SCORE)
                .state(Cycle.SCORE, drive.run(() -> {}))
                .transitionAfter(Cycle.SCORE, 0, Cycle.DONE)
                .terminal(Cycle.DONE);

        scheduler.schedule(fsm);
        assertFalse(scheduler.isScheduled(manualDrive));
        assertTrue(scheduler.isScheduled(fsm));

        scheduler.run();
        assertFalse(scheduler.isScheduled(fsm));
        assertNull(scheduler.requiring(drive));
    }
}
