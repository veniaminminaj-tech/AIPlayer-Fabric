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

import java.util.Comparator;
import java.util.StringJoiner;

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
        dispatcher.register(Commands.literal("aiinventory")
                .executes(context -> showInventory(context.getSource())));
        dispatcher.register(Commands.literal("aigather")
                .executes(context -> toggleGatherMode(context.getSource())));
    }

    private static int showInventory(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        AIPlayerEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            source.sendFailure(Component.literal("Your companion was not found within 256 blocks."));
            return 0;
        }

        StringJoiner items = new StringJoiner(", ");
        int occupiedSlots = 0;
        for (int slot = 0; slot < companion.getInventory().getContainerSize(); slot++) {
            var stack = companion.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            occupiedSlots++;
            items.add(stack.getCount() + "x " + stack.getHoverName().getString());
        }

        final String inventoryText = occupiedSlots == 0
                ? "Your companion's inventory is empty."
                : "Companion inventory (" + occupiedSlots + "/27 slots): " + items;
        source.sendSuccess(() -> Component.literal(inventoryText), false);
        return 1;
    }

    private static int toggleGatherMode(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        AIPlayerEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            source.sendFailure(Component.literal("Your companion was not found within 256 blocks."));
            return 0;
        }

        boolean enabled = companion.toggleGatherMode();
        String message = enabled
                ? "Log gathering enabled. Your companion will harvest nearby tree logs."
                : "Log gathering disabled.";
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static AIPlayerEntity findOwnedCompanion(ServerPlayer player) {
        String ownerTag = AIPlayerEntity.ownerTag(player.getUUID());
        return player.level().getEntitiesOfClass(
                        AIPlayerEntity.class,
                        player.getBoundingBox().inflate(256.0),
                        entity -> entity.isAlive() && entity.getTags().contains(ownerTag))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
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
        if (!level.addFreshEntity(bot)) {
            source.sendFailure(Component.literal("Could not spawn AI companion at this location."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned AI companion: " + safeName), false);
        return 1;
    }
}
