package dev.rasm.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAttackInvoker {
    @Accessor("missTime")
    void rasm$clearMissTime(int ticks);

    @Invoker("startAttack")
    boolean rasm$startAttack();
}
