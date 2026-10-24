package dev.rasm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NoFallRuleTest {
    @Test void fallingFromLargeHeightsStillSelectsGroundReporting() {
        for (float height : new float[]{3, 20, 256, 10000}) {
            assertTrue(NoFallRule.shouldReportGround(true, true, false, false, height, -1));
        }
    }

    @Test void noChangesWhenDisabledDeadRidingOrFlying() {
        assertFalse(NoFallRule.shouldReportGround(false, true, false, false, 20, -1));
        assertFalse(NoFallRule.shouldReportGround(true, false, false, false, 20, -1));
        assertFalse(NoFallRule.shouldReportGround(true, true, true, false, 20, -1));
        assertFalse(NoFallRule.shouldReportGround(true, true, false, true, 20, -1));
    }

    @Test void jumpingHoveringAndShortFallsAreUnaffected() {
        assertFalse(NoFallRule.shouldReportGround(true, true, false, false, 20, 1));
        assertFalse(NoFallRule.shouldReportGround(true, true, false, false, 20, 0));
        assertFalse(NoFallRule.shouldReportGround(true, true, false, false, 2.5F, -1));
    }
}
