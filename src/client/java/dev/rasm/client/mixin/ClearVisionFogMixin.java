package dev.rasm.client.mixin;

import dev.rasm.client.ClearVision;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectFogEnvironment.class)
public abstract class ClearVisionFogMixin {
    @Shadow public abstract Holder<MobEffect> getMobEffect();
    @Inject(method="isApplicable", at=@At("HEAD"), cancellable=true)
    private void rasm$clearEffectFog(FogType type, Entity entity, CallbackInfoReturnable<Boolean> result) {
        if(ClearVision.suppresses(getMobEffect())) result.setReturnValue(false);
    }
}
