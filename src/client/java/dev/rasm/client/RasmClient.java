package dev.rasm.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.multiplayer.ClientLevel;
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
    private ClientLevel turnLevel;
    private Entity pendingTarget;
    private int attackDelay = -1;
    final SwapActions swaps = new SwapActions();
    final GhostBlock ghost = new GhostBlock();
    private KeyMapping noFallKey, lungeKey, maceKey, ghostKey, serverFlightKey, stunKey;
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
        stunKey = key("stun_slam", GLFW.GLFW_KEY_V, category);
        ghostKey = key("ghost", GLFW.GLFW_KEY_B, category);
        serverFlightKey = key("server_flight", GLFW.GLFW_KEY_UNKNOWN, category);
        EntityGlow.register();
        ghost.register();
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
        if (turningPlayer != client.player || turnLevel != client.level) {
            turn.cancel();
            pendingTarget = null; attackDelay = -1;
            turnLevel = client.level;
            turningPlayer = client.player;
        }
        if (client.player == null) {
            drainKeys();
            return;
        }
        if (!client.player.isAlive() || client.screen != null) {
            pendingTarget = null; attackDelay = -1;
        }
        if (attackDelay > 0 && --attackDelay == 0) {
            if (pendingTarget != null && pendingTarget.isAlive() && client.gameMode != null
                    && pendingTarget.level() == client.level && client.player.hasLineOfSight(pendingTarget)
                    && client.player.distanceToSqr(pendingTarget) <= Math.pow(client.player.entityInteractionRange(), 2)) {
                client.gameMode.attack(client.player, pendingTarget);
                client.player.swing(InteractionHand.MAIN_HAND);
            }
            pendingTarget = null; attackDelay = -1;
        }
        if (turn.active() && turn.tick()) {
            client.player.setYRot(turn.originalYaw());
            sendFacing(client);
            if (pendingTarget != null) attackDelay = 20;
        }
        if (client.screen != null) {
            drainKeys();
            return;
        }
        while (menuKey.consumeClick()) client.setScreen(new RasmScreen(this));
        while (flipKey.consumeClick()) quickTurn(client);
        while (flightKey.consumeClick()) toggleFlight(client);
        while (noFallKey.consumeClick()) NoFall.toggle(client);
        while (lungeKey.consumeClick()) swaps.lunge(client);
        while (maceKey.consumeClick()) swaps.mace(client);
        while (stunKey.consumeClick()) swaps.stunSlam(client);
        while (ghostKey.consumeClick()) ghost.place(client);
        while (serverFlightKey.consumeClick()) requestServerFlight(client);
    }

    private void drainKeys() {
        while (menuKey.consumeClick()) { }
        while (flipKey.consumeClick()) { }
        while (flightKey.consumeClick()) { }
        for (KeyMapping key : new KeyMapping[]{noFallKey,lungeKey,maceKey,ghostKey,serverFlightKey,stunKey})
            while (key.consumeClick()) { }
    }

    void quickTurn(Minecraft client) {
        if (client.player == null) return;
        if (client.player.isPassenger() || client.player.isSleeping()) {
            client.player.displayClientMessage(Component.literal("Rasm: turn while standing freely."), true);
            return;
        }
        if (attackDelay > 0 || turn.active()) return;
        if (turn.start(client.player.getYRot())) {
            turningPlayer = client.player; turnLevel = client.level;
            client.player.setYRot(turn.turnedYaw());
            sendFacing(client);
            client.gameRenderer.pick(1.0F);
            pendingTarget = client.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
            // Prefer an in-reach player behind us, even if the original pitch pointed at the ground.
            double range = client.player.entityInteractionRange();
            Player best = null;
            double distance = range * range;
            for (Player other : client.level.players()) {
                if (other == client.player || !other.isAlive() || other.isSpectator()) continue;
                Vec3 offset = other.position().subtract(client.player.position());
                double squared = client.player.distanceToSqr(other);
                if (squared <= distance && BackwardTargetRule.eligible(offset.x, offset.y, offset.z, turn.turnedYaw(), range) && client.player.hasLineOfSight(other)) {
                    best = other; distance = squared;
                }
            }
            if (best != null) pendingTarget = best;
        }
    }

    private void sendFacing(Minecraft client) {
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(
            client.player.getYRot(), client.player.getXRot(), client.player.onGround(), client.player.horizontalCollision));
    }

    void toggleFlight(Minecraft client) { flight.toggle(client); }
    boolean clientFlightActive() { return flight.active(); }
    void requestServerFlight(Minecraft client) { flight.requestServerFlight(client); }
}
