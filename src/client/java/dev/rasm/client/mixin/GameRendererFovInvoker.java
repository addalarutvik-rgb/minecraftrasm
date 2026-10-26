package dev.rasm.client.mixin;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GameRenderer.class)
public interface GameRendererFovInvoker {
    @Invoker("getFov") float rasm$getFov(Camera camera, float partialTick, boolean changing);
}
