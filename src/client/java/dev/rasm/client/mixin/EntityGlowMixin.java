package dev.rasm.client.mixin;

import dev.rasm.client.EntityGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class EntityGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void rasm$glowSelectedType(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (EntityGlow.matches(entity)) result.setReturnValue(true);
    }
}
