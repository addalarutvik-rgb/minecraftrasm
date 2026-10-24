package dev.rasm.client;

import dev.rasm.client.mixin.MinecraftAttackInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.InteractionHand;

final class SwapActions {
    private boolean lunge;
    private boolean mace;
    private boolean stun;
    private int returnSlot;
    private int stunChargeTicks;
    private LivingEntity stunTarget;
    private int phase;
    private int delay;
    private LocalPlayer owner;
    private ClientLevel level;
    boolean lungeActive() { return lunge; }
    void stop() { lunge = false; mace = false; stun = false; stunTarget = null; stunChargeTicks = 0; phase = 0; delay = 0; owner = null; level = null; }
    void lunge(Minecraft client) {
        if (lunge || mace || stun) return;
        if (!ready(client)) return;
        if (!client.player.getInventory().getItem(2).is(ItemTags.SPEARS)) {
            message(client, "Put a Lunge-enchanted spear in slot 3 and a low-cooldown item in slot 1."); return;
        }
        stop(); owner = client.player; level = client.level; lunge = true;
    }
    void mace(Minecraft client) {
        if (lunge || mace || stun) return;
        if (!ready(client)) return;
        if (!client.player.getInventory().getItem(1).is(Items.MACE)) {
            message(client, "Put your first weapon in slot 1 and a mace in slot 2."); return;
        }
        stop(); owner = client.player; level = client.level; mace = true;
    }
    void stunSlam(Minecraft client) {
        if (lunge || mace || stun || !ready(client)) return;
        if (!client.player.getInventory().getItem(7).is(ItemTags.AXES)
                || !client.player.getInventory().getItem(1).is(Items.MACE)) {
            message(client, "Stun Slam needs an axe in slot 8 and a mace in slot 2."); return;
        }
        client.gameRenderer.pick(1.0F);
        if (!(client.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) {
            message(client, "Aim at an entity within melee reach to Stun Slam."); return;
        }
        stop(); owner = client.player; level = client.level; stun = true;
        returnSlot = owner.getInventory().getSelectedSlot();
        stunTarget = target;
    }

    private boolean ready(Minecraft client) { return client.player != null && client.gameMode != null && client.player.isAlive(); }
    void tick(Minecraft client) {
        if (!lunge && !mace && !stun) return;
        if (!ready(client) || owner != client.player || level != client.level) { stop(); return; }
        if (client.screen != null) { restore(client); stop(); return; }
        if (stun && (!owner.getInventory().getItem(7).is(ItemTags.AXES)
                || !owner.getInventory().getItem(1).is(Items.MACE)
                || stunTarget == null || !stunTarget.isAlive()
                || stunTarget.level() != level || !owner.hasLineOfSight(stunTarget)
                || owner.distanceToSqr(stunTarget) > Math.pow(owner.entityInteractionRange(), 2))) {
            restore(client); stop(); return;
        }
        if (delay-- > 0) return;
        if (phase == 0) {
            select(client, stun ? 7 : 0); phase = 1;
        } else if (phase == 1) {
            if (stun && owner.getInventory().getSelectedSlot() != 7) { restore(client); stop(); return; }
            if (stun && owner.getAttackStrengthScale(0) < 0.9F) {
                if (++stunChargeTicks > 40) {
                    message(client, "Stun Slam cancelled: axe attack did not charge in time.");
                    restore(client); stop();
                }
                return;
            }
            attack(client); phase = 2;
            if (lunge) delay = 4; // Recover the fast item before changing to the spear.
        } else if (phase == 2) {
            if ((lunge && !owner.getInventory().getItem(2).is(ItemTags.SPEARS))
                    || (mace && !owner.getInventory().getItem(1).is(Items.MACE))) { restore(client); stop(); return; }
            select(client, (mace || stun) ? 1 : 2); attack(client); phase = 3;
        } else {
            restore(client);
            stop();
        }
    }
    private void restore(Minecraft client) { if (client.player == owner) select(client, stun ? returnSlot : 0); }
    private void select(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }
    private void attack(Minecraft client) {
        if (stun) {
            client.gameMode.attack(owner, stunTarget);
            owner.swing(InteractionHand.MAIN_HAND);
        } else click(client);
    }
    private void click(Minecraft client) {
        client.gameRenderer.pick(1.0F);
        MinecraftAttackInvoker attack = (MinecraftAttackInvoker) client;
        attack.rasm$clearMissTime(0);
        attack.rasm$startAttack();
    }
    private void message(Minecraft client, String text) { client.player.displayClientMessage(Component.literal("Rasm: " + text), false); }
}
