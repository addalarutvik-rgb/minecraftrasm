package dev.rasm.client;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Ordered local rules: the latest matching command wins; team colors stay live. */
public final class GlowRules {
    private record Style(Integer color, String team, boolean enabled) {}
    private final Map<String, Style> rules = new LinkedHashMap<>();
    private final Map<String, Integer> teams = new HashMap<>();
    public void clear() { rules.clear(); teams.clear(); }
    public Set<String> teams() { return Set.copyOf(teams.keySet()); }
    public void colorTeam(String team, int color) { teams.put(team, color); }
    public void set(String selector, Integer color, String team) {
        if (team != null) teams.putIfAbsent(team, 0xFFFFFF);
        rules.remove(selector);
        rules.put(selector, new Style(color, team, true));
    }
    public void remove(String selector) {
        rules.remove(selector);
        rules.put(selector, new Style(null, null, false));
    }
    public void removeTeam(String team) {
        var members = rules.entrySet().stream().filter(entry -> team.equals(entry.getValue().team))
            .map(Map.Entry::getKey).toList();
        members.forEach(this::remove);
        teams.remove(team);
    }
    public boolean active(String selector) {
        var style = rules.get(selector);
        return style != null && style.enabled;
    }
    public Integer color(Predicate<String> matches, Predicate<String> permitted) {
        Integer result = null;
        for (var entry : rules.entrySet()) {
            if (!matches.test(entry.getKey())) continue;
            var style = entry.getValue();
            if (!style.enabled) result = null;
            else if (permitted.test(entry.getKey())) result = style.team == null
                ? (style.color == null ? 0xFFFFFF : style.color) : teams.getOrDefault(style.team, 0xFFFFFF);
        }
        return result;
    }
}
