package dev.aiplayer.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class BlockyNetworking {
    private BlockyNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(
                BlockyHelloPayload.TYPE,
                BlockyHelloPayload.STREAM_CODEC
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler, BlockyHelloPayload.TYPE)) {
                ServerPlayNetworking.send(handler.player, new BlockyHelloPayload());
            }
        });
    }
}
