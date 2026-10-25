package dev.rasm.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

public final class EntityGlow {
    private static final Set<Identifier> types = new HashSet<>();
    private static final Set<String> groups = new HashSet<>();
    private static final Set<UUID> players = new HashSet<>();
    private EntityGlow() {}
    static boolean groupActive(String group) { return groups.contains(group); }
    static void toggleGroup(String group) {
        if (!Set.of("players", "hostile", "passive").contains(group)) return;
        if (!groups.remove(group)) groups.add(group);
    }
    public static void reset() { types.clear(); groups.clear(); players.clear(); }
    public static boolean matches(Entity entity) {
        boolean hostile = entity.getType().getCategory() == MobCategory.MONSTER;
        return entity instanceof Player && players.contains(entity.getUUID())
            || types.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
            || groups.contains("players") && entity instanceof Player
            || groups.contains("hostile") && entity instanceof Mob && hostile
            || groups.contains("passive") && entity instanceof Mob && !hostile;
    }
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            literal("glow").then(argument("filter", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    for (String option : new String[]{"off","clear","@a","players","hostile","passive","only players","only hostile","only passive"})
                        if (option.startsWith(builder.getRemaining())) builder.suggest(option);
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString)
                        .filter(id -> id.startsWith(builder.getRemaining()) || id.startsWith("minecraft:" + builder.getRemaining()))
                        .forEach(builder::suggest);
                    var connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                        for (var info : connection.getOnlinePlayers()) {
                            String playerName = info.getProfile().name();
                            for (String prefix : new String[]{"", "only ", "player ", "only player "}) {
                                String option = prefix + playerName;
                                if (option.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(option);
                            }
                        }
                    }
                    return builder.buildFuture();
                }).executes(context -> {
                    String name = StringArgumentType.getString(context, "filter").trim();
                    if (name.equals("off") || name.equals("clear")) {
                        reset(); context.getSource().sendFeedback(Component.literal("Rasm: glow filters cleared.")); return 1;
                    }
                    boolean only = name.startsWith("only ");
                    if (only) name = name.substring(5).trim();
                    if (name.equals("@a")) name = "players";
                    boolean group = Set.of("players","hostile","passive").contains(name);
                    // Resolve the server's player list, including players outside render distance.
                    boolean explicitPlayer = name.startsWith("player ");
                    String playerName = explicitPlayer ? name.substring(7).trim() : name;
                    var connection = Minecraft.getInstance().getConnection();
                    UUID playerId = null;
                    if (connection != null && (!group || explicitPlayer)) {
                        for (var info : connection.getOnlinePlayers()) {
                            if (info.getProfile().name().equalsIgnoreCase(playerName)) {
                                playerId = info.getProfile().id();
                                playerName = info.getProfile().name();
                                break;
                            }
                        }
                    }
                    if (playerId != null) {
                        if (only) reset();
                        players.add(playerId);
                        context.getSource().sendFeedback(Component.literal("Rasm: " + (only ? "only " : "added ")
                            + playerName + " player glow. /glow clear removes all filters."));
                        return 1;
                    }
                    Identifier id = Identifier.tryParse(name);
                    if (explicitPlayer || !group && (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id))) {
                        context.getSource().sendError(Component.literal("Unknown online player, entity, or group: " + name)); return 0;
                    }
                    if (only) reset();
                    if (group) groups.add(name); else types.add(id);
                    context.getSource().sendFeedback(Component.literal("Rasm: " + (only ? "only " : "added ") + name + " glow. /glow clear removes all filters."));
                    return 1;
                }))));
    }
}
