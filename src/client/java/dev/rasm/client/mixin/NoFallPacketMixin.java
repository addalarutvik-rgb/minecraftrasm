package dev.rasm.client.mixin;

import dev.rasm.client.NoFall;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundMovePlayerPacket.class)
public abstract class NoFallPacketMixin {
    @Shadow @Final @Mutable protected boolean onGround;

    @Inject(method = "<init>(DDDFFZZZZ)V", at = @At("RETURN"))
    private void rasm$reportGroundWhileFalling(CallbackInfo callback) {
        if (NoFall.shouldReportGround()) onGround = true;
    }
}
