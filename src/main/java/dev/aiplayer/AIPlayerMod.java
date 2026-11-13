package dev.aiplayer;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AIPlayerMod implements ModInitializer {
    public static final String MOD_ID = "aiplayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AIPlayerConfig.load();
        LOGGER.info("AIPlayer initialized. Press P in-game to configure an AI provider.");
    }
}
