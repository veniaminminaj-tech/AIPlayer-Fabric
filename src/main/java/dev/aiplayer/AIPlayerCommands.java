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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

public final class AIPlayerCommands {
    private AIPlayerCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnai")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .executes(context -> spawn(context.getSource(), AIPlayerConfig.get().botName))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> spawn(context.getSource(),
                                StringArgumentType.getString(context, "name")))));

        dispatcher.register(Commands.literal("aiplayer")
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .executes(context -> setMode(context.getSource(),
                                        StringArgumentType.getString(context, "mode")))))
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource()))));

        dispatcher.register(Commands.literal("aiclear")
                .executes(context -> {
                    var source = context.getSource();
                    var player = source.getPlayer();
                    if (player == null) {
                        source.sendFailure(Component.literal("This command can only be used by a player."));
                        return 0;
                    }
                    AIPlayerChat.clearMemory(player.getUUID());
                    source.sendSuccess(() -> Component.literal("Your AI companion conversation memory was cleared."), false);
                    return 1;
                }));
    }

    private static int setMode(CommandSourceStack source, String mode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        String normalized = mode.toLowerCase(java.util.Locale.ROOT);
        if (!AIPlayerEntity.MODE_FOLLOW.equals(normalized)
                && !AIPlayerEntity.MODE_WANDER.equals(normalized)
                && !AIPlayerEntity.MODE_STAY.equals(normalized)) {
            source.sendFailure(Component.literal("Use: /aiplayer mode follow, wander, or stay"));
            return 0;
        }

        AIPlayerEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            source.sendFailure(Component.literal("Your AI companion could not be found in this dimension."));
            return 0;
        }
        companion.setBehaviorMode(normalized);
        source.sendSuccess(() -> Component.literal("AI Player mode set to: " + normalized), false);
        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        AIPlayerEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            source.sendFailure(Component.literal("Your AI companion could not be found in this dimension."));
            return 0;
        }
        String mode = companion.getBehaviorMode();
        source.sendSuccess(() -> Component.literal("AI Player: " + companion.getName().getString()
                + " | mode: " + mode + " | health: "
                + Math.round(companion.getHealth()) + "/" + Math.round(companion.getMaxHealth())), false);
        return 1;
    }

    private static AIPlayerEntity findOwnedCompanion(ServerPlayer player) {
        return player.level().getEntitiesOfClass(AIPlayerEntity.class,
                        player.getBoundingBox().inflate(128.0))
                .stream()
                .filter(entity -> entity.isOwnedBy(player.getUUID()))
                .min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
    }

    private static int spawn(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        AIPlayerEntity bot = AIPlayerEntities.AI_PLAYER.create(level, EntitySpawnReason.COMMAND);
        if (bot == null) {
            source.sendFailure(Component.literal("Could not create AI player."));
            return 0;
        }
        String safeName = name == null || name.isBlank() ? "AI Player" : name.substring(0, Math.min(32, name.length()));
        bot.setCustomName(Component.literal(safeName));
        bot.setCustomNameVisible(true);
        bot.setPos(pos.x, pos.y, pos.z);
        bot.setYRot(source.getRotation().y);
        bot.setXRot(0.0F);
        ServerPlayer owner = source.getPlayer();
        if (owner != null) bot.addTag(AIPlayerEntity.OWNER_TAG_PREFIX + owner.getUUID());
        if (!level.addFreshEntity(bot)) {
            source.sendFailure(Component.literal("Could not spawn AI companion at this location."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned AI companion: " + safeName), false);
        return 1;
    }
}
