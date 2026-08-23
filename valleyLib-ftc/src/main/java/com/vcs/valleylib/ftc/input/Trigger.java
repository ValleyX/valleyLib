package com.vcs.valleylib.ftc.input;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.scheduler.CommandScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * FTC-friendly trigger primitive inspired by WPILib/NextFTC trigger pipelines.
 * <p>
 * Binding a command (onTrue, whileTrue, ...) automatically registers the
 * trigger with {@link TriggerManager#getDefault()}, which CommandOpMode polls
 * every loop — so bindings work without manual bind() calls. Standalone users
 * can still poll their own {@link TriggerManager}.
 * <p>
 * A Trigger is also a {@link BooleanSupplier}, so it can be passed directly
 * to command decorators like {@code onlyWhile(...)} and {@code until(...)}.
 */
public class Trigger implements BooleanSupplier {

    private final BooleanSupplier condition;
    private final List<Runnable> bindings = new ArrayList<>();

    public Trigger(BooleanSupplier condition) {
        this.condition = condition;
    }

    /**
     * @return the current value of the trigger's condition
     */
    @Override
    public boolean getAsBoolean() {
        return condition.getAsBoolean();
    }

    public Trigger and(BooleanSupplier other) {
        return new Trigger(() -> condition.getAsBoolean() && other.getAsBoolean());
    }

    public Trigger and(Trigger other) {
        return and(other.condition);
    }

    public Trigger or(BooleanSupplier other) {
        return new Trigger(() -> condition.getAsBoolean() || other.getAsBoolean());
    }

    public Trigger or(Trigger other) {
        return or(other.condition);
    }

    public Trigger negate() {
        return new Trigger(() -> !condition.getAsBoolean());
    }

    /**
     * Returns a trigger that updates to true only after the condition has stayed
     * true continuously for the provided duration.
     */
    public Trigger debounce(double seconds) {
        return new Trigger(new DebouncedBooleanSupplier(condition, seconds));
    }

    public Trigger onTrue(Command command) {
        return addBinding(command, TriggerEvent.ON_TRUE);
    }

    public Trigger onFalse(Command command) {
        return addBinding(command, TriggerEvent.ON_FALSE);
    }

    public Trigger onChange(Command command) {
        return addBinding(command, TriggerEvent.ON_CHANGE);
    }

    public Trigger whileTrue(Command command) {
        return addBinding(command, TriggerEvent.WHILE_TRUE);
    }

    public Trigger whileFalse(Command command) {
        return addBinding(command, TriggerEvent.WHILE_FALSE);
    }

    public Trigger toggleOnTrue(Command command) {
        return addBinding(command, TriggerEvent.TOGGLE_ON_TRUE);
    }

    private Trigger addBinding(Command command, TriggerEvent event) {
        bindings.add(new TriggerBinding(condition, command, event));
        TriggerManager.getDefault().bind(this);
        return this;
    }

    List<Runnable> getBindings() {
        return bindings;
    }

    private enum TriggerEvent {
        ON_TRUE,
        ON_FALSE,
        ON_CHANGE,
        WHILE_TRUE,
        WHILE_FALSE,
        TOGGLE_ON_TRUE
    }

    private static class TriggerBinding implements Runnable {

        private final BooleanSupplier condition;
        private final Command command;
        private final TriggerEvent event;
        private boolean previous;

        private TriggerBinding(BooleanSupplier condition, Command command, TriggerEvent event) {
            this.condition = condition;
            this.command = command;
            this.event = event;
        }

        @Override
        public void run() {
            boolean current = condition.getAsBoolean();
            CommandScheduler scheduler = CommandScheduler.getInstance();
            switch (event) {
                case ON_TRUE:
                    if (current && !previous) {
                        scheduler.schedule(command);
                    }
                    break;
                case ON_FALSE:
                    if (!current && previous) {
                        scheduler.schedule(command);
                    }
                    break;
                case ON_CHANGE:
                    if (current != previous) {
                        scheduler.schedule(command);
                    }
                    break;
                case WHILE_TRUE:
                    if (current) {
                        if (!scheduler.isScheduled(command)) {
                            scheduler.schedule(command);
                        }
                    } else if (previous) {
                        scheduler.cancel(command);
                    }
                    break;
                case WHILE_FALSE:
                    if (!current) {
                        if (!scheduler.isScheduled(command)) {
                            scheduler.schedule(command);
                        }
                    } else if (!previous) {
                        scheduler.cancel(command);
                    }
                    break;
                case TOGGLE_ON_TRUE:
                    if (current && !previous) {
                        if (scheduler.isScheduled(command)) {
                            scheduler.cancel(command);
                        } else {
                            scheduler.schedule(command);
                        }
                    }
                    break;
            }
            previous = current;
        }
    }

    private static class DebouncedBooleanSupplier implements BooleanSupplier {

        private final BooleanSupplier source;
        private final long debounceNanos;

        private boolean lastSample;
        private long changeTimestampNanos;

        private DebouncedBooleanSupplier(BooleanSupplier source, double seconds) {
            this.source = source;
            this.debounceNanos = (long) (seconds * 1_000_000_000L);
            this.lastSample = source.getAsBoolean();
            this.changeTimestampNanos = System.nanoTime();
        }

        @Override
        public boolean getAsBoolean() {
            boolean sample = source.getAsBoolean();
            long now = System.nanoTime();

            if (sample != lastSample) {
                lastSample = sample;
                changeTimestampNanos = now;
            }

            return sample && (now - changeTimestampNanos) >= debounceNanos;
        }
    }
}
