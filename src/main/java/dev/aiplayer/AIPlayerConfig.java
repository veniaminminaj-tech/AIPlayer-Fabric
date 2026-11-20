package dev.aiplayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class AIPlayerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aiplayer.json");
    public static final Path PLAYER_FOLDER = FabricLoader.getInstance().getGameDir().resolve("ai-playerv");
    public static final Path API_KEY_FILE = PLAYER_FOLDER.resolve("api-key.txt");
    public static final Path SKIN_FILE = PLAYER_FOLDER.resolve("skin.png");
    private static final Path README_FILE = PLAYER_FOLDER.resolve("README.txt");

    public String provider = "Gemini";
    public String model = "gemini-2.5-flash";
    public transient String apiKey = "";
    public String endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    public String skin = "steve";
    public String botName = "Blocky";
    public String personality = "Friendly, curious, helpful, and knowledgeable about Minecraft.";
    public int memoryMessages = 8;

    private static AIPlayerConfig INSTANCE = new AIPlayerConfig();

    private AIPlayerConfig() {}

    public static synchronized AIPlayerConfig get() {
        return INSTANCE;
    }

    public static synchronized void load() {
        ensurePlayerFolder();
        try {
            if (Files.exists(FILE)) {
                AIPlayerConfig loaded = GSON.fromJson(Files.readString(FILE), AIPlayerConfig.class);
                if (loaded != null) {
                    if (loaded.provider == null || loaded.provider.equalsIgnoreCase("Ollama")) loaded.provider = "Gemini";
                    if (loaded.model == null || loaded.model.equals("llama3.2")) loaded.model = defaultModel(loaded.provider);
                    if (loaded.endpoint == null || loaded.endpoint.contains("localhost")) loaded.endpoint = defaultEndpoint(loaded.provider);
                    loaded.apiKey = readApiKey();
                    if (loaded.skin == null) loaded.skin = "steve";
                    if (loaded.botName == null || loaded.botName.isBlank() || loaded.botName.equalsIgnoreCase("AI Player")) loaded.botName = "Blocky";
                    if (loaded.personality == null || loaded.personality.isBlank())
                        loaded.personality = "Friendly, curious, helpful, and knowledgeable about Minecraft.";
                    loaded.memoryMessages = Math.max(0, Math.min(20, loaded.memoryMessages));
                    INSTANCE = loaded;
                }
            }
        } catch (Exception e) {
            AIPlayerMod.LOGGER.error("Could not read config/aiplayer.json; using defaults.", e);
            INSTANCE = new AIPlayerConfig();
        }
        if (Files.notExists(FILE)) {
            try {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, GSON.toJson(INSTANCE));
                AIPlayerMod.LOGGER.info("Created default config at {}", FILE);
            } catch (IOException e) {
                AIPlayerMod.LOGGER.error("Could not create default config/aiplayer.json.", e);
            }
        }
        INSTANCE.apiKey = readApiKey();
    }

    public static synchronized void save() throws IOException {
        ensurePlayerFolder();
        INSTANCE.apiKey = "";
        Files.createDirectories(FILE.getParent());
        Files.writeString(FILE, GSON.toJson(INSTANCE));
    }

    public static synchronized String readApiKey() {
        ensurePlayerFolder();
        try {
            return Files.exists(API_KEY_FILE) ? Files.readString(API_KEY_FILE).trim() : "";
        } catch (IOException e) {
            AIPlayerMod.LOGGER.error("Could not read .minecraft/ai-playerv/api-key.txt", e);
            return "";
        }
    }

    public static synchronized void ensurePlayerFolder() {
        try {
            Files.createDirectories(PLAYER_FOLDER);
            if (Files.notExists(API_KEY_FILE)) Files.writeString(API_KEY_FILE, "");
            // Install the user's supplied Blocky texture only when no custom skin exists.
            if (Files.notExists(SKIN_FILE)) {
                Files.write(SKIN_FILE, DefaultBlockySkin.pngBytes());
                AIPlayerMod.LOGGER.info("Installed default Blocky skin at {}", SKIN_FILE);
            }
            if (Files.notExists(README_FILE)) {
                Files.writeString(README_FILE, """
                        AI Player files

                        1. Paste your provider API key into api-key.txt and save the file.
                        2. The supplied Blocky skin is installed as skin.png automatically.
                        3. Replace skin.png with your own 64x64 PNG to use a custom skin.
                        4. Keep api-key.txt private; do not share it or upload it to GitHub.
                        """);
            }
        } catch (IOException e) {
            AIPlayerMod.LOGGER.error("Could not create .minecraft/ai-playerv folder.", e);
        }
    }

    public static String defaultEndpoint(String provider) {
        return switch (provider) {
            case "OpenRouter" -> "https://openrouter.ai/api/v1/chat/completions";
            case "DeepSeek" -> "https://api.deepseek.com/chat/completions";
            case "OpenAI" -> "https://api.openai.com/v1/chat/completions";
            default -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        };
    }

    public static String defaultModel(String provider) {
        return switch (provider) {
            case "OpenRouter" -> "google/gemini-2.5-flash";
            case "DeepSeek" -> "deepseek-chat";
            case "OpenAI" -> "gpt-4.1-mini";
            default -> "gemini-2.5-flash";
        };
    }
}
