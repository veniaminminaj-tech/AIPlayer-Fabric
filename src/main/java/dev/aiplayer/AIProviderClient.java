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

        JsonObject root = new JsonObject();
        root.addProperty("model", config.model);
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "You are a helpful Minecraft companion. Keep replies concise and friendly.");
        messages.add(system);
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);
        root.add("messages", messages);
        root.addProperty("temperature", 0.7);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()));

        String provider = config.provider == null ? "" : config.provider;
        if (!provider.equals("Ollama") && config.apiKey != null && !config.apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + config.apiKey.trim());
        }

        return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("HTTP " + response.statusCode() + ": " + trim(response.body()));
                    }
                    return parseReply(response.body());
                });
    }

    private static String parseReply(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("choices")) {
                return json.getAsJsonArray("choices").get(0).getAsJsonObject()
                        .getAsJsonObject("message").get("content").getAsString();
            }
            return json.getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse provider response: " + trim(body), e);
        }
    }

    private static String trim(String value) {
        if (value == null) return "";
        return value.length() > 500 ? value.substring(0, 500) + "…" : value;
    }
}
