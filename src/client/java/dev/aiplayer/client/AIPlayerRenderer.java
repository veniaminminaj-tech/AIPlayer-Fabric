package dev.aiplayer.client;

import dev.aiplayer.AIPlayerConfig;
import dev.aiplayer.entity.AIPlayerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public final class AIPlayerRenderer extends HumanoidMobRenderer<AIPlayerEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier STEVE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final Identifier ALEX = Identifier.withDefaultNamespace("textures/entity/player/slim/alex.png");

    public AIPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return "alex".equalsIgnoreCase(AIPlayerConfig.get().skin) ? ALEX : STEVE;
    }
}
