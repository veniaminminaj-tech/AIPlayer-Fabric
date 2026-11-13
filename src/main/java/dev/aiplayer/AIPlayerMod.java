package dev.aiplayer;

import dev.aiplayer.entity.AIPlayerEntities;
import dev.aiplayer.entity.AIPlayerEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AIPlayerMod implements ModInitializer {
    public static final String MOD_ID = "aiplayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AIPlayerConfig.load();
        FabricDefaultAttributeRegistry.register(AIPlayerEntities.AI_PLAYER, AIPlayerEntity.createAttributes());
        AIPlayerCommands.register();
        AIPlayerAutoSpawn.register();
        AIPlayerChat.register();
        LOGGER.info("AIPlayer initialized. Companions spawn automatically in every game mode; press M for settings.");
    }
}
