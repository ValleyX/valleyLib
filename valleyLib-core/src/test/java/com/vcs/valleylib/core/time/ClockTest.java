package com.vcs.valleylib.core.time;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.command.Commands;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockTest {

    private ManualClock clock;

    @BeforeEach
    void setUp() {
        clock = new ManualClock();
        RobotClock.setClock(clock);
    }

    @AfterEach
    void tearDown() {
        RobotClock.useSystemClock();
    }

    @Test
    void manualClockOnlyMovesWhenAdvanced() {
        assertEquals(0.0, RobotClock.seconds());
        clock.advance(1.5);
        assertEquals(1.5, RobotClock.seconds(), 1e-9);
        assertEquals(1_500_000_000L, RobotClock.nanos());
        assertEquals(1500L, RobotClock.millis());
        clock.set(10);
        assertEquals(10.0, RobotClock.seconds(), 1e-9);
    }

    @Test
    void manualClockRejectsGoingBackwards() {
        assertThrows(IllegalArgumentException.class, () -> clock.advance(-0.1));
    }

    @Test
    void useSystemClockRestoresDefault() {
        assertFalse(RobotClock.isSystemClock());
        RobotClock.useSystemClock();
        assertTrue(RobotClock.isSystemClock());
        assertTrue(RobotClock.nanos() > 0);
    }

    @Test
    void waitCommandIsDeterministicUnderManualClock() {
        Command wait = Commands.waitSeconds(0.5);
        wait.initialize();

        assertFalse(wait.isFinished());
        clock.advance(0.49);
        assertFalse(wait.isFinished());
        clock.advance(0.01);
        assertTrue(wait.isFinished());
    }

    @Test
    void timeoutDecoratorIsDeterministicUnderManualClock() {
        Command endless = Commands.run(() -> {}).withTimeout(2.0);
        endless.initialize();
        endless.execute();

        assertFalse(endless.isFinished());
        clock.advance(1.999);
        endless.execute();
        assertFalse(endless.isFinished());
        clock.advance(0.001);
        endless.execute();
        assertTrue(endless.isFinished());
    }
}
