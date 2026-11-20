package dev.rasm.client.mixin;

import dev.rasm.client.RasmClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SmoothTurnMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void rasm$smoothTurn(CallbackInfo ci) { RasmClient.renderTurn(); }
}
