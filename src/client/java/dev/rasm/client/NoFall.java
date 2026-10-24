package dev.rasm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class NoFall {
    private static LocalPlayer enabledPlayer;
    private static ClientLevel enabledLevel;

    private NoFall() {}

    public static boolean active() { return enabledPlayer != null; }

    public static void reset() {
        enabledPlayer = null;
        enabledLevel = null;
    }

    public static void toggle(Minecraft client) {
        if (active()) {
            reset();
        } else if (client.player != null && client.player.isAlive()) {
            enabledPlayer = client.player;
            enabledLevel = client.level;
        }
        if (client.player != null) client.player.displayClientMessage(
            Component.literal("Rasm: NoFall " + (active() ? "ON" : "OFF") + "."), true);
    }

    public static void tick(Minecraft client) {
        if (enabledPlayer != null && (client.player != enabledPlayer || client.level != enabledLevel
                || !enabledPlayer.isAlive())) reset();
    }

    public static boolean shouldReportGround() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player != enabledPlayer || client.level != enabledLevel
                || player.isSpectator() || player.isInWater()) return false;
        return NoFallRule.shouldReportGround(true, player.isAlive(), player.isPassenger(),
            player.getAbilities().flying, player.fallDistance, player.getDeltaMovement().y);
    }
}
