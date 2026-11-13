package dev.aiplayer.client;

import dev.aiplayer.AIPlayerConfig;
import dev.aiplayer.AIPlayerMod;
import dev.aiplayer.AIProviderClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class AISettingsScreen extends Screen {
    private static final List<String> PROVIDERS = List.of("Gemini", "Ollama", "DeepSeek", "OpenAI");
    private int providerIndex;
    private EditBox modelBox;
    private EditBox endpointBox;
    private EditBox keyBox;
    private String status = "Choose a provider and enter its settings.";

    public AISettingsScreen() {
        super(Component.literal("AI Player Settings"));
    }

    @Override
    protected void init() {
        AIPlayerConfig config = AIPlayerConfig.get();
        providerIndex = Math.max(0, PROVIDERS.indexOf(config.provider));

        int center = this.width / 2;
        int left = center - 150;
        int y = this.height / 2 - 105;

        addRenderableWidget(Button.builder(providerLabel(), button -> {
            providerIndex = (providerIndex + 1) % PROVIDERS.size();
            button.setMessage(providerLabel());
            String provider = PROVIDERS.get(providerIndex);
            if (modelBox != null) modelBox.setValue(AIPlayerConfig.defaultModel(provider));
            if (endpointBox != null) endpointBox.setValue(AIPlayerConfig.defaultEndpoint(provider));
        }).bounds(left, y, 300, 20).build());

        y += 29;
        modelBox = new EditBox(font, left, y, 300, 20, Component.literal("Model"));
        modelBox.setMaxLength(128);
        modelBox.setValue(config.model == null ? "" : config.model);
        modelBox.setHint(Component.literal("Model ID"));
        addRenderableWidget(modelBox);

        y += 29;
        endpointBox = new EditBox(font, left, y, 300, 20, Component.literal("Endpoint"));
        endpointBox.setMaxLength(512);
        endpointBox.setValue(config.endpoint == null ? "" : config.endpoint);
        endpointBox.setHint(Component.literal("API endpoint URL"));
        addRenderableWidget(endpointBox);

        y += 29;
        keyBox = new EditBox(font, left, y, 300, 20, Component.literal("API key"));
        keyBox.setMaxLength(512);
        keyBox.setValue(config.apiKey == null ? "" : config.apiKey);
        keyBox.setHint(Component.literal("API key (not needed for Ollama)"));
        addRenderableWidget(keyBox);

        y += 34;
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(left, y, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Test connection"), button -> testConnection())
                .bounds(left + 155, y, 145, 20).build());

        y += 25;
        addRenderableWidget(Button.builder(Component.literal("Skin: " + config.skin), button -> {
            AIPlayerConfig.get().skin = AIPlayerConfig.get().skin.equals("steve") ? "alex" : "steve";
            button.setMessage(Component.literal("Skin: " + AIPlayerConfig.get().skin));
            status = "Skin preference saved when you press Save.";
        }).bounds(left, y, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + 155, y, 145, 20).build());
    }

    private Component providerLabel() {
        return Component.literal("Provider: " + PROVIDERS.get(providerIndex) + "  (click to change)");
    }

    private void save() {
        AIPlayerConfig config = AIPlayerConfig.get();
        config.provider = PROVIDERS.get(providerIndex);
        config.model = modelBox.getValue().trim();
        config.endpoint = endpointBox.getValue().trim();
        config.apiKey = keyBox.getValue().trim();
        try {
            AIPlayerConfig.save();
            status = "Settings saved to config/aiplayer.json";
        } catch (Exception e) {
            status = "Save failed: " + e.getMessage();
            AIPlayerMod.LOGGER.error("Could not save AI settings.", e);
        }
    }

    private void testConnection() {
        save();
        status = "Testing provider connection...";
        AIProviderClient.ask("Reply with exactly: connection successful")
                .whenComplete((reply, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null) {
                        status = "Connection failed: " + error.getMessage();
                    } else {
                        status = "Connected: " + reply;
                    }
                }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 132, 0xFFFFFF);
        graphics.drawCenteredString(font, status, width / 2, height / 2 + 100, 0xA0E0A0);
        graphics.drawCenteredString(font, "API key stays in your local config file.", width / 2, height - 18, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
