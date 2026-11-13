package dev.aiplayer;

import dev.aiplayer.entity.AIPlayerEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AIPlayerChat {
    private static final Map<UUID, Deque<String>> MEMORY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 3000L;
    private static final int MAX_REPLY_CHARS = 350;

    private AIPlayerChat() {}

    public static void clearMemory(UUID playerId) {
        MEMORY.remove(playerId);
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.getPlayer().getUUID();
            MEMORY.remove(playerId);
            LAST_REQUEST.remove(playerId);
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, parameters) -> {
            String rawText = message.signedContent();
            AIPlayerConfig config = AIPlayerConfig.get();
            String trigger = config.botName == null || config.botName.isBlank()
                    ? "AI Player" : config.botName.trim();
            String promptText = extractPrompt(rawText, trigger);
            if (promptText == null || promptText.isBlank()) return;

            long now = System.currentTimeMillis();
            Long previous = LAST_REQUEST.put(sender.getUUID(), now);
            if (previous != null && now - previous < COOLDOWN_MS) {
                LAST_REQUEST.put(sender.getUUID(), previous);
                sender.sendSystemMessage(Component.literal("[" + trigger + "] Please wait a moment before asking again."));
                return;
            }

            AIPlayerEntity bot = sender.level()
                    .getEntitiesOfClass(AIPlayerEntity.class, sender.getBoundingBox().inflate(32.0))
                    .stream()
                    .filter(entity -> entity.distanceToSqr(sender) <= 1024.0)
                    .min(Comparator.comparingDouble((AIPlayerEntity entity) -> entity.distanceToSqr(sender)))
                    .orElse(null);
            if (bot == null) return;

            String playerName = sender.getName().getString();
            String playerContext = "Player: " + playerName
                    + ". Dimension: " + sender.level().dimension().identifier()
                    + ". Position: " + sender.blockPosition().getX() + ", "
                    + sender.blockPosition().getY() + ", " + sender.blockPosition().getZ()
                    + ". Health: " + Math.round(sender.getHealth()) + "/" + Math.round(sender.getMaxHealth())
                    + ". Companion: " + bot.getName().getString() + ".";
            Deque<String> history = MEMORY.computeIfAbsent(sender.getUUID(), ignored -> new ArrayDeque<>());
            String prompt;
            synchronized (history) {
                StringBuilder conversation = new StringBuilder(playerContext).append("\nRecent conversation:\n");
                for (String line : history) conversation.append(line).append('\n');
                conversation.append(playerName).append(": ").append(promptText);
                prompt = conversation.toString();
                history.addLast(playerName + ": " + promptText);
                trimHistory(history, config.memoryMessages);
            }

            MinecraftServer server = sender.level().getServer();
            AIProviderClient.ask(prompt).whenComplete((reply, error) -> server.execute(() -> {
                if (error != null) {
                    AIPlayerMod.LOGGER.warn("AI provider request failed for player {}", playerName, error);
                    server.getPlayerList().broadcastSystemMessage(
                            Component.literal("[" + trigger + "] I couldn't reach the AI provider. Check the server log and settings."), false);
                    return;
                }
                String safeReply = trimReply(reply);
                synchronized (history) {
                    history.addLast(trigger + ": " + safeReply);
                    trimHistory(history, config.memoryMessages);
                }
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("[" + trigger + "] " + safeReply), false);
            }));
        });
    }

    private static String extractPrompt(String message, String botName) {
        if (message == null) return null;
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        String name = botName.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("@" + name)) return trimmed.substring(name.length() + 1).replaceFirst("^[,: ]+", "").trim();
        if (lower.startsWith(name)) {
            String rest = trimmed.substring(botName.length());
            if (rest.isEmpty() || !(rest.charAt(0) == ':' || rest.charAt(0) == ',' || Character.isWhitespace(rest.charAt(0))))
                return null;
            return rest.replaceFirst("^[,: ]+", "").trim();
        }
        return null;
    }

    private static void trimHistory(Deque<String> history, int memoryMessages) {
        while (history.size() > Math.max(0, memoryMessages) * 2) history.removeFirst();
    }

    private static String trimReply(String reply) {
        if (reply == null) return "I don't have a response right now.";
        String clean = reply.trim();
        return clean.length() > MAX_REPLY_CHARS ? clean.substring(0, MAX_REPLY_CHARS - 1) + "…" : clean;
    }
}
