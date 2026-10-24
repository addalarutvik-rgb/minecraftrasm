package dev.rasm.client;

import dev.rasm.flight.FlightMotion;
import dev.rasm.flight.FlightTogglePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Local movement mode; normal movement packets remain subject to server checks. */
final class LocalFlight {
    private LocalPlayer flyingPlayer;
    private ClientLevel flyingLevel;
    private boolean previousNoGravity;

    boolean active() { return flyingPlayer != null; }

    void toggle(Minecraft client) {
        if (active()) {
            stop();
            message(client, "Client flight OFF.");
            return;
        }
        if (client.player == null || !client.player.isAlive()) return;
        if (client.player.isPassenger() || client.player.isSleeping()) {
            message(client, "Leave your vehicle or bed before enabling client flight.");
            return;
        }
        flyingPlayer = client.player;
        flyingLevel = client.level;
        previousNoGravity = flyingPlayer.isNoGravity();
        flyingPlayer.setNoGravity(true);
        flyingPlayer.setDeltaMovement(Vec3.ZERO);
        message(client, "Client flight ON. Jump rises, sneak descends. Servers may reject the movement.");
    }

    void tick(Minecraft client) {
        if (!active()) return;
        if (client.player != flyingPlayer || client.level != flyingLevel
                || !flyingPlayer.isAlive() || flyingPlayer.isPassenger() || flyingPlayer.isSleeping()) {
            stop();
            return;
        }
        flyingPlayer.setNoGravity(true);
        // GUI input must not keep flying in a direction while typing or using a menu.
        if (client.screen != null) {
            flyingPlayer.setDeltaMovement(Vec3.ZERO);
            return;
        }
        var keys = client.options;
        var motion = FlightMotion.calculate(flyingPlayer.getYRot(),
            (keys.keyUp.isDown() ? 1 : 0) - (keys.keyDown.isDown() ? 1 : 0),
            (keys.keyLeft.isDown() ? 1 : 0) - (keys.keyRight.isDown() ? 1 : 0),
            keys.keyJump.isDown(), keys.keyShift.isDown(), keys.keySprint.isDown());
        flyingPlayer.setDeltaMovement(motion.x(), motion.y(), motion.z());
    }

    void stop() {
        if (flyingPlayer == null) return;
        flyingPlayer.setNoGravity(previousNoGravity);
        flyingPlayer.setDeltaMovement(Vec3.ZERO);
        flyingPlayer = null;
        flyingLevel = null;
    }

    void requestServerFlight(Minecraft client) {
        if (client.player == null) return;
        if (!ClientPlayNetworking.canSend(FlightTogglePayload.TYPE)) {
            message(client, "This server does not support permission-based Rasm flight. Use client flight with G.");
            return;
        }
        stop();
        ClientPlayNetworking.send(FlightTogglePayload.INSTANCE);
    }

    private void message(Minecraft client, String text) {
        if (client.player != null) client.player.displayClientMessage(Component.literal("Rasm: " + text), false);
    }
}
