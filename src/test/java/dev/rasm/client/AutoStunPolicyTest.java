package dev.rasm.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AutoStunPolicyTest {
    @Test void groundAutoStunDoesNotNeedStandaloneShieldToggleOrMace() {
        assertTrue(AutoStunPolicy.shield(true,false,false,true,true,true));
        assertFalse(AutoStunPolicy.shield(true,false,false,true,true,false));
    }
    @Test void masterSwitchesBlockTheirOwnMode() {
        assertFalse(AutoStunPolicy.shield(false,true,true,true,true,true));
        assertFalse(AutoStunPolicy.shield(true,false,true,true,false,true));
        assertFalse(AutoStunPolicy.shield(true,true,false,false,true,true));
    }
    @Test void standaloneShieldAutomationStillWorksInAir() {
        assertTrue(AutoStunPolicy.shield(true,true,true,false,false,false));
    }
}
