package dev.aiplayer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.aiplayer.entity.AIPlayerEntities;
import dev.aiplayer.entity.AIPlayerEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

public final class AIPlayerCommands {
    private AIPlayerCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aiplayerspawn")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .executes(context -> spawn(context.getSource(), AIPlayerConfig.get().botName))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> spawn(context.getSource(),
                                StringArgumentType.getString(context, "name")))));
    }

    private static int spawn(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        AIPlayerEntity bot = AIPlayerEntities.AI_PLAYER.create(level, EntitySpawnReason.COMMAND);
        if (bot == null) {
            source.sendFailure(Component.literal("Could not create AI player."));
            return 0;
        }
        String safeName = name == null || name.isBlank() ? "Blocky" : name.substring(0, Math.min(32, name.length()));
        bot.setCustomName(Component.literal(safeName));
        bot.setCustomNameVisible(true);
        bot.setPos(pos.x, pos.y, pos.z);
        bot.setYRot(source.getRotation().y);
        bot.setXRot(0.0F);
        if (!level.addFreshEntity(bot)) {
            source.sendFailure(Component.literal("Could not spawn AI companion at this location."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned AI companion: " + safeName), false);
        return 1;
    }
}
