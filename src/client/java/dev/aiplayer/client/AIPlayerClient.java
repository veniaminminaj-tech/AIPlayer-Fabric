package dev.aiplayer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aiplayer.AIPlayerMod;
import dev.aiplayer.entity.AIPlayerEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AIPlayerClient implements ClientModInitializer {
    private static KeyMapping openSettings;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(AIPlayerMod.MOD_ID, "general"));
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aiplayer.open_settings",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_P,
                category
        ));

        EntityRenderers.register(AIPlayerEntities.AI_PLAYER, AIPlayerRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) {
                Minecraft.getInstance().setScreen(new AISettingsScreen());
            }
        });
    }
}
