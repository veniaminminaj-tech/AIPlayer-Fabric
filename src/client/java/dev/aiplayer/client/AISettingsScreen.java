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
    private static final List<String> PROVIDERS = List.of("Gemini", "OpenRouter", "DeepSeek", "OpenAI");
    private int providerIndex;
    private EditBox modelBox;
    private EditBox endpointBox;
    private EditBox nameBox;
    private EditBox personalityBox;
    private String status = "API key: .minecraft/ai-playerv/api-key.txt";
    private int statusColor = 0xA0E0A0;

    public AISettingsScreen() {
        super(Component.literal("AI Player Settings"));
    }

    @Override
    protected void init() {
        AIPlayerConfig config = AIPlayerConfig.get();
        providerIndex = Math.max(0, PROVIDERS.indexOf(config.provider));

        int center = this.width / 2;
        int left = center - 150;
        int y = Math.max(38, this.height / 2 - 120);

        addRenderableWidget(Button.builder(providerLabel(), button -> {
            providerIndex = (providerIndex + 1) % PROVIDERS.size();
            button.setMessage(providerLabel());
            String provider = PROVIDERS.get(providerIndex);
            modelBox.setValue(AIPlayerConfig.defaultModel(provider));
            endpointBox.setValue(AIPlayerConfig.defaultEndpoint(provider));
            status = "Provider preset changed. API key is read from ai-playerv/api-key.txt.";
        }).bounds(left, y, 300, 20).build());

        y += 25;
        modelBox = field(left, y, "Model ID", config.model, 128);
        y += 25;
        endpointBox = field(left, y, "HTTPS API endpoint URL", config.endpoint, 512);
        y += 25;
        nameBox = field(left, y, "Companion name", config.botName, 32);
        y += 25;
        personalityBox = field(left, y, "Personality", config.personality, 180);

        y += 30;
        addRenderableWidget(Button.builder(Component.literal("Save settings"), button -> save())
                .bounds(left, y, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Test connection"), button -> testConnection())
                .bounds(left + 155, y, 145, 20).build());

        y += 25;
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left, y, 300, 20).build());
    }

    private EditBox field(int x, int y, String hint, String value, int maxLength) {
        EditBox box = new EditBox(font, x, y, 300, 20, Component.literal(hint));
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setHint(Component.literal(hint));
        addRenderableWidget(box);
        return box;
    }

    private Component providerLabel() {
        return Component.literal("Provider: " + PROVIDERS.get(providerIndex) + "  (click to change)");
    }

    private void save() {
        AIPlayerConfig config = AIPlayerConfig.get();
        config.provider = PROVIDERS.get(providerIndex);
        config.model = modelBox.getValue().trim();
        config.endpoint = endpointBox.getValue().trim();
        config.botName = nameBox.getValue().trim().isEmpty() ? "AI Player" : nameBox.getValue().trim();
        config.personality = personalityBox.getValue().trim().isEmpty()
                ? "Friendly, curious, helpful, and knowledgeable about Minecraft."
                : personalityBox.getValue().trim();
        try {
            AIPlayerConfig.save();
            status = "Saved. API key: .minecraft/ai-playerv/api-key.txt";
            statusColor = 0x80E0A0;
        } catch (Exception e) {
            status = "Save failed: " + e.getMessage();
            statusColor = 0xFF8080;
            AIPlayerMod.LOGGER.error("Could not save AI settings.", e);
        }
    }

    private void testConnection() {
        save();
        if (status.startsWith("Save failed")) return;
        status = "Testing provider connection...";
        statusColor = 0xFFFF80;
        AIProviderClient.ask("Reply with exactly: connection successful")
                .whenComplete((reply, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null) {
                        status = "Connection failed: " + error.getMessage();
                        statusColor = 0xFF8080;
                    } else {
                        status = "Connected successfully.";
                        statusColor = 0x80E0A0;
                    }
                }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(font, status, width / 2, height - 28, statusColor);
        graphics.drawCenteredString(font, "M: settings | Skin PNG: .minecraft/ai-playerv/skin.png", width / 2, height - 14, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
