package dev.rasm.client.mixin;

import dev.rasm.client.Xray;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class XrayLightMixin {
    @Inject(method="getLightColor", at=@At("RETURN"), cancellable=true)
    private static void rasm$brightOres(CallbackInfoReturnable<Integer> result) {
        if (Xray.rendering()) result.setReturnValue(0x00F000F0);
    }
}
