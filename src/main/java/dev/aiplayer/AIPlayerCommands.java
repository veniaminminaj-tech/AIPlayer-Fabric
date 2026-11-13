package dev.aiplayer;

import com.mojang.brigadier.CommandDispatcher;
import dev.aiplayer.entity.AIPlayerEntities;
import dev.aiplayer.entity.AIPlayerEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class AIPlayerCommands {
    private AIPlayerCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnai")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerLevel level = source.getLevel();
                    Vec3 pos = source.getPosition();
                    AIPlayerEntity bot = AIPlayerEntities.AI_PLAYER.create(level);
                    if (bot == null) {
                        source.sendFailure(Component.literal("Could not create AI player."));
                        return 0;
                    }
                    bot.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);
                    level.addFreshEntity(bot);
                    source.sendSuccess(() -> Component.literal("AI Player spawned."), false);
                    return 1;
                }));
    }
}
