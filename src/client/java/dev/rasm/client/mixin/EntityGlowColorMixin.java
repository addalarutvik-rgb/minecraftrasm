package dev.rasm.client.mixin;

import dev.rasm.client.EntityGlow;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowColorMixin {
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void rasm$glowColor(CallbackInfoReturnable<Integer> result) {
        Integer color = EntityGlow.color((Entity) (Object) this);
        if (color != null) result.setReturnValue(color);
    }
}
