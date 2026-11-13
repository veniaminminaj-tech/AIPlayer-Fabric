package dev.aiplayer;

import dev.aiplayer.entity.AIPlayerEntities;
import dev.aiplayer.entity.AIPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

public final class AIPlayerAutoSpawn {
    private AIPlayerAutoSpawn() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                spawnForPlayer(server, handler.getPlayer()));
    }

    private static void spawnForPlayer(MinecraftServer server, ServerPlayer player) {
        String ownerTag = "aiplayer_owner_" + player.getUUID().toString().replace("-", "");
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AIPlayerEntity && entity.getTags().contains(ownerTag)) return;
            }
        }

        ServerLevel level = (ServerLevel) player.level();
        AIPlayerEntity companion = AIPlayerEntities.AI_PLAYER.create(level, EntitySpawnReason.COMMAND);
        if (companion == null) {
            AIPlayerMod.LOGGER.error("Could not create an automatic AI companion for {}",
                    player.getName().getString());
            return;
        }

        String configuredName = AIPlayerConfig.get().botName;
        String name = configuredName == null || configuredName.isBlank() ? "AI Player" : configuredName;
        companion.setCustomName(Component.literal(name));
        companion.setCustomNameVisible(true);
        companion.addTag(ownerTag);
        companion.setPos(player.getX() + 2.0, player.getY(), player.getZ() + 1.0);
        companion.setYRot(player.getYRot());
        companion.setXRot(0.0F);

        if (level.addFreshEntity(companion)) {
            player.sendSystemMessage(Component.literal(name + " joined your world as your AI companion."));
            AIPlayerMod.LOGGER.info("Automatically spawned AI companion for {} in game mode {}",
                    player.getName().getString(), player.gameMode.getGameModeForPlayer());
        }
    }
}
