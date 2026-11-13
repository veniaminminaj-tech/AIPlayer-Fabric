package dev.aiplayer;

import dev.aiplayer.entity.AIPlayerEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;

public final class AIPlayerChat {
    private AIPlayerChat() {}

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, parameters) -> {
            AIPlayerEntity bot = sender.serverLevel()
                    .getEntitiesOfClass(AIPlayerEntity.class, sender.getBoundingBox().inflate(32.0))
                    .stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(sender)))
                    .orElse(null);

            if (bot == null) return;

            String prompt = "A Minecraft player named " + sender.getName().getString()
                    + " says: " + message.getContent()
                    + ". Reply naturally and briefly as their in-game AI companion.";
            AIProviderClient.ask(prompt).whenComplete((reply, error) -> {
                sender.getServer().execute(() -> {
                    if (!sender.isRemoved()) {
                        String answer = error == null ? reply : "AI connection error: " + error.getMessage();
                        sender.sendSystemMessage(Component.literal("[AI Player] " + answer));
                    }
                });
            });
        });
    }
}
