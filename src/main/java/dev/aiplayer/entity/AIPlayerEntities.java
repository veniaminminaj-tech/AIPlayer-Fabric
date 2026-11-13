package dev.aiplayer.entity;

import dev.aiplayer.AIPlayerMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.Registry;

public final class AIPlayerEntities {
    public static final ResourceKey<EntityType<?>> AI_PLAYER_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AIPlayerMod.MOD_ID, "ai_player")
    );

    public static final EntityType<AIPlayerEntity> AI_PLAYER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AIPlayerMod.MOD_ID, "ai_player"),
            EntityType.Builder.of(AIPlayerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build(AI_PLAYER_KEY)
    );

    private AIPlayerEntities() {}
}
