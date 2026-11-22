package dev.rasm.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
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
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class RasmClient implements ClientModInitializer {
    private static RasmClient instance;
    private Entity flickTarget;
    private boolean autoShield;
    private int shieldRetry;
    private KeyMapping clearVisionKey;
    private KeyMapping checkpointKey;
    private KeyMapping replayKey;
    private KeyMapping autoShieldKey;
    private boolean autoStun;
    private final FallTrigger autoFall = new FallTrigger();
    private LocalPlayer autoOwner;
    private ClientLevel autoLevel;
    private final TurnPulse turn = new TurnPulse();
    private final LocalFlight flight = new LocalFlight();
    private LocalPlayer turningPlayer;
    private ClientLevel turnLevel;
    final SwapActions swaps = new SwapActions();
    final GhostBlock ghost = new GhostBlock();
    private KeyMapping noFallKey, lungeKey, maceKey, ghostKey, serverFlightKey, stunKey, xrayKey, shieldStunKey;
    private KeyMapping autoStunKey, blockEspKey;
    private final java.util.List<KeyMapping> bindings = new java.util.ArrayList<>();
    java.util.List<KeyMapping> bindings() { return java.util.List.copyOf(bindings); }
    private KeyMapping pearlCatchKey;
    private KeyMapping panicKey;
    private KeyMapping menuKey;
    private KeyMapping flipKey;
    private KeyMapping flightKey;

    @Override
    public void onInitializeClient() {
        instance = this;
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rasmclient", "controls"));
        panicKey = key("panic", GLFW.GLFW_KEY_END, category);
        menuKey = key("menu", GLFW.GLFW_KEY_LEFT_BRACKET, category);
        flipKey = key("flip", GLFW.GLFW_KEY_R, category);
        flightKey = key("flight", GLFW.GLFW_KEY_G, category);
        noFallKey = key("nofall", GLFW.GLFW_KEY_N, category);
        lungeKey = key("lunge", GLFW.GLFW_KEY_Z, category);
        maceKey = key("mace", GLFW.GLFW_KEY_X, category);
        stunKey = key("stun_slam", GLFW.GLFW_KEY_V, category);
        shieldStunKey = key("shield_stun", GLFW.GLFW_KEY_J, category);
        pearlCatchKey = key("pearl_catch", GLFW.GLFW_KEY_P, category);
        xrayKey = key("xray", GLFW.GLFW_KEY_H, category);
        ghostKey = key("ghost", GLFW.GLFW_KEY_B, category);
        serverFlightKey = key("server_flight", GLFW.GLFW_KEY_UNKNOWN, category);
        clearVisionKey = key("clear_vision", GLFW.GLFW_KEY_UNKNOWN, category);
        checkpointKey = key("movement_checkpoint", GLFW.GLFW_KEY_UNKNOWN, category);
        replayKey = key("movement_replay", GLFW.GLFW_KEY_5, category);
        autoShieldKey = key("auto_shield", GLFW.GLFW_KEY_UNKNOWN, category);
        autoStunKey = key("auto_stun", GLFW.GLFW_KEY_UNKNOWN, category);
        blockEspKey = key("block_esp", GLFW.GLFW_KEY_UNKNOWN, category);
        BlockEsp.register();
        EntityGlow.register();
        ghost.register();
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            if (screen instanceof RasmKeybindScreen || screen instanceof net.minecraft.client.gui.screens.options.controls.KeyBindsScreen) return;
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.allowKeyPress(screen).register((shown, event) -> {
                if (client.player != null && panicKey.matches(event)) { panic(client); return false; }
                return true;
            });
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (panicKey.consumeClick()) panic(client);
        });
        ClientTickEvents.START_CLIENT_TICK.register(MovementRecorder.INSTANCE::tick);
        ClientTickEvents.START_CLIENT_TICK.register(flight::tick);
        ClientTickEvents.START_CLIENT_TICK.register(NoFall::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MovementRecorder.INSTANCE.clear();
            flight.stop();
            swaps.stop(); ghost.clear(); EntityGlow.reset();
            ClearVision.reset(); NoFall.reset(); Xray.reset(); BlockEsp.reset(); autoStun = false; autoShield = false; shieldRetry = 0; autoFall.reset();
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private KeyMapping key(String name, int code, KeyMapping.Category category) {
        KeyMapping binding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.rasmclient." + name, InputConstants.Type.KEYSYM, code, category));
        bindings.add(binding);
        return binding;
    }

    void panic(Minecraft client) {
        MovementRecorder.INSTANCE.stop();
        FeatureSwitches.disableAll();
        autoStun = false; autoShield = false; shieldRetry = 0; autoFall.reset(); BlockEsp.reset();
        swaps.cancel(client);
        if (turn.active() && client.player != null && client.player == turningPlayer && client.level == turnLevel) {
            client.player.setYRot(turn.originalYaw());
            sendFacing(client);
        }
        turn.cancel();
        flight.panic(client);
        ClearVision.reset();
        NoFall.reset();
        EntityGlow.reset();
        ghost.clear();
        Xray.disable(client);
        drainKeys();
        if (client.screen instanceof RasmScreen || client.screen instanceof BlockEspScreen || client.screen instanceof MovementScreen) client.setScreen(null);
    }

    private void tick(Minecraft client) {
        if (panicKey.consumeClick()) { panic(client); return; }
        BlockEsp.tick(client);
        ghost.tick(client);
        swaps.tick(client);
        if (turningPlayer != client.player || turnLevel != client.level) {
            turn.cancel();
            turnLevel = client.level;
            turningPlayer = client.player;
        }
        if (client.player == null) {
            drainKeys();
            return;
        }
        updateTurn(client);
        if (turn.atPeak(System.nanoTime())) {
            hitAtPeak(client);
            turn.beginReturn(System.nanoTime());
        }
        if (client.screen != null) {
            drainKeys();
            return;
        }
        while (menuKey.consumeClick()) client.setScreen(new RasmScreen(this));
        if (client.screen != null) { drainKeys(); return; }
        while (clearVisionKey.consumeClick()) if (FeatureSwitches.enabled("clear_vision")) ClearVision.toggle();
        while (replayKey.consumeClick()) if (FeatureSwitches.enabled("movement_replay")) MovementRecorder.INSTANCE.press(client);
        while (checkpointKey.consumeClick()) if (FeatureSwitches.enabled("movement_replay")) MovementRecorder.INSTANCE.checkpoint(client);
        if (MovementRecorder.INSTANCE.active()) { drainKeys(); return; }
        while (autoShieldKey.consumeClick()) if (FeatureSwitches.enabled("auto_shield")) toggleAutoShield();
        while (autoStunKey.consumeClick()) if (FeatureSwitches.enabled("auto_stun")) toggleAutoStun();
        while (blockEspKey.consumeClick()) if (FeatureSwitches.enabled("block_esp")) client.setScreen(new BlockEspScreen(new RasmScreen(this)));
        if (client.screen != null) { drainKeys(); return; }
        tickAutoStun(client);
        while (flipKey.consumeClick()) if (FeatureSwitches.enabled("flip")) quickTurn(client);
        while (flightKey.consumeClick()) if (FeatureSwitches.enabled("flight")) toggleFlight(client);
        while (noFallKey.consumeClick()) if (FeatureSwitches.enabled("nofall")) NoFall.toggle(client);
        while (lungeKey.consumeClick()) if (!turn.active() && FeatureSwitches.enabled("lunge")) swaps.lunge(client);
        while (maceKey.consumeClick()) if (!turn.active() && FeatureSwitches.enabled("mace")) swaps.mace(client);
        while (xrayKey.consumeClick()) if (FeatureSwitches.enabled("xray")) Xray.toggle(client);
        while (stunKey.consumeClick()) if (!turn.active() && FeatureSwitches.enabled("stun_slam")) swaps.stunSlam(client);
        while (shieldStunKey.consumeClick()) if (!turn.active() && FeatureSwitches.enabled("shield_stun")) swaps.shieldStun(client);
        while (pearlCatchKey.consumeClick()) if (!turn.active() && FeatureSwitches.enabled("pearl_catch")) swaps.pearlCatch(client);
        while (ghostKey.consumeClick()) if (FeatureSwitches.enabled("ghost")) ghost.place(client);
        while (serverFlightKey.consumeClick()) if (FeatureSwitches.enabled("server_flight")) requestServerFlight(client);
        tickAutoShield(client);
    }

    private void drainKeys() {
        while (panicKey.consumeClick()) { }
        while (menuKey.consumeClick()) { }
        while (flipKey.consumeClick()) { }
        while (flightKey.consumeClick()) { }
        for (KeyMapping key : new KeyMapping[]{noFallKey,lungeKey,maceKey,ghostKey,serverFlightKey,stunKey,xrayKey,shieldStunKey,pearlCatchKey,autoStunKey,blockEspKey,autoShieldKey,replayKey,checkpointKey,clearVisionKey})
            while (key.consumeClick()) { }
    }

    void quickTurn(Minecraft client) {
        if (client.player == null || client.gameMode == null || !client.player.isAlive()) return;
        if (client.player.isPassenger() || client.player.isSleeping()) {
            return;
        }
        if (MovementRecorder.INSTANCE.active() || turn.active() || swaps.active() || client.player.isUsingItem()) return;
        // Capture the intended opponent before rotating away. Picking only after
        // the turn loses a front-facing target and produces ordinary rear hits.
        client.gameRenderer.pick(1.0F);
        Entity aimedTarget = client.hitResult instanceof EntityHitResult aimed
            && aimed.getEntity() instanceof LivingEntity ? aimed.getEntity() : null;
        flickTarget = aimedTarget;
        if (turn.start(client.player.getYRot(), System.nanoTime())) {
            turningPlayer = client.player; turnLevel = client.level;
        }
    }

    public static void renderTurn() {
        if (instance != null) instance.updateTurn(Minecraft.getInstance());
    }
    private void updateTurn(Minecraft client) {
        if (!turn.active()) return;
        if (client.player == null || client.player != turningPlayer || client.level != turnLevel
                || !client.player.isAlive() || client.player.isUsingItem() || client.screen != null) {
            turn.cancel(); flickTarget = null; return;
        }
        client.player.setYRot(turn.sample(System.nanoTime()));
    }
    private void hitAtPeak(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;
        Entity aimedTarget = flickTarget;
        {
            client.gameRenderer.pick(1.0F);
            Entity target = client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity
                ? hit.getEntity() : null;
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
            if (best != null) target = best;
            if (aimedTarget != null) target = aimedTarget;
            // Synchronize sprint and the flick yaw before the attack packet, all in this tick.
            client.player.connection.send(new ServerboundPlayerCommandPacket(client.player,
                client.player.isSprinting() ? ServerboundPlayerCommandPacket.Action.START_SPRINTING
                    : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            sendFacing(client);
            if (target != null && target.level() == client.level && target.isAlive() && client.player.hasLineOfSight(target)
                    && client.player.distanceToSqr(target) <= range * range) {
                client.gameMode.attack(client.player, target);
            }
            client.player.swing(InteractionHand.MAIN_HAND);
            if (FeatureSwitches.enabled("stun_slam")) swaps.stunSlam(client, target instanceof LivingEntity living ? living : null);
        }
    }

    private void sendFacing(Minecraft client) {
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(
            client.player.getYRot(), client.player.getXRot(), client.player.onGround(), client.player.horizontalCollision));
    }

    boolean featureEnabled(String id) { return FeatureSwitches.enabled(id); }
    void toggleFeature(String id) {
        boolean enabled = !FeatureSwitches.enabled(id);
        FeatureSwitches.set(id, enabled);
        if (enabled) return;
        Minecraft client = Minecraft.getInstance();
        switch (id) {
            case "clear_vision" -> ClearVision.reset();
            case "movement_replay" -> MovementRecorder.INSTANCE.stop();
            case "flight" -> flight.stop();
            case "server_flight" -> flight.panic(client);
            case "nofall" -> NoFall.reset();
            case "xray" -> Xray.disable(client);
            case "ghost" -> ghost.clear();
            case "block_esp" -> BlockEsp.disable();
            case "auto_stun", "stun_slam" -> { autoStun = false; swaps.cancel(client); }
            case "auto_shield" -> { autoShield = false; swaps.cancelAutomaticShield(client); }
            case "shield_stun" -> { autoShield = false; swaps.cancel(client); }
            case "lunge", "mace", "pearl_catch" -> swaps.cancel(client);
            case "flip" -> {
                swaps.cancel(client);
                if (turn.active() && client.player == turningPlayer && client.player != null && client.level == turnLevel) {
                    client.player.setYRot(turn.originalYaw()); sendFacing(client);
                }
                turn.cancel();
            }
            default -> { }
        }
        drainKeys();
    }
    static boolean movementBusy() {
        var c=Minecraft.getInstance();
        return instance == null || instance.turn.active() || instance.swaps.active() || instance.flight.active()
            || c.player == null || c.player.isUsingItem() || c.player.getAbilities().flying || c.player.isFallFlying();
    }
    public static boolean replayUsesHotbar(KeyMapping key) {
        if (instance == null || !FeatureSwitches.enabled("movement_replay")) return false;
        for (var hotbar : Minecraft.getInstance().options.keyHotbarSlots)
            if (hotbar == key && key.same(instance.replayKey)) return true;
        return false;
    }

    boolean autoShieldActive() { return autoShield; }
    void toggleAutoShield() { autoShield = !autoShield; if (!autoShield) swaps.cancelAutomaticShield(Minecraft.getInstance()); }
    private void tickAutoShield(Minecraft client) {
        if (shieldRetry > 0) shieldRetry--;
        if (!autoShield || !FeatureSwitches.enabled("auto_shield") || !FeatureSwitches.enabled("shield_stun")
                || shieldRetry > 0 || swaps.active() || turn.active() || SwapActions.busy(client)) return;
        client.gameRenderer.pick(1.0F);
        if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player target
                && target.isBlocking() && target.getUseItem().is(net.minecraft.world.item.Items.SHIELD)
                && swaps.automaticShield(client, target)) shieldRetry = 20;
    }

    boolean autoStunActive() { return autoStun; }
    void toggleAutoStun() { autoStun = !autoStun; }
    private void tickAutoStun(Minecraft client) {
        var player = client.player;
        if (player != autoOwner || client.level != autoLevel) {
            autoFall.reset(); autoOwner = player; autoLevel = client.level;
        }
        if (player.onGround()) autoFall.reset();
        if (!autoStun || !FeatureSwitches.enabled("auto_stun") || !FeatureSwitches.enabled("stun_slam")
                || swaps.active() || turn.active() || !player.isAlive() || player.isSpectator()
                || player.isPassenger() || player.isInWater() || player.isFallFlying()
                || player.getAbilities().flying || flight.active() || player.getDeltaMovement().y >= -0.08
                || player.fallDistance < 0.5 || client.gameMode == null) return;
        if (!player.getInventory().getItem(7).is(net.minecraft.tags.ItemTags.AXES)
                || !player.getInventory().getItem(1).is(net.minecraft.world.item.Items.MACE)) return;
        client.gameRenderer.pick(1.0F);
        if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player target
                && target != player && target.isAlive() && !target.isSpectator() && player.hasLineOfSight(target)
                && player.distanceToSqr(target) <= Math.pow(player.entityInteractionRange(), 2)
                && autoFall.update(player.onGround(), true)) swaps.stunSlam(client, target);
    }

    String bindingLabel(String name) {
        KeyMapping binding = switch (name) {
            case "clear_vision" -> clearVisionKey;
            case "movement_checkpoint" -> checkpointKey;
            case "movement_replay" -> replayKey;
            case "auto_shield" -> autoShieldKey;
            case "auto_stun" -> autoStunKey;
            case "block_esp" -> blockEspKey;
            case "panic" -> panicKey;
            case "flip" -> flipKey;
            case "stun_slam" -> stunKey;
            case "shield_stun" -> shieldStunKey;
            case "lunge" -> lungeKey;
            case "mace" -> maceKey;
            case "pearl_catch" -> pearlCatchKey;
            case "flight" -> flightKey;
            case "nofall" -> noFallKey;
            case "server_flight" -> serverFlightKey;
            case "xray" -> xrayKey;
            case "ghost" -> ghostKey;
            default -> menuKey;
        };
        return binding.isUnbound() ? "Unbound" : binding.getTranslatedKeyMessage().getString();
    }

    void toggleFlight(Minecraft client) { flight.toggle(client); }
    boolean clientFlightActive() { return flight.active(); }
    void requestServerFlight(Minecraft client) { flight.requestServerFlight(client); }
}
