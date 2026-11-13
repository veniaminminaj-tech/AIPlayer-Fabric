package dev.aiplayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class AIProviderClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private AIProviderClient() {}

    public static CompletableFuture<String> ask(String prompt) {
        AIPlayerConfig config = AIPlayerConfig.get();
        String endpoint = config.endpoint == null ? "" : config.endpoint.trim();
        if (endpoint.isEmpty()) return CompletableFuture.failedFuture(
                new IllegalStateException("Provider endpoint is empty."));
        URI endpointUri;
        try {
            endpointUri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid provider endpoint URL.", e));
        }
        String scheme = endpointUri.getScheme() == null ? "" : endpointUri.getScheme().toLowerCase(java.util.Locale.ROOT);
        if (!scheme.equals("https")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "AI Player accepts HTTPS provider endpoints only. Localhost HTTP endpoints are disabled."));
        }
        if (config.model == null || config.model.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Model ID is empty."));
        }

        String apiKey = AIPlayerConfig.readApiKey();
        if (apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Add your API key to .minecraft/ai-playerv/api-key.txt."));
        }

        JsonObject root = new JsonObject();
        root.addProperty("model", config.model.trim());
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "You are " + safe(config.botName, "AI Player")
                + ", a Minecraft AI companion. Personality: " + safe(config.personality, "Friendly and helpful.")
                + " You can discuss Minecraft and the player's provided game context. Never claim to have performed an in-game action unless the mod actually did it. Keep replies concise, useful, and friendly.");
        messages.add(system);
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);
        root.add("messages", messages);
        root.addProperty("temperature", 0.7);
        // Do not send max_tokens: allow the provider/model to use its configured output limit.

        HttpRequest.Builder builder;
        try {
            builder = HttpRequest.newBuilder(endpointUri)
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString()));
            if ("OpenRouter".equalsIgnoreCase(config.provider)) {
                builder.header("HTTP-Referer", "https://github.com/veniaminminaj-tech/AIPlayer-Fabric");
                builder.header("X-Title", "AI Player Fabric");
            }
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid provider endpoint URL.", e));
        }

        return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("HTTP " + response.statusCode() + ": " + trim(response.body()));
                    }
                    String reply = parseReply(response.body());
                    if (reply.isBlank()) throw new IllegalStateException("The AI provider returned an empty reply.");
                    return reply;
                });
    }

    private static String parseReply(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject message = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                        .getAsJsonObject("message");
                if (message != null && message.has("content") && !message.get("content").isJsonNull())
                    return message.get("content").getAsString().trim();
            }
            if (json.has("candidates") && json.getAsJsonArray("candidates").size() > 0) {
                return json.getAsJsonArray("candidates").get(0).getAsJsonObject()
                        .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                        .get("text").getAsString().trim();
            }
            throw new IllegalStateException("Response did not contain a supported chat completion.");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse provider response: " + trim(body), e);
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trim(String value) {
        if (value == null) return "";
        return value.length() > 500 ? value.substring(0, 500) + "…" : value;
    }
}
