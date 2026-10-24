package dev.rasm.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public final class EntityGlow {
    private static Identifier target;
    private EntityGlow() {}
    public static void reset() { target = null; }
    public static boolean matches(Entity entity) {
        return target != null && target.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            literal("glow").then(argument("entity", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    builder.suggest("off");
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString)
                        .filter(id -> id.startsWith(builder.getRemaining()) || id.startsWith("minecraft:" + builder.getRemaining()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                }).executes(context -> {
                    String name = StringArgumentType.getString(context, "entity");
                    if (name.equals("off")) {
                        reset();
                        context.getSource().sendFeedback(Component.literal("Rasm: entity glow OFF."));
                        return 1;
                    }
                    Identifier id = Identifier.tryParse(name);
                    if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                        context.getSource().sendError(Component.literal("Unknown entity type: " + name));
                        return 0;
                    }
                    target = id;
                    context.getSource().sendFeedback(Component.literal("Rasm: highlighting " + id + ". Use /glow off to clear."));
                    return 1;
                }))));
    }
}
