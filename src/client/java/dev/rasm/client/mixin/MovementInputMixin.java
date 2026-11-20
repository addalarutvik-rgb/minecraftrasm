package dev.rasm.client.mixin;

import dev.rasm.client.MovementRecorder;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MovementInputMixin extends ClientInput {
    @Inject(method="tick", at=@At("RETURN"))
    private void rasm$recordAndReplay(CallbackInfo ci) {
        keyPresses=MovementRecorder.INSTANCE.input(keyPresses);
        moveVector=new Vec2((keyPresses.left()?1:0)-(keyPresses.right()?1:0),
            (keyPresses.forward()?1:0)-(keyPresses.backward()?1:0)).normalized();
    }
}
