package dev.rasm.client;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/** Rendering-only suppression; the server's effects and durations remain intact. */
public final class ClearVision {
    private static boolean enabled;
    private ClearVision() {}
    public static boolean active() { return enabled && FeatureSwitches.enabled("clear_vision"); }
    public static boolean suppresses(Holder<MobEffect> effect) {
        return active() && (effect.equals(MobEffects.BLINDNESS) || effect.equals(MobEffects.DARKNESS));
    }
    static void toggle() { enabled=!enabled; }
    static void reset() { enabled=false; }
}
