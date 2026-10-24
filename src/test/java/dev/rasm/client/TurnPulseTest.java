package dev.rasm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurnPulseTest {
    @Test void returnsAfterTwoTicksAlongTheSameUnwrappedArc() {
        for (float yaw : new float[]{0, 179, -179, 720, -900}) {
            TurnPulse pulse = new TurnPulse();
            assertTrue(pulse.start(yaw));
            assertEquals(180, pulse.turnedYaw() - yaw);
            assertFalse(pulse.tick());
            assertTrue(pulse.tick());
            assertEquals(-180, pulse.originalYaw() - pulse.turnedYaw());
            assertFalse(pulse.tick());
        }
    }

    @Test void repeatedPressCannotReplaceTheReturnHeading() {
        TurnPulse pulse = new TurnPulse();
        pulse.start(30);
        assertFalse(pulse.start(210));
        assertEquals(30, pulse.originalYaw());
    }

    @Test void cancellationAndNextTurnDoNotReuseOldState() {
        TurnPulse pulse = new TurnPulse();
        pulse.start(30);
        pulse.cancel();
        assertFalse(pulse.tick());
        assertTrue(pulse.start(70));
        assertEquals(250, pulse.turnedYaw());
    }
}
