package dev.rasm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurnPulseTest {
    @Test void smoothUnwrappedOutAndBackWithOneAttackMidpoint() {
        for (float yaw : new float[]{0, 179, -179, 720, -900}) {
            var pulse = new TurnPulse();
            assertTrue(pulse.start(yaw, 0));
            assertEquals(yaw, pulse.sample(0));
            assertEquals(yaw + 90, pulse.sample(75_000_000));
            assertFalse(pulse.atPeak(149_000_000));
            assertEquals(yaw + 180, pulse.sample(150_000_000));
            assertTrue(pulse.atPeak(150_000_000));
            pulse.beginReturn(150_000_000);
            assertFalse(pulse.atPeak(150_000_000));
            assertEquals(yaw + 90, pulse.sample(225_000_000));
            assertEquals(yaw, pulse.sample(300_000_000));
            assertFalse(pulse.active());
        }
    }
    @Test void lateAttackTickHoldsThePeakWithoutSkippingTheHit() {
        var pulse = new TurnPulse(); pulse.start(30, 0);
        assertEquals(210, pulse.sample(500_000_000));
        assertTrue(pulse.active()); assertTrue(pulse.atPeak(500_000_000));
        pulse.beginReturn(500_000_000);
        assertEquals(210, pulse.sample(500_000_000));
        assertEquals(30, pulse.sample(650_000_000));
    }
    @Test void repeatedPressAndCancellationDoNotReuseOldState() {
        var pulse = new TurnPulse(); pulse.start(30, 0);
        assertFalse(pulse.start(210, 50_000_000));
        assertEquals(30, pulse.originalYaw());
        pulse.cancel(); assertFalse(pulse.atPeak(200_000_000));
        assertTrue(pulse.start(70, 300_000_000));
        assertEquals(70, pulse.sample(300_000_000));
        assertEquals(250, pulse.turnedYaw());
    }
}
