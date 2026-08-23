package com.vcs.valleylib.ftc.input;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Registry for command trigger bindings.
 * <p>
 * Triggers register themselves with {@link #getDefault()} automatically when
 * a command is bound, and CommandOpMode polls the default manager every loop.
 * Manual bind()/bindAll() calls remain supported (and are idempotent) for
 * standalone managers, tests, and explicit registration styles.
 */
public class TriggerManager {

    private static final TriggerManager DEFAULT_INSTANCE = new TriggerManager();

    /**
     * @return the shared manager that triggers auto-register with and that
     * CommandOpMode polls every loop
     */
    public static TriggerManager getDefault() {
        return DEFAULT_INSTANCE;
    }

    // LinkedHashSet keeps registration order while making bind() idempotent —
    // auto-registration plus a manual bindAll() must not double-poll a trigger.
    private final Set<Trigger> triggers = new LinkedHashSet<>();

    // Reused snapshot so bindings may register new triggers mid-poll.
    private final List<Trigger> pollBuffer = new ArrayList<>();

    public Trigger bind(Trigger trigger) {
        triggers.add(trigger);
        return trigger;
    }

    public void bindAll(Trigger... triggers) {
        for (Trigger trigger : triggers) {
            bind(trigger);
        }
    }

    public void poll() {
        pollBuffer.clear();
        pollBuffer.addAll(triggers);
        for (Trigger trigger : pollBuffer) {
            for (Runnable binding : trigger.getBindings()) {
                binding.run();
            }
        }
    }

    public void clear() {
        triggers.clear();
    }
}
