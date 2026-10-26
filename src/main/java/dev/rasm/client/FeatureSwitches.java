package dev.rasm.client;

import java.util.HashSet;
import java.util.Set;

/** Session-scoped master switches, independent of each feature's active state. */
public final class FeatureSwitches {
    private static boolean defaultEnabled = true;
    private static final Set<String> exceptions = new HashSet<>();
    private FeatureSwitches() {}
    public static boolean enabled(String id) { return defaultEnabled != exceptions.contains(id); }
    public static void set(String id, boolean enabled) {
        if (enabled == defaultEnabled) exceptions.remove(id); else exceptions.add(id);
    }
    public static void disableAll() { defaultEnabled = false; exceptions.clear(); }
}
