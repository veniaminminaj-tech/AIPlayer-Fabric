package dev.aiplayer.entity;

import dev.aiplayer.AIPlayerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

public final class AIPlayerEntity extends PathfinderMob {
    private static final String OWNER_TAG_PREFIX = "aiplayer_owner_";
    private static final double FOLLOW_START_DISTANCE_SQUARED = 12.25;
    private static final double FOLLOW_STOP_DISTANCE_SQUARED = 6.25;

    public AIPlayerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setCustomName(Component.literal(AIPlayerConfig.get().botName));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    public static String ownerTag(UUID ownerId) {
        return OWNER_TAG_PREFIX + ownerId;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.45));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.15, 1.45));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    /**
     * Follows only the player who owns this companion. It deliberately does not
     * fall back to the nearest player, which could make companions follow strangers.
     */
    private static final class FollowOwnerGoal extends Goal {
        private final AIPlayerEntity mob;
        private final double speed;
        private Player owner;

        private FollowOwnerGoal(AIPlayerEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            owner = findOwner();
            return owner != null && owner.isAlive()
                    && mob.distanceToSqr(owner) > FOLLOW_START_DISTANCE_SQUARED;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive()
                    && owner.level() == mob.level()
                    && mob.distanceToSqr(owner) > FOLLOW_STOP_DISTANCE_SQUARED;
        }

        @Override
        public void tick() {
            // Re-path periodically instead of recalculating the route every game tick.
            if (owner != null && mob.tickCount % 10 == 0) {
                mob.getNavigation().moveTo(owner, speed);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            owner = null;
        }

        private Player findOwner() {
            if (!(mob.level() instanceof ServerLevel serverLevel)) return null;
            for (String tag : mob.getTags()) {
                if (!tag.startsWith(OWNER_TAG_PREFIX)) continue;
                try {
                    UUID ownerId = UUID.fromString(tag.substring(OWNER_TAG_PREFIX.length()));
                    return serverLevel.getPlayerByUUID(ownerId);
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed ownership tags and keep searching.
                }
            }
            return null;
        }
    }
}
