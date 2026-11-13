package dev.aiplayer.client;

import dev.aiplayer.AIPlayerConfig;
import dev.aiplayer.entity.AIPlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AIPlayerRenderer extends HumanoidMobRenderer<AIPlayerEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier STEVE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final Identifier ALEX = Identifier.withDefaultNamespace("textures/entity/player/slim/alex.png");
    private static final Identifier CUSTOM = Identifier.fromNamespaceAndPath("aiplayer", "textures/custom_player_skin");
    private static boolean customSkinLoaded;

    public AIPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        Identifier custom = loadCustomSkin();
        if (custom != null) return custom;
        return "alex".equalsIgnoreCase(AIPlayerConfig.get().skin) ? ALEX : STEVE;
    }

    private static Identifier loadCustomSkin() {
        if (customSkinLoaded) {
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            return manager.getTexture(CUSTOM) == null ? null : CUSTOM;
        }
        customSkinLoaded = true;
        Path skinPath = AIPlayerConfig.SKIN_FILE;
        if (!Files.isRegularFile(skinPath)) return null;
        try (InputStream input = Files.newInputStream(skinPath)) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() != 64 || image.getHeight() != 64) {
                image.close();
                AIPlayerModLog.warn("Custom skin must be a 64x64 PNG: " + skinPath);
                return null;
            }
            DynamicTexture texture = new DynamicTexture(() -> "AI Player custom skin", image);
            Minecraft.getInstance().getTextureManager().register(CUSTOM, texture);
            return CUSTOM;
        } catch (Exception e) {
            AIPlayerModLog.warn("Could not load custom skin PNG: " + e.getMessage());
            return null;
        }
    }

    private static final class AIPlayerModLog {
        private static void warn(String message) {
            dev.aiplayer.AIPlayerMod.LOGGER.warn(message);
        }
    }
}
