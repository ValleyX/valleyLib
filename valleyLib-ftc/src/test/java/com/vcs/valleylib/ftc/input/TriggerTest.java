package com.vcs.valleylib.ftc.input;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.scheduler.CommandScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriggerTest {

    @AfterEach
    void tearDown() {
        CommandScheduler.getInstance().reset();
        TriggerManager.getDefault().clear();
    }

    @Test
    void onChangeSchedulesOnBothEdges() {
        AtomicBoolean state = new AtomicBoolean(false);
        TriggerManager manager = new TriggerManager();
        CountingCommand command = new CountingCommand();

        manager.bind(new Trigger(state::get).onChange(command));

        manager.poll();
        state.set(true);
        manager.poll();
        CommandScheduler.getInstance().run();

        state.set(false);
        manager.poll();
        CommandScheduler.getInstance().run();

        assertEquals(2, command.initializeCalls);
    }

    @Test
    void andCompositionRequiresBothConditions() {
        AtomicBoolean a = new AtomicBoolean(false);
        AtomicBoolean b = new AtomicBoolean(false);
        TriggerManager manager = new TriggerManager();
        CountingCommand command = new CountingCommand();

        manager.bind(new Trigger(a::get).and(b::get).onTrue(command));

        manager.poll();
        a.set(true);
        manager.poll();
        b.set(true);
        manager.poll();

        assertEquals(1, command.initializeCalls);
    }

    @Test
    void bindingAutoRegistersWithDefaultManager() {
        AtomicBoolean state = new AtomicBoolean(false);
        CountingCommand command = new CountingCommand();

        // No manual bind: onTrue alone must be enough.
        new Trigger(state::get).onTrue(command);

        TriggerManager.getDefault().poll();
        state.set(true);
        TriggerManager.getDefault().poll();

        assertEquals(1, command.initializeCalls);
    }

    @Test
    void manualBindPlusAutoRegistrationDoesNotDoublePoll() {
        AtomicBoolean state = new AtomicBoolean(false);
        CountingCommand onFalseCommand = new CountingCommand();

        // onFalse fires on a true -> false edge; a duplicate registration
        // would advance edge state twice per poll and could double-fire.
        Trigger trigger = new Trigger(state::get).onFalse(onFalseCommand);
        TriggerManager.getDefault().bind(trigger);
        TriggerManager.getDefault().bind(trigger);

        state.set(true);
        TriggerManager.getDefault().poll();
        state.set(false);
        TriggerManager.getDefault().poll();

        assertEquals(1, onFalseCommand.initializeCalls);
    }

    @Test
    void triggerActsAsBooleanSupplier() {
        AtomicBoolean state = new AtomicBoolean(false);
        Trigger trigger = new Trigger(state::get);

        assertEquals(false, trigger.getAsBoolean());
        state.set(true);
        assertEquals(true, trigger.getAsBoolean());
        assertEquals(false, trigger.negate().getAsBoolean());
    }

    private static class CountingCommand implements Command {
        int initializeCalls;

        @Override
        public void initialize() {
            initializeCalls++;
        }

        @Override
        public void execute() {}

        @Override
        public boolean isFinished() {
            // Finish immediately so edge-triggered tests can reschedule the
            // same instance across scheduler runs.
            return true;
        }
    }
}
