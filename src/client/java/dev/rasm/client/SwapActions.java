package dev.rasm.client;

import dev.rasm.client.mixin.MinecraftAttackInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
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
    private boolean shield;
    private int returnSlot;
    private LivingEntity stunTarget;
    private int phase;
    private LocalPlayer owner;
    private ClientLevel level;
    boolean lungeActive() { return lunge; }
    boolean active() { return lunge || mace || stun || shield; }
    void stop() { lunge = false; mace = false; stun = false; shield = false; stunTarget = null; phase = 0; owner = null; level = null; }
    void cancel(Minecraft client) {
        if (active() && client.player == owner && client.level == level) restore(client);
        stop();
    }
    void lunge(Minecraft client) {
        if (active()) return;
        if (!ready(client)) return;
        if (!client.player.getInventory().getItem(2).is(ItemTags.SPEARS)) {
            return;
        }
        stop(); owner = client.player; level = client.level; lunge = true;
    }
    void mace(Minecraft client) {
        if (active()) return;
        if (!ready(client)) return;
        if (!client.player.getInventory().getItem(1).is(Items.MACE)) {
            return;
        }
        stop(); owner = client.player; level = client.level; mace = true;
    }
    void stunSlam(Minecraft client) {
        if (active() || !ready(client)) return;
        client.gameRenderer.pick(1.0F);
        LivingEntity target = client.hitResult instanceof EntityHitResult hit
            && hit.getEntity() instanceof LivingEntity living ? living : null;
        stunSlam(client, target);
    }

    // Reuse the flick's locked target instead of raycasting after its camera turn.
    void stunSlam(Minecraft client, LivingEntity target) {
        if (active() || !ready(client) || client.screen != null) return;
        if (!client.player.getInventory().getItem(7).is(ItemTags.AXES)
                || !client.player.getInventory().getItem(1).is(Items.MACE)) {
            return;
        }
        stop(); owner = client.player; level = client.level; stun = true;
        returnSlot = owner.getInventory().getSelectedSlot();
        stunTarget = target;
        tick(client); // Select slot 1 now; allow a tick before the axe hit.
    }

    void shieldStun(Minecraft client) {
        if (active() || !ready(client) || client.screen != null) return;
        if (!client.player.getInventory().getItem(7).is(ItemTags.AXES)) {
            return;
        }
        client.gameRenderer.pick(1.0F);
        LivingEntity target = client.hitResult instanceof EntityHitResult hit
            && hit.getEntity() instanceof LivingEntity living ? living : null;
        stop(); owner = client.player; level = client.level; shield = true;
        stunTarget = target;
        tick(client);
    }

    private boolean ready(Minecraft client) { return client.player != null && client.gameMode != null && client.player.isAlive(); }
    void tick(Minecraft client) {
        if (!active()) return;
        if (!ready(client) || owner != client.player || level != client.level) { stop(); return; }
        if (client.screen != null) { restore(client); stop(); return; }
        if ((stun || shield) && (!owner.getInventory().getItem(7).is(ItemTags.AXES)
                || stun && !owner.getInventory().getItem(1).is(Items.MACE))) {
            restore(client); stop(); return;
        }
        if (phase == 0) {
            select(client, 0);
            phase = lunge ? 2 : 1; // Lunge skips the slot-1 attack entirely.
        } else if (phase == 1) {
            if (stun || shield) {
                select(client, 7);
                attack(client);
                phase = shield ? 3 : 2;
                // Advance to the mace on the next tick (~50 ms after the axe hit).
            } else {
                attack(client); phase = 2;
            }
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
        if (stun || shield) {
            entityAttackOrSwing(client, stunTarget);
        } else click(client);
    }
    private void entityAttackOrSwing(Minecraft client, LivingEntity target) {
        if (target != null && target.isAlive() && target.level() == client.level
                && client.player.hasLineOfSight(target)
                && client.player.distanceToSqr(target) <= Math.pow(client.player.entityInteractionRange(), 2)) {
            client.gameMode.attack(client.player, target);
        }
        client.player.swing(InteractionHand.MAIN_HAND);
    }
    private void click(Minecraft client) {
        client.gameRenderer.pick(1.0F);
        MinecraftAttackInvoker attack = (MinecraftAttackInvoker) client;
        attack.rasm$clearMissTime(0);
        attack.rasm$startAttack();
    }
}
