package dev.rasm.client.mixin;

import dev.rasm.client.ClearVision;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class ClearVisionLightMixin {
    @Redirect(method="updateLightTexture", at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float rasm$clearDarknessPulse(LocalPlayer player, Holder<MobEffect> effect, float partialTick) {
        return ClearVision.suppresses(effect)?0F:player.getEffectBlendFactor(effect,partialTick);
    }
}
