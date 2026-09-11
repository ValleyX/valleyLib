package com.vcs.valleylib.core.fsm;

import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.core.subsystem.Subsystem;
import com.vcs.valleylib.core.time.RobotClock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * A finite state machine that runs as a {@link Command}.
 * <p>
 * States are enum constants. Each state may run a command while active and
 * may have enter/exit actions. Transitions are evaluated once per scheduler
 * cycle, after the active state's command executes, in this order:
 * <ol>
 *   <li>global transitions ({@link #transitionFromAny}), in declaration order</li>
 *   <li>the current state's transitions, in declaration order</li>
 * </ol>
 * The first transition whose condition is true wins; at most one transition
 * happens per cycle. When a transition fires, the outgoing state's command
 * (if still running) is interrupted with {@code end(true)}, its exit action
 * runs, then the incoming state's enter action runs and its command is
 * initialized. That command starts executing on the next cycle.
 * <p>
 * The machine finishes when it is in a {@link #terminal} state and that
 * state's command (if any) has finished. It can be decorated, bound to
 * triggers, and nested inside other commands or state machines like any
 * other command. Re-scheduling it restarts from the initial state.
 * <p>
 * The hot path allocates nothing: state lookups use an {@link EnumMap} and
 * transition lists are walked by index.
 *
 * @param <S> the enum type naming the states
 */
public final class StateMachine<S extends Enum<S>> implements Command {

    /**
     * Observer for state transitions.
     */
    @FunctionalInterface
    public interface TransitionListener<S> {
        void onTransition(S from, S to);
    }

    private static final class Transition<S> {
        final S to;
        final BooleanSupplier condition;

        Transition(S to, BooleanSupplier condition) {
            this.to = to;
            this.condition = condition;
        }
    }

    private static final class StateSpec<S> {
        Command command;
        Runnable onEnter;
        Runnable onExit;
        final List<Transition<S>> transitions = new ArrayList<>();
    }

    private final S initialState;
    private final EnumMap<S, StateSpec<S>> specs;
    private final EnumSet<S> terminalStates;
    private final List<Transition<S>> globalTransitions = new ArrayList<>();
    private final List<TransitionListener<S>> listeners = new ArrayList<>();

    private S current;
    private S previous;
    private StateSpec<S> currentSpec;
    private boolean commandRunning;
    private boolean commandFinished;
    private long enterTimeNanos;

    /**
     * Creates a state machine that starts in {@code initialState} each time
     * it is initialized.
     */
    public StateMachine(S initialState) {
        this.initialState = Objects.requireNonNull(initialState, "initialState");
        Class<S> stateClass = initialState.getDeclaringClass();
        this.specs = new EnumMap<>(stateClass);
        this.terminalStates = EnumSet.noneOf(stateClass);
    }

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    /**
     * Sets the command that runs while the machine is in {@code state}.
     * The command is initialized on entry, executed every cycle, ended
     * naturally when it finishes, and interrupted if the state is left
     * while it is still running.
     */
    public StateMachine<S> state(S state, Command command) {
        spec(state).command = Objects.requireNonNull(command, "command");
        return this;
    }

    /**
     * Runs an action once each time {@code state} is entered, before its
     * command is initialized.
     */
    public StateMachine<S> onEnter(S state, Runnable action) {
        spec(state).onEnter = Objects.requireNonNull(action, "action");
        return this;
    }

    /**
     * Runs an action once each time {@code state} is left (including when
     * the machine ends while in that state), after its command is ended.
     */
    public StateMachine<S> onExit(S state, Runnable action) {
        spec(state).onExit = Objects.requireNonNull(action, "action");
        return this;
    }

    /**
     * Moves from {@code from} to {@code to} when {@code condition} is true.
     * A transition with {@code from == to} re-enters the state (exit, then
     * enter, restarting its command).
     */
    public StateMachine<S> transition(S from, S to, BooleanSupplier condition) {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(condition, "condition");
        spec(from).transitions.add(new Transition<>(to, condition));
        return this;
    }

    /**
     * Moves from {@code from} to {@code to} once the {@code from} state's
     * command has finished. A state with no command counts as finished
     * immediately, which makes it a pass-through state.
     */
    public StateMachine<S> transitionOnFinish(S from, S to) {
        return transition(from, to, () -> commandFinished);
    }

    /**
     * Moves from {@code from} to {@code to} after the machine has been in
     * {@code from} for at least {@code seconds}.
     */
    public StateMachine<S> transitionAfter(S from, double seconds, S to) {
        return transition(from, to, () -> getTimeInState() >= seconds);
    }

    /**
     * Moves from any state to {@code to} when {@code condition} is true.
     * Global transitions are evaluated before state-specific ones, so they
     * are the right tool for overrides and aborts. They never fire while
     * the machine is already in {@code to}.
     */
    public StateMachine<S> transitionFromAny(S to, BooleanSupplier condition) {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(condition, "condition");
        globalTransitions.add(new Transition<>(to, condition));
        return this;
    }

    /**
     * Marks states in which the machine finishes (once that state's
     * command, if any, has finished).
     */
    @SafeVarargs
    public final StateMachine<S> terminal(S... states) {
        for (S state : states) {
            terminalStates.add(state);
        }
        return this;
    }

    /**
     * Adds a listener notified after every transition (not on the initial
     * entry). Useful for telemetry and match logs.
     */
    public StateMachine<S> onTransition(TransitionListener<S> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    // ------------------------------------------------------------------
    // Runtime queries
    // ------------------------------------------------------------------

    /**
     * @return the active state, or null before the machine has started
     */
    public S getState() {
        return current;
    }

    /**
     * @return the state active before the most recent transition, or null
     */
    public S getPreviousState() {
        return previous;
    }

    /**
     * @return seconds since the active state was entered (0 if not started)
     */
    public double getTimeInState() {
        if (current == null) {
            return 0;
        }
        return (RobotClock.nanos() - enterTimeNanos) / 1e9;
    }

    /**
     * @return whether the machine is currently in {@code state}
     */
    public boolean isIn(S state) {
        return current == state;
    }

    /**
     * Returns a supplier that is true while the machine is in
     * {@code state}. Handy for building triggers and decorators, e.g.
     * {@code new Trigger(fsm.in(SCORE))}.
     */
    public BooleanSupplier in(S state) {
        return () -> current == state;
    }

    /**
     * @return whether the active state's command has finished (true for a
     * state with no command)
     */
    public boolean isStateCommandFinished() {
        return commandFinished;
    }

    /**
     * Forces an immediate transition to {@code state}, bypassing transition
     * conditions. The normal exit/enter sequence still runs and listeners
     * are notified. Intended for driver overrides and recovery.
     */
    public void forceState(S state) {
        Objects.requireNonNull(state, "state");
        if (current == null) {
            enter(state);
        } else {
            moveTo(state);
        }
    }

    // ------------------------------------------------------------------
    // Command lifecycle
    // ------------------------------------------------------------------

    @Override
    public void initialize() {
        previous = null;
        current = null;
        currentSpec = null;
        commandRunning = false;
        commandFinished = false;
        enter(initialState);
    }

    @Override
    public void execute() {
        if (current == null) {
            return;
        }

        if (commandRunning) {
            Command command = currentSpec.command;
            command.execute();
            if (command.isFinished()) {
                command.end(false);
                commandRunning = false;
                commandFinished = true;
            }
        }

        S next = findTransition();
        if (next != null) {
            moveTo(next);
        }
    }

    @Override
    public boolean isFinished() {
        return current != null && terminalStates.contains(current) && !commandRunning;
    }

    @Override
    public void end(boolean interrupted) {
        if (current == null) {
            return;
        }
        if (commandRunning) {
            currentSpec.command.end(true);
            commandRunning = false;
        }
        if (currentSpec.onExit != null) {
            currentSpec.onExit.run();
        }
    }

    /**
     * @return "StateMachine[STATE]" with the active (or initial) state, so
     * command logs show where the machine is
     */
    @Override
    public String getName() {
        return "StateMachine[" + (current != null ? current : initialState) + "]";
    }

    /**
     * @return the union of every state command's requirements
     */
    @Override
    public Set<Subsystem> getRequirements() {
        Set<Subsystem> requirements = new LinkedHashSet<>();
        for (StateSpec<S> spec : specs.values()) {
            if (spec.command != null) {
                requirements.addAll(spec.command.getRequirements());
            }
        }
        return requirements;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private StateSpec<S> spec(S state) {
        Objects.requireNonNull(state, "state");
        StateSpec<S> spec = specs.get(state);
        if (spec == null) {
            spec = new StateSpec<>();
            specs.put(state, spec);
        }
        return spec;
    }

    private static final StateSpec<?> EMPTY_SPEC = new StateSpec<>();

    @SuppressWarnings("unchecked")
    private StateSpec<S> specOrEmpty(S state) {
        StateSpec<S> spec = specs.get(state);
        return spec != null ? spec : (StateSpec<S>) EMPTY_SPEC;
    }

    private S findTransition() {
        // Index loops rather than iterators keep the hot path allocation-free.
        for (int i = 0; i < globalTransitions.size(); i++) {
            Transition<S> transition = globalTransitions.get(i);
            if (transition.to != current && transition.condition.getAsBoolean()) {
                return transition.to;
            }
        }
        List<Transition<S>> local = currentSpec.transitions;
        for (int i = 0; i < local.size(); i++) {
            Transition<S> transition = local.get(i);
            if (transition.condition.getAsBoolean()) {
                return transition.to;
            }
        }
        return null;
    }

    private void moveTo(S next) {
        S from = current;
        exitCurrent();
        enter(next);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onTransition(from, next);
        }
    }

    private void exitCurrent() {
        if (commandRunning) {
            currentSpec.command.end(true);
            commandRunning = false;
        }
        if (currentSpec.onExit != null) {
            currentSpec.onExit.run();
        }
        previous = current;
    }

    private void enter(S state) {
        current = state;
        currentSpec = specOrEmpty(state);
        enterTimeNanos = RobotClock.nanos();
        if (currentSpec.onEnter != null) {
            currentSpec.onEnter.run();
        }
        Command command = currentSpec.command;
        if (command != null) {
            command.initialize();
            commandRunning = true;
            commandFinished = false;
        } else {
            commandRunning = false;
            commandFinished = true;
        }
    }
}
