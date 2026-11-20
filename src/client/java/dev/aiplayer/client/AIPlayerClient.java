package dev.aiplayer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aiplayer.AIPlayerConfig;
import dev.aiplayer.AIPlayerMod;
import dev.aiplayer.entity.AIPlayerEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import dev.aiplayer.network.BlockyHelloPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AIPlayerClient implements ClientModInitializer {
    public enum ConnectionMode { GHOST, FULL_MOD }
    private static volatile ConnectionMode connectionMode = ConnectionMode.GHOST;
    private static KeyMapping openSettings;

    public static ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    @Override
    public void onInitializeClient() {
        AIPlayerConfig.ensurePlayerFolder();
        ClientPlayNetworking.registerGlobalReceiver(BlockyHelloPayload.TYPE, (payload, context) ->
                context.client().execute(() -> connectionMode = ConnectionMode.FULL_MOD));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                connectionMode = ConnectionMode.GHOST);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                connectionMode = ConnectionMode.GHOST);
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(AIPlayerMod.MOD_ID, "general"));
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aiplayer.open_settings",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_M,
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
