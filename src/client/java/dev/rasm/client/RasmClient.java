package dev.rasm.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rasm.client.mixin.MinecraftAttackInvoker;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
    final SwapActions swaps = new SwapActions();
    final GhostBlock ghost = new GhostBlock();
    private KeyMapping noFallKey, lungeKey, maceKey, ghostKey, serverFlightKey;
    private KeyMapping menuKey;
    private KeyMapping flipKey;
    private KeyMapping flightKey;

    @Override
    public void onInitializeClient() {
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rasmclient", "controls"));
        menuKey = key("menu", GLFW.GLFW_KEY_LEFT_BRACKET, category);
        flipKey = key("flip", GLFW.GLFW_KEY_R, category);
        flightKey = key("flight", GLFW.GLFW_KEY_G, category);
        noFallKey = key("nofall", GLFW.GLFW_KEY_N, category);
        lungeKey = key("lunge", GLFW.GLFW_KEY_Z, category);
        maceKey = key("mace", GLFW.GLFW_KEY_X, category);
        ghostKey = key("ghost", GLFW.GLFW_KEY_B, category);
        serverFlightKey = key("server_flight", GLFW.GLFW_KEY_UNKNOWN, category);
        EntityGlow.register();
        ClientTickEvents.START_CLIENT_TICK.register(flight::tick);
        ClientTickEvents.START_CLIENT_TICK.register(NoFall::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            flight.stop();
            swaps.stop(); ghost.clear(); EntityGlow.reset();
            NoFall.reset();
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private KeyMapping key(String name, int code, KeyMapping.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.rasmclient." + name, InputConstants.Type.KEYSYM, code, category));
    }

    private void tick(Minecraft client) {
        ghost.tick(client);
        swaps.tick(client);
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
        while (noFallKey.consumeClick()) NoFall.toggle(client);
        while (lungeKey.consumeClick()) swaps.toggleLunge(client);
        while (maceKey.consumeClick()) swaps.mace(client);
        while (ghostKey.consumeClick()) ghost.toggle(client);
        while (serverFlightKey.consumeClick()) requestServerFlight(client);
    }

    private void drainKeys() {
        while (menuKey.consumeClick()) { }
        while (flipKey.consumeClick()) { }
        while (flightKey.consumeClick()) { }
        for (KeyMapping key : new KeyMapping[]{noFallKey,lungeKey,maceKey,ghostKey,serverFlightKey})
            while (key.consumeClick()) { }
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
            // Send the new facing before the ordinary left-click interaction.
            client.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                client.player.getYRot(), client.player.getXRot(),
                client.player.onGround(), client.player.horizontalCollision));
            client.gameRenderer.pick(1.0F);
            ((MinecraftAttackInvoker) client).rasm$startAttack();
        }
    }

    void toggleFlight(Minecraft client) { flight.toggle(client); }
    boolean clientFlightActive() { return flight.active(); }
    void requestServerFlight(Minecraft client) { flight.requestServerFlight(client); }
}
