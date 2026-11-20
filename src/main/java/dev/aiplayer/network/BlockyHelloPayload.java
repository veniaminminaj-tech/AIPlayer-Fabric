package dev.aiplayer.network;

import dev.aiplayer.AIPlayerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BlockyHelloPayload() implements CustomPacketPayload {
    public static final Type<BlockyHelloPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AIPlayerMod.MOD_ID, "hello")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockyHelloPayload> STREAM_CODEC =
            StreamCodec.unit(new BlockyHelloPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
