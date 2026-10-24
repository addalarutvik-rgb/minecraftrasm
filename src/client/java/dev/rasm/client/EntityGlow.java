package dev.rasm.client;

import java.util.HashSet;
import java.util.Set;
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
    private EntityGlow() {}
    public static void reset() { types.clear(); groups.clear(); }
    public static boolean matches(Entity entity) {
        boolean hostile = entity.getType().getCategory() == MobCategory.MONSTER;
        return types.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
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
                    Identifier id = Identifier.tryParse(name);
                    if (!group && (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id))) {
                        context.getSource().sendError(Component.literal("Unknown entity/group: " + name)); return 0;
                    }
                    if (only) reset();
                    if (group) groups.add(name); else types.add(id);
                    context.getSource().sendFeedback(Component.literal("Rasm: " + (only ? "only " : "added ") + name + " glow. /glow clear removes all filters."));
                    return 1;
                }))));
    }
}
