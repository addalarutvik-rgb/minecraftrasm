package dev.rasm.client.mixin;

import dev.rasm.client.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="net.minecraft.client.renderer.block.ModelBlockRenderer$Cache")
public abstract class XrayCacheMixin {
    @Inject(method="getLightColor", at=@At("HEAD"), cancellable=true)
    private void rasm$fullLight(CallbackInfoReturnable<Integer> result) {
        if (Xray.rendering()) result.setReturnValue(0x00F000F0);
    }
    @Inject(method="getShadeBrightness", at=@At("HEAD"), cancellable=true)
    private void rasm$fullShade(CallbackInfoReturnable<Float> result) {
        if (Xray.rendering()) result.setReturnValue(1.0F);
    }
}
