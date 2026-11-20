package dev.rasm.client.mixin;

import dev.rasm.client.RasmClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class ReplayHotbarMixin {
    @Redirect(method="handleKeybinds", at=@At(value="INVOKE", target="Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private boolean rasm$keepReplayOutOfHotbar(KeyMapping key) {
        boolean pressed=key.consumeClick();
        return pressed && !RasmClient.replayUsesHotbar(key);
    }
}
