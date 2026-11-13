package dev.aiplayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AIPlayerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "aiplayer.json");

    public String provider = "Gemini";
    public String model = "gemini-2.5-flash";
    public String apiKey = "";
    public String endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    public String skin = "steve";

    private static AIPlayerConfig INSTANCE = new AIPlayerConfig();

    private AIPlayerConfig() {}

    public static synchronized AIPlayerConfig get() {
        return INSTANCE;
    }

    public static synchronized void load() {
        try {
            if (Files.exists(FILE)) {
                AIPlayerConfig loaded = GSON.fromJson(Files.readString(FILE), AIPlayerConfig.class);
                if (loaded != null) INSTANCE = loaded;
            }
        } catch (Exception e) {
            AIPlayerMod.LOGGER.error("Could not read config/aiplayer.json; using defaults.", e);
            INSTANCE = new AIPlayerConfig();
        }
    }

    public static synchronized void save() throws IOException {
        Files.createDirectories(FILE.getParent());
        Files.writeString(FILE, GSON.toJson(INSTANCE));
    }

    public static String defaultEndpoint(String provider) {
        return switch (provider) {
            case "Ollama" -> "http://localhost:11434/v1/chat/completions";
            case "DeepSeek" -> "https://api.deepseek.com/chat/completions";
            case "OpenAI" -> "https://api.openai.com/v1/chat/completions";
            default -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        };
    }

    public static String defaultModel(String provider) {
        return switch (provider) {
            case "Ollama" -> "llama3.2";
            case "DeepSeek" -> "deepseek-chat";
            case "OpenAI" -> "gpt-4.1-mini";
            default -> "gemini-2.5-flash";
        };
    }
}
