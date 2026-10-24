package dev.rasm.client.mixin;

import dev.rasm.client.Xray;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XrayStateMixin {
    @Inject(method="getRenderShape", at=@At("RETURN"), cancellable=true)
    private void rasm$hideTerrain(CallbackInfoReturnable<RenderShape> result) {
        if (Xray.rendering() && !Xray.ore(((BlockBehaviour.BlockStateBase)(Object)this).getBlock()))
            result.setReturnValue(RenderShape.INVISIBLE);
    }
    @Inject(method="getLightEmission", at=@At("RETURN"), cancellable=true)
    private void rasm$emissiveOres(CallbackInfoReturnable<Integer> result) {
        if (Xray.rendering() && Xray.ore(((BlockBehaviour.BlockStateBase)(Object)this).getBlock())) result.setReturnValue(15);
    }
    @Inject(method="getShadeBrightness", at=@At("RETURN"), cancellable=true)
    private void rasm$unshaded(CallbackInfoReturnable<Float> result) {
        if (Xray.rendering()) result.setReturnValue(1.0F);
    }
    @Inject(method="isSolidRender", at=@At("RETURN"), cancellable=true)
    private void rasm$disableOcclusion(CallbackInfoReturnable<Boolean> result) {
        if (Xray.rendering()) result.setReturnValue(false);
    }
}
