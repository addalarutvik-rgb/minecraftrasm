package dev.rasm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlowRulesTest {
    @Test void unglowExcludesOnlyTheMatchingSubsetAndCanBeReenabled() {
        var rules = new GlowRules();
        rules.set("players", 0xFF0000, null);
        rules.remove("rasmore");
        assertNull(rules.color(key -> true, key -> true));
        assertEquals(0xFF0000, rules.color(key -> key.equals("players"), key -> true));
        rules.set("rasmore", 0x0000FF, null);
        assertEquals(0x0000FF, rules.color(key -> true, key -> true));
    }
    @Test void teamColorUpdatesAllMembersWithoutAffectingOtherRules() {
        var rules = new GlowRules();
        rules.set("rasmore", null, "hiphop");
        rules.set("alex", null, "hiphop");
        rules.set("creeper", 0x00FF00, null);
        rules.colorTeam("hiphop", 0x0000FF);
        assertEquals(0x0000FF, rules.color(key -> key.equals("rasmore"), key -> true));
        assertEquals(0x0000FF, rules.color(key -> key.equals("alex"), key -> true));
        assertEquals(0x00FF00, rules.color(key -> key.equals("creeper"), key -> true));
        rules.removeTeam("hiphop");
        assertNull(rules.color(key -> key.equals("rasmore"), key -> true));
        assertNull(rules.color(key -> key.equals("alex"), key -> true));
        assertEquals(0x00FF00, rules.color(key -> key.equals("creeper"), key -> true));
    }
    @Test void latestMatchingCommandWinsAndDisabledMasterDoesNotGlow() {
        var rules = new GlowRules();
        rules.set("type:creeper", 0x00FF00, null);
        rules.remove("hostile");
        assertNull(rules.color(key -> true, key -> true));
        rules.set("type:creeper", 0xFF0000, null);
        assertEquals(0xFF0000, rules.color(key -> true, key -> true));
        assertNull(rules.color(key -> true, key -> false));
    }
    @Test void removingTeamOverridesALaterAllPlayersRule() {
        var rules = new GlowRules();
        rules.set("rasmore", null, "hiphop");
        rules.set("players", 0xFF0000, null);
        rules.removeTeam("hiphop");
        assertNull(rules.color(key -> true, key -> true));
        assertEquals(0xFF0000, rules.color(key -> key.equals("players"), key -> true));
    }
    @Test void movingMemberToNewTeamAndResetDoNotLeaveOldColor() {
        var rules = new GlowRules();
        rules.set("rasmore", null, "team1");
        rules.set("rasmore", null, "hiphop");
        rules.colorTeam("team1", 0xFF0000);
        assertEquals(0xFFFFFF, rules.color(key -> true, key -> true));
        rules.colorTeam("hiphop", 0x0000FF);
        assertEquals(0x0000FF, rules.color(key -> true, key -> true));
        rules.clear();
        assertNull(rules.color(key -> true, key -> true));
        assertTrue(rules.teams().isEmpty());
    }
}
