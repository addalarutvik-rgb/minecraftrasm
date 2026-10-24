package dev.rasm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class GhostBlock {
    private ClientLevel level;
    private LocalPlayer owner;
    private BlockPos position;
    private BlockState original;
    boolean active() { return position != null; }
    void clear() {
        if (position != null && level.getBlockState(position).is(Blocks.GLASS))
            level.setBlock(position, original, 3);
        level = null; owner = null; position = null; original = null;
    }
    void tick(Minecraft client) {
        if (active() && (client.level != level || client.player != owner || !owner.isAlive())) clear();
        else if (active() && !level.getBlockState(position).is(Blocks.GLASS)) {
            level = null; owner = null; position = null; original = null;
        }
    }
    void toggle(Minecraft client) {
        if (active()) { clear(); return; }
        if (client.player == null || client.level == null) return;
        BlockPos at = BlockPos.containing(client.player.getX(), client.player.getBoundingBox().minY - 1, client.player.getZ());
        if (!client.level.getBlockState(at).isAir()) {
            client.player.displayClientMessage(Component.literal("Rasm: jump or move above air to place a ghost block under your feet."), true);
            return;
        }
        level = client.level; owner = client.player; position = at; original = level.getBlockState(at);
        level.setBlock(position, Blocks.GLASS.defaultBlockState(), 3);
    }
}
