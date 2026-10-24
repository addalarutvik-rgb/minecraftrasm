package dev.rasm.client.mixin;

import dev.rasm.client.Xray;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class XrayFaceMixin {
    @Inject(method="shouldRenderFace", at=@At("HEAD"), cancellable=true)
    private static void rasm$oreFaces(BlockState state, BlockState adjacent, Direction side, CallbackInfoReturnable<Boolean> result) {
        if (Xray.rendering()) result.setReturnValue(Xray.ore(state.getBlock()));
    }
}
