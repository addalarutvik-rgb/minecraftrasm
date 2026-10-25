package dev.rasm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public final class Xray {
    private static volatile boolean enabled;
    private Xray() {}
    public static boolean active() { return enabled; }
    public static boolean rendering() {
        if (!enabled) return false;
        var server = Minecraft.getInstance().getSingleplayerServer();
        return server == null || !server.isSameThread();
    }
    public static boolean ore(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return name.endsWith("_ore") || name.equals("ancient_debris");
    }
    public static void toggle(Minecraft client) {
        enabled = !enabled;
        if (client.level != null) client.levelRenderer.allChanged();
    }
    public static void disable(Minecraft client) {
        if (!enabled) return;
        enabled = false;
        if (client.level != null) client.levelRenderer.allChanged();
    }
    public static void reset() { enabled = false; }
}
