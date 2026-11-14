package dev.aiplayer.client;

import dev.aiplayer.network.BlockyHelloPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public final class AIPlayerClient implements ClientModInitializer {
    public enum ConnectionMode {
        GHOST,
        FULL_MOD
    }

    private static volatile ConnectionMode connectionMode = ConnectionMode.GHOST;

    public static ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BlockyHelloPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    connectionMode = ConnectionMode.FULL_MOD;
                    if (context.client().player != null) {
                        context.client().player.displayClientMessage(
                                Component.literal("Blocky: Full Mod server detected."),
                                false
                        );
                    }
                })
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            connectionMode = ConnectionMode.GHOST;
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.literal("Blocky: Ghost Mode (local-only companion)."),
                        false
                );
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                connectionMode = ConnectionMode.GHOST
        );
    }
}
