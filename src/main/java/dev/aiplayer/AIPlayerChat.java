package dev.aiplayer;

import dev.aiplayer.entity.AIPlayerEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AIPlayerChat {
    private static final Map<UUID, Deque<String>> MEMORY = new ConcurrentHashMap<>();
    private AIPlayerChat() {}

    public static void clearMemory(UUID playerId) {
        MEMORY.remove(playerId);
    }

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, parameters) -> {
            AIPlayerEntity bot = sender.serverLevel()
                    .getEntitiesOfClass(AIPlayerEntity.class, sender.getBoundingBox().inflate(32.0))
                    .stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(sender)))
                    .orElse(null);

            if (bot == null) return;

            AIPlayerConfig config = AIPlayerConfig.get();
            String playerName = sender.getName().getString();
            String playerContext = "Player: " + playerName
                    + ". Dimension: " + sender.level().dimension().location()
                    + ". Position: " + sender.blockPosition().getX() + ", "
                    + sender.blockPosition().getY() + ", " + sender.blockPosition().getZ()
                    + ". Health: " + Math.round(sender.getHealth()) + "/" + Math.round(sender.getMaxHealth())
                    + ". Nearby companion: " + bot.getName().getString() + ".";
            String playerText = message.getContent();

            Deque<String> history = MEMORY.computeIfAbsent(sender.getUUID(), ignored -> new ArrayDeque<>());
            String prompt;
            synchronized (history) {
                StringBuilder conversation = new StringBuilder(playerContext).append("\nRecent conversation:\n");
                for (String line : history) conversation.append(line).append('\n');
                conversation.append(playerName).append(": ").append(playerText);
                prompt = conversation.toString();
                history.addLast(playerName + ": " + playerText);
                while (history.size() > Math.max(0, config.memoryMessages) * 2) history.removeFirst();
            }

            AIProviderClient.ask(prompt).whenComplete((reply, error) -> {
                sender.getServer().execute(() -> {
                    if (!sender.isRemoved()) {
                        String answer = error == null ? reply : "Connection error: " + rootMessage(error);
                        sender.sendSystemMessage(Component.literal("[" + config.botName + "] " + answer));
                        if (error == null) {
                            synchronized (history) {
                                history.addLast(config.botName + ": " + reply);
                                while (history.size() > Math.max(0, config.memoryMessages) * 2)
                                    history.removeFirst();
                            }
                        }
                    }
                });
            });
        });
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
