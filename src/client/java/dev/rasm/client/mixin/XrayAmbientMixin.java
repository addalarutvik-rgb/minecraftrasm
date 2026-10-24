package dev.rasm.client.mixin;

import dev.rasm.client.Xray;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class XrayAmbientMixin {
    @Inject(method="useAmbientOcclusion", at=@At("RETURN"), cancellable=true)
    private static void rasm$flatLight(CallbackInfoReturnable<Boolean> result) {
        if (Xray.rendering()) result.setReturnValue(false);
    }
}
