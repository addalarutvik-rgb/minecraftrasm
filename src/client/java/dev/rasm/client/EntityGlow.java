package dev.rasm.client;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Set;
import net.minecraft.ChatFormatting;
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
    private static final GlowRules rules = new GlowRules();
    private EntityGlow() {}
    static boolean groupActive(String group) { return rules.active("group:" + group); }
    static void toggleGroup(String group) {
        if (!Set.of("players", "hostile", "passive").contains(group)) return;
        if (groupActive(group)) rules.remove("group:" + group);
        else rules.set("group:" + group, null, null);
    }
    public static void reset() { rules.clear(); }
    public static boolean matches(Entity entity) { return color(entity) != null; }
    public static Integer color(Entity entity) {
        // Never affect integrated-server entities or server scoreboard teams.
        if (!entity.level().isClientSide()) return null;
        String player = "player:" + entity.getUUID();
        String type = "type:" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean hostile = entity.getType().getCategory() == MobCategory.MONSTER;
        return rules.color(key -> key.equals(type) || entity instanceof Player && key.equals(player)
            || key.equals("group:players") && entity instanceof Player
            || key.equals("group:hostile") && entity instanceof Mob && hostile
            || key.equals("group:passive") && entity instanceof Mob && !hostile,
            key -> FeatureSwitches.enabled(switch (key) {
                case "group:players" -> "Glow Players";
                case "group:hostile" -> "Glow Hostile";
                case "group:passive" -> "Glow Passive";
                default -> "Custom Glow";
            }));
    }
    private static String normalize(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "@a", "player", "players" -> "players";
            case "hostiles", "hostile" -> "hostile";
            case "passives", "passive" -> "passive";
            default -> name.toLowerCase(Locale.ROOT);
        };
    }
    private static Integer parseColor(String name) {
        ChatFormatting color = ChatFormatting.getByName(name);
        if (color != null && color.isColor()) return color.getColor();
        if (name.matches("#[0-9a-fA-F]{6}")) return Integer.parseInt(name.substring(1), 16);
        return null;
    }
    private static String resolve(String name, boolean explicitPlayer) {
        String normalized = normalize(name);
        if (!explicitPlayer && Set.of("players", "hostile", "passive").contains(normalized)) return "group:" + normalized;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) for (var info : connection.getOnlinePlayers()) {
            if (info.getProfile().name().equalsIgnoreCase(name)) return "player:" + info.getProfile().id();
        }
        if (explicitPlayer) return null;
        Identifier id = Identifier.tryParse(normalized);
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? "type:" + id : null;
    }
    private static int command(String input, boolean remove, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
        String[] words = input.trim().split("\\s+");
        int index = 0;
        boolean only = !remove && words[index].equalsIgnoreCase("only");
        if (only) index++;
        boolean explicitPlayer = index < words.length && words[index].equalsIgnoreCase("player") && index + 1 < words.length;
        if (explicitPlayer) index++;
        if (index >= words.length || words.length - index > (remove ? 1 : 2)) {
            source.sendError(Component.literal("Usage: /glow <target> [color|team], /glow <team> <color>, /unglow <target|team>")); return 0;
        }
        String name = words[index++];
        String normalized = name.toLowerCase(Locale.ROOT);
        if (!explicitPlayer && (normalized.equals("clear") || normalized.equals("off")) && index == words.length) { reset(); return 1; }
        String value = index < words.length ? words[index].toLowerCase(Locale.ROOT) : null;
        Integer color = value == null ? null : parseColor(value);
        if (!explicitPlayer && rules.teams().contains(normalized)) {
            if (remove) { rules.removeTeam(normalized); return 1; }
            if (color == null) { source.sendError(Component.literal("Choose a color for team " + name + ", e.g. /glow " + name + " blue")); return 0; }
            if (only) { source.sendError(Component.literal("Use /glow only with a player, entity type, or built-in group.")); return 0; }
            rules.colorTeam(normalized, color); return 1;
        }
        String selector = resolve(name, explicitPlayer);
        if (selector == null) { source.sendError(Component.literal("Unknown online player, entity, or team: " + name)); return 0; }
        String team = value != null && color == null ? value : null;
        if (team != null && (!team.matches("[a-z0-9_-]{1,32}") || resolve(team, false) != null
                || Set.of("off", "clear", "only").contains(team))) {
            source.sendError(Component.literal("Use a unique team name (letters, numbers, _ or -), or a Minecraft color / #RRGGBB.")); return 0;
        }
        if (only) reset();
        if (remove) rules.remove(selector); else rules.set(selector, color, team);
        return 1; // Successful commands stay silent.
    }
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            for (String command : new String[]{"glow", "unglow"}) dispatcher.register(
                literal(command).then(argument("filter", StringArgumentType.greedyString())
                    .suggests((context, builder) -> {
                        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                        var options = new ArrayList<String>();
                        options.addAll(Set.of("off", "clear", "@a", "players", "hostiles", "passive", "only players", "only hostile", "only passive"));
                        options.addAll(rules.teams());
                        BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> {
                            options.add(id.toString()); if (id.getNamespace().equals("minecraft")) options.add(id.getPath());
                        });
                        var connection = Minecraft.getInstance().getConnection();
                        if (connection != null) connection.getOnlinePlayers().forEach(info -> {
                            for (String prefix : new String[]{"", "only ", "player ", "only player "}) options.add(prefix + info.getProfile().name());
                        });
                        for (String option : options) if (option.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(option);
                        if (remaining.contains(" ") && command.equals("glow")) {
                            String prefix = builder.getRemaining().substring(0, remaining.lastIndexOf(' ') + 1);
                            for (ChatFormatting formatting : ChatFormatting.values()) if (formatting.isColor()) {
                                String option = prefix + formatting.getName();
                                if (option.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(option);
                            }
                            for (String team : rules.teams()) if ((prefix + team).toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(prefix + team);
                        }
                        return builder.buildFuture();
                    }).executes(context -> command(StringArgumentType.getString(context, "filter"), command.equals("unglow"), context.getSource()))));
        });
    }
}
