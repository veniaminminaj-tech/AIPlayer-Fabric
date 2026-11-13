package dev.aiplayer.entity;

import dev.aiplayer.AIPlayerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public final class AIPlayerEntity extends PathfinderMob {
    public AIPlayerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setCustomName(Component.literal(AIPlayerConfig.get().botName));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FollowNearbyPlayerGoal(this, 1.12));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    private static final class FollowNearbyPlayerGoal extends Goal {
        private final AIPlayerEntity mob;
        private final double speed;
        private Player target;

        private FollowNearbyPlayerGoal(AIPlayerEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            target = mob.level().getNearestPlayer(mob, 16.0);
            return target != null && target.isAlive() && mob.distanceToSqr(target) > 6.25;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive()
                    && mob.distanceToSqr(target) > 6.25
                    && mob.distanceToSqr(target) < 256.0;
        }

        @Override
        public void tick() {
            if (target != null) mob.getNavigation().moveTo(target, speed);
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            target = null;
        }
    }
}
