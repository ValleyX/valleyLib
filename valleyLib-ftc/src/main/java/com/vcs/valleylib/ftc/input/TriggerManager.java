package com.vcs.valleylib.ftc.input;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for command trigger bindings.
 */
public class TriggerManager {

    private final List<Trigger> triggers = new ArrayList<>();

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
        for (Trigger trigger : triggers) {
            for (Runnable binding : trigger.getBindings()) {
                binding.run();
            }
        }
    }

    public void clear() {
        triggers.clear();
    }
}
