package dev.rasm.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FeatureSwitchesTest {
    @Test void panicBlocksEveryFeatureUntilExplicitlyReenabled() {
        FeatureSwitches.set("lunge",true);
        FeatureSwitches.set("stun_slam",true);
        FeatureSwitches.disableAll();
        assertFalse(FeatureSwitches.enabled("lunge"));
        assertFalse(FeatureSwitches.enabled("stun_slam"));
        assertFalse(FeatureSwitches.enabled("future_feature"));
        FeatureSwitches.set("stun_slam",true);
        assertTrue(FeatureSwitches.enabled("stun_slam"));
        assertFalse(FeatureSwitches.enabled("lunge"));
        FeatureSwitches.set("stun_slam",false);
        assertFalse(FeatureSwitches.enabled("stun_slam"));
    }
}
