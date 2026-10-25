package dev.rasm.flight;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RasmMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("rasmclient");
    private record Grant(ServerPlayer player, boolean mayfly, boolean flying) {}
    private final Map<UUID, Grant> grants = new HashMap<>();
    private final Map<UUID, Long> lastRequests = new HashMap<>();
    private FlightAccess access = FlightAccess.denyMultiplayer();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(FlightTogglePayload.TYPE, FlightTogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FlightDisablePayload.TYPE, FlightDisablePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FlightDisablePayload.TYPE,
            (payload, context) -> restore(context.player()));
        // Fabric invokes play receivers on the server thread. The client cannot choose a target.
        ServerPlayNetworking.registerGlobalReceiver(FlightTogglePayload.TYPE,
            (payload, context) -> toggle(context.server(), context.player()));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            grants.clear();
            lastRequests.clear();
            try {
                access = FlightAccess.load(FabricLoader.getInstance().getConfigDir()
                    .resolve("rasm-flight-allowed-players.txt"));
            } catch (IOException error) {
                access = FlightAccess.denyMultiplayer();
                LOGGER.error("Could not load Rasm flight permissions; multiplayer requests will be denied", error);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            restore(handler.player);
            lastRequests.remove(handler.player.getUUID());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            restore(newPlayer);
            lastRequests.remove(newPlayer.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Grant grant : grants.values()) restoreAbilities(grant.player(), grant);
            grants.clear();
            lastRequests.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(server ->
            grants.values().removeIf(grant -> grant.player().isCreative()
                || grant.player().isSpectator() || !grant.player().getAbilities().mayfly));
    }

    private void toggle(MinecraftServer server, ServerPlayer player) {
        UUID id = player.getUUID();
        long now = System.nanoTime();
        Long previous = lastRequests.get(id);
        if (previous != null && now - previous < 250_000_000L) return;
        lastRequests.put(id, now);
        if (grants.containsKey(id)) {
            restore(player);
            message(player, "Survival flight OFF. Disabling high above ground can cause fall damage.");
            return;
        }
        if (!access.permits(id, server.isSingleplayerOwner(new NameAndId(player.getGameProfile())))) {
            message(player, "Flight denied by the server. Ask its owner to add your UUID (" + id
                + ") to config/rasm-flight-allowed-players.txt and restart the server.");
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            message(player, "Your current game mode already supports flight.");
            return;
        }
        var abilities = player.getAbilities();
        grants.put(id, new Grant(player, abilities.mayfly, abilities.flying));
        abilities.mayfly = true;
        abilities.flying = true;
        player.fallDistance = 0;
        player.onUpdateAbilities();
        message(player, "Survival flight ON. Jump to rise; sneak to descend.");
    }

    private void restore(ServerPlayer player) {
        Grant grant = grants.remove(player.getUUID());
        if (grant != null) restoreAbilities(player, grant);
    }

    private void restoreAbilities(ServerPlayer player, Grant grant) {
        if (player.isCreative() || player.isSpectator()) return;
        player.getAbilities().mayfly = grant.mayfly();
        player.getAbilities().flying = grant.flying();
        player.fallDistance = 0;
        player.onUpdateAbilities();
    }

    private void message(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal("Rasm: " + text));
    }
}
