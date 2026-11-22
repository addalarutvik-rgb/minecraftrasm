package dev.rasm.client.mixin;

import dev.rasm.client.ClearVision;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
public abstract class ClearVisionFogColorMixin {
    @Redirect(method="computeFogColor", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean rasm$normalFogColor(LivingEntity entity, Holder<MobEffect> effect) {
        return !ClearVision.suppresses(effect) && entity.hasEffect(effect);
    }
}
