package dev.rasm.client;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
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
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    int count() { return blocks.size(); }
    void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            literal("ghost").then(literal("clear").executes(context -> {
                int removed = count(); clear();
                context.getSource().sendFeedback(Component.literal("Rasm: cleared " + removed + " ghost blocks."));
                return removed;
            }))));
    }
    void clear() {
        if (level != null) blocks.forEach((pos, original) -> {
            if (level.getBlockState(pos).is(Blocks.GLASS)) level.setBlock(pos, original, 3);
        });
        blocks.clear(); level = null; owner = null;
    }
    void tick(Minecraft client) {
        if (level != null && (client.level != level || client.player != owner || !owner.isAlive())) clear();
        else if (level != null) blocks.keySet().removeIf(pos -> !level.getBlockState(pos).is(Blocks.GLASS));
    }
    void place(Minecraft client) {
        tick(client);
        if (client.player == null || client.level == null) return;
        BlockPos at = BlockPos.containing(client.player.getX(), client.player.getBoundingBox().minY - 1, client.player.getZ());
        if (!client.level.getBlockState(at).isAir()) {
            client.player.displayClientMessage(Component.literal("Rasm: move above an empty space to place another ghost block."), true);
            return;
        }
        level = client.level; owner = client.player;
        blocks.put(at, level.getBlockState(at));
        level.setBlock(at, Blocks.GLASS.defaultBlockState(), 3);
    }
}
