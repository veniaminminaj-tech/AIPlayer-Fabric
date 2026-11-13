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
    public static final String OWNER_TAG_PREFIX = "aiplayer_owner_";
    private static final String MODE_TAG_PREFIX = "aiplayer_mode_";
    public static final String MODE_FOLLOW = "follow";
    public static final String MODE_WANDER = "wander";
    public static final String MODE_STAY = "stay";

    public AIPlayerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setCustomName(Component.literal(AIPlayerConfig.get().botName));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15);
    }

    public String getBehaviorMode() {
        for (String tag : getTags()) {
            if (tag.startsWith(MODE_TAG_PREFIX)) return tag.substring(MODE_TAG_PREFIX.length());
        }
        return MODE_FOLLOW;
    }

    public void setBehaviorMode(String mode) {
        if (!MODE_FOLLOW.equals(mode) && !MODE_WANDER.equals(mode) && !MODE_STAY.equals(mode)) {
            throw new IllegalArgumentException("Unsupported AI Player mode: " + mode);
        }
        for (String tag : getTags()) {
            if (tag.startsWith(MODE_TAG_PREFIX)) removeTag(tag);
        }
        addTag(MODE_TAG_PREFIX + mode);
        getNavigation().stop();
    }

    public boolean isOwnedBy(UUID playerId) {
        return getTags().contains(OWNER_TAG_PREFIX + playerId);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.45));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.15, 1.45));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.12));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return MODE_WANDER.equals(getBehaviorMode()) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return MODE_WANDER.equals(getBehaviorMode()) && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new StayGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

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
            if (!MODE_FOLLOW.equals(mob.getBehaviorMode())) return false;
            owner = findOwner();
            return owner != null && owner.isAlive() && mob.distanceToSqr(owner) > 6.25;
        }

        @Override
        public boolean canContinueToUse() {
            return MODE_FOLLOW.equals(mob.getBehaviorMode())
                    && owner != null && owner.isAlive()
                    && mob.distanceToSqr(owner) > 6.25
                    && mob.distanceToSqr(owner) < 1024.0;
        }

        @Override
        public void tick() {
            if (owner != null) mob.getNavigation().moveTo(owner, speed);
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
                    // Ignore malformed ownership tags.
                }
            }
            return null;
        }
    }

    private static final class StayGoal extends Goal {
        private final AIPlayerEntity mob;

        private StayGoal(AIPlayerEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return MODE_STAY.equals(mob.getBehaviorMode());
        }

        @Override
        public boolean canContinueToUse() {
            return MODE_STAY.equals(mob.getBehaviorMode());
        }

        @Override
        public void tick() {
            mob.getNavigation().stop();
        }
    }
}
