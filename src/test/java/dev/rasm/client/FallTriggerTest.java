package dev.rasm.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FallTriggerTest {
    @Test void firesOnceUntilLandingEvenWhenAimIsLostAndRegained() {
        FallTrigger trigger = new FallTrigger();
        assertFalse(trigger.update(false,false));
        assertTrue(trigger.update(false,true));
        assertFalse(trigger.update(false,false));
        assertFalse(trigger.update(false,true));
        assertFalse(trigger.update(true,true));
        assertTrue(trigger.update(false,true));
    }
    @Test void worldResetRearms() {
        FallTrigger trigger = new FallTrigger();
        assertTrue(trigger.update(false,true));
        trigger.reset();
        assertTrue(trigger.update(false,true));
    }
}
