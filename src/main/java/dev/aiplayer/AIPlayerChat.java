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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AIPlayerChat {
    private static final Map<UUID, Deque<String>> MEMORY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 3000L;
    private static final double CHAT_RANGE = 64.0;

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
                    ? "Blocky" : config.botName.trim();
            String promptText = extractPrompt(rawText, trigger);
            if (promptText == null || promptText.isBlank()) return;

            AIPlayerEntity bot = sender.level()
                    .getEntitiesOfClass(AIPlayerEntity.class, sender.getBoundingBox().inflate(CHAT_RANGE))
                    .stream()
                    .filter(entity -> entity.isAlive()
                            && entity.getTags().contains(AIPlayerEntity.ownerTag(sender.getUUID()))
                            && entity.distanceToSqr(sender) <= CHAT_RANGE * CHAT_RANGE)
                    .min(Comparator.comparingDouble((AIPlayerEntity entity) -> entity.distanceToSqr(sender)))
                    .orElse(null);
            if (bot == null) {
                sender.sendSystemMessage(Component.literal("[" + trigger + "] I can't hear you from here. Come within 4 chunks."));
                return;
            }

            String intentReply = handleSimpleIntent(promptText, bot);
            if (intentReply != null) {
                String name = bot.getName().getString();
                sender.sendSystemMessage(Component.literal("[" + name + "] " + intentReply));
                return;
            }

            long now = System.currentTimeMillis();
            Long previous = LAST_REQUEST.put(sender.getUUID(), now);
            if (previous != null && now - previous < COOLDOWN_MS) {
                LAST_REQUEST.put(sender.getUUID(), previous);
                sender.sendSystemMessage(Component.literal("[" + trigger + "] Please wait a moment before asking again."));
                return;
            }

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
                StringBuilder conversation = new StringBuilder(playerContext).append("\nFull conversation history:\n");
                for (String line : history) conversation.append(line).append('\n');
                conversation.append(playerName).append(": ").append(promptText);
                prompt = conversation.toString();
                history.addLast(playerName + ": " + promptText);
            }

            MinecraftServer server = sender.level().getServer();
            AIProviderClient.ask(prompt).whenComplete((reply, error) -> server.execute(() -> {
                if (error != null) {
                    AIPlayerMod.LOGGER.warn("AI provider request failed for player {}", playerName, error);
                    sender.sendSystemMessage(Component.literal("[" + trigger + "] I couldn't reach the AI provider. Check the server log and settings."));
                    return;
                }
                String safeReply = reply == null ? "I don't have a response right now." : reply.trim();
                synchronized (history) {
                    history.addLast(trigger + ": " + safeReply);
                }
                sender.sendSystemMessage(Component.literal("[" + trigger + "] " + safeReply));
            }));
        });
    }

    private static String handleSimpleIntent(String text, AIPlayerEntity bot) {
        String normalized = text.toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "зупини пвп", "закінчи пвп", "досить битися",
                "припини бій", "stop pvp", "stop fighting", "end fight", "стоп пвп")) {
            if (bot.isPvpTrainingMode()) bot.setPvpTrainingMode(false);
            return "Добре, тренувальний бій зупинено.";
        }
        if (containsAny(normalized, "давай пвп", "починай пвп", "бийся зі мною",
                "тренувальний бій", "починай бій", "потренуймося битися",
                "start pvp", "fight me", "let's fight", "pvp training")) {
            if (!bot.isPvpTrainingMode()) bot.setPvpTrainingMode(true);
            return "Готовий! Починаємо PvP на середній складності. Напиши «Blocky, зупини PvP», щоб завершити.";
        }
        if (containsAny(normalized, "яка складність", "яка в тебе складність", "what difficulty")) {
            return bot.isPvpTrainingMode()
                    ? "Зараз тренувальний PvP увімкнений на середній складності."
                    : "Зараз PvP вимкнений. Скажи «Blocky, давай PvP», щоб почати на середній складності.";
        }
        if (containsAny(normalized, "добудь дерево", "збирай деревину", "рубaй дерево",
                "chop wood", "gather wood", "collect logs", "добывай дерево", "собирай древесину")) {
            if (!bot.isGatherMode()) bot.toggleGatherMode();
            return "Добре, шукатиму дерева поруч і збиратиму деревину.";
        }
        if (containsAny(normalized, "припини рубати", "зупини збір", "не збирай",
                "stop gathering", "stop chopping", "хватит рубить", "не руби")) {
            if (bot.isGatherMode()) bot.toggleGatherMode();
            return "Гаразд, припиняю збирати деревину.";
        }
        if (containsAny(normalized, "покажи інвентар", "що в тебе в інвентарі", "твій інвентар",
                "show inventory", "what is in your inventory", "инвентарь")) {
            StringBuilder items = new StringBuilder();
            for (int slot = 0; slot < bot.getInventory().getContainerSize(); slot++) {
                var stack = bot.getInventory().getItem(slot);
                if (stack.isEmpty()) continue;
                if (!items.isEmpty()) items.append(", ");
                items.append(stack.getCount()).append("x ").append(stack.getHoverName().getString());
            }
            return items.isEmpty() ? "Мій інвентар поки порожній." : "У моєму інвентарі: " + items + ".";
        }
        return null;
    }

    private static boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) return true;
        }
        return false;
    }

    private static String extractPrompt(String message, String botName) {
        if (message == null) return null;
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String name = botName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("@" + name)) return trimmed.substring(name.length() + 1).replaceFirst("^[,: ]+", "").trim();
        if (lower.startsWith(name)) {
            String rest = trimmed.substring(botName.length());
            if (rest.isEmpty() || !(rest.charAt(0) == ':' || rest.charAt(0) == ',' || Character.isWhitespace(rest.charAt(0))))
                return null;
            return rest.replaceFirst("^[,: ]+", "").trim();
        }
        return null;
    }
}
