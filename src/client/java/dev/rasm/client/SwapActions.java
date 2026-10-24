package dev.rasm.client;

import dev.rasm.client.mixin.MinecraftAttackInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

final class SwapActions {
    private boolean lunge;
    private boolean mace;
    private int phase;
    private int delay;
    private LocalPlayer owner;
    private ClientLevel level;
    boolean lungeActive() { return lunge; }
    void stop() { lunge = false; mace = false; phase = 0; delay = 0; owner = null; level = null; }
    void toggleLunge(Minecraft client) {
        if (lunge) { restore(client); stop(); return; }
        if (!ready(client)) return;
        if (!client.player.getInventory().getItem(2).is(ItemTags.SPEARS)) {
            message(client, "Put a Lunge-enchanted spear in slot 3 and a low-cooldown item in slot 1."); return;
        }
        stop(); owner = client.player; level = client.level; lunge = true;
    }
    void mace(Minecraft client) {
        if (!ready(client)) return;
        if (!client.player.getInventory().getItem(2).is(Items.MACE)) {
            message(client, "Put your first weapon in slot 1 and a mace in slot 3."); return;
        }
        stop(); owner = client.player; level = client.level; mace = true;
    }
    private boolean ready(Minecraft client) { return client.player != null && client.gameMode != null && client.player.isAlive(); }
    void tick(Minecraft client) {
        if (!lunge && !mace) return;
        if (!ready(client) || owner != client.player || level != client.level) { stop(); return; }
        if (client.screen != null) { restore(client); stop(); return; }
        if (delay-- > 0) return;
        if (phase == 0) {
            select(client, 0); click(client); phase = 1;
            if (lunge) delay = 4; // Recover the fast item before changing to the spear.
        } else if (phase == 1) {
            if ((lunge && !owner.getInventory().getItem(2).is(ItemTags.SPEARS))
                    || (mace && !owner.getInventory().getItem(2).is(Items.MACE))) { restore(client); stop(); return; }
            select(client, 2); click(client); phase = 2;
        } else {
            restore(client);
            if (mace) stop();
            else { phase = 0; delay = 2; }
        }
    }
    private void restore(Minecraft client) { if (client.player == owner) select(client, 0); }
    private void select(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }
    private void click(Minecraft client) {
        client.gameRenderer.pick(1.0F);
        MinecraftAttackInvoker attack = (MinecraftAttackInvoker) client;
        attack.rasm$clearMissTime(0);
        attack.rasm$startAttack();
    }
    private void message(Minecraft client, String text) { client.player.displayClientMessage(Component.literal("Rasm: " + text), false); }
}
