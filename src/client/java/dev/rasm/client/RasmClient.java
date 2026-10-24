package dev.rasm.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class RasmClient implements ClientModInitializer {
    private final TurnPulse turn = new TurnPulse();
    private final LocalFlight flight = new LocalFlight();
    private LocalPlayer turningPlayer;
    private KeyMapping menuKey;
    private KeyMapping flipKey;
    private KeyMapping flightKey;

    @Override
    public void onInitializeClient() {
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rasmclient", "controls"));
        menuKey = key("menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        flipKey = key("flip", GLFW.GLFW_KEY_R, category);
        flightKey = key("flight", GLFW.GLFW_KEY_G, category);
        ClientTickEvents.START_CLIENT_TICK.register(flight::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> flight.stop());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private KeyMapping key(String name, int code, KeyMapping.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.rasmclient." + name, InputConstants.Type.KEYSYM, code, category));
    }

    private void tick(Minecraft client) {
        if (turningPlayer != client.player) {
            turn.cancel();
            turningPlayer = client.player;
        }
        if (client.player == null) {
            drainKeys();
            return;
        }
        if (turn.active()) {
            // Hold the outward heading briefly, then return with a negative 180 degree turn.
            client.player.setYRot(turn.tick() ? turn.originalYaw() : turn.turnedYaw());
        }
        if (client.screen != null) {
            drainKeys();
            return;
        }
        while (menuKey.consumeClick()) client.setScreen(new RasmScreen(this));
        while (flipKey.consumeClick()) quickTurn(client);
        while (flightKey.consumeClick()) toggleFlight(client);
    }

    private void drainKeys() {
        while (menuKey.consumeClick()) { }
        while (flipKey.consumeClick()) { }
        while (flightKey.consumeClick()) { }
    }

    void quickTurn(Minecraft client) {
        if (client.player == null) return;
        if (client.player.isPassenger() || client.player.isSleeping()) {
            client.player.displayClientMessage(Component.literal("Rasm: turn while standing freely."), true);
            return;
        }
        if (turn.start(client.player.getYRot())) {
            turningPlayer = client.player;
            client.player.setYRot(turn.turnedYaw());
        }
    }

    void toggleFlight(Minecraft client) { flight.toggle(client); }
    boolean clientFlightActive() { return flight.active(); }
    void requestServerFlight(Minecraft client) { flight.requestServerFlight(client); }
}
