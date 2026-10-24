package dev.rasm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BackwardTargetRuleTest {
    @Test void picksBehindPlayerRegardlessOfDownwardCameraPitch() {
        assertTrue(BackwardTargetRule.eligible(0, 0, -2, 180, 3));
        assertFalse(BackwardTargetRule.eligible(0, 0, 2, 180, 3));
        assertTrue(BackwardTargetRule.eligible(2, 0, 0, 270, 3));
    }
    @Test void respectsReachIncludingHeightAndRejectsTargetsDirectlyAbove() {
        assertFalse(BackwardTargetRule.eligible(0, 0, -4, 180, 3));
        assertFalse(BackwardTargetRule.eligible(0, 3, -2, 180, 3));
        assertFalse(BackwardTargetRule.eligible(0, 2, 0, 180, 3));
    }
    @Test void acceptsNearRearDiagonalButRejectsSideTargets() {
        assertTrue(BackwardTargetRule.eligible(1, 0, -2, 180, 3));
        assertFalse(BackwardTargetRule.eligible(2, 0, 0, 180, 3));
    }
}
