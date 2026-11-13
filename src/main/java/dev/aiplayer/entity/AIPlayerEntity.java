package dev.aiplayer.entity;

import dev.aiplayer.AIPlayerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.Comparator;
import java.util.UUID;

public final class AIPlayerEntity extends PathfinderMob {
    private static final String OWNER_TAG_PREFIX = "aiplayer_owner_";
    private static final double FOLLOW_START_DISTANCE_SQUARED = 12.25;
    private static final double FOLLOW_STOP_DISTANCE_SQUARED = 6.25;
    private static final double ITEM_SEARCH_RADIUS = 10.0;
    private static final double ITEM_PICKUP_DISTANCE_SQUARED = 2.25;

    // A player-sized inventory: 27 main slots. Hotbar/equipment UI will be added later.
    private final SimpleContainer inventory = new SimpleContainer(27);

    public AIPlayerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setCustomName(Component.literal(AIPlayerConfig.get().botName));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    public static String ownerTag(UUID ownerId) {
        return OWNER_TAG_PREFIX + ownerId;
    }

    public SimpleContainer getInventory() {
        return inventory;
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
        goalSelector.addGoal(3, new CollectItemsGoal(this, 1.05));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.15));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        inventory.storeAsItemList(output.list("Inventory", ItemStack.CODEC));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        inventory.fromItemList(input.listOrEmpty("Inventory", ItemStack.CODEC));
    }

    /**
     * Collects nearby dropped items into the companion's own persistent inventory.
     * It does not take items directly from the owner or other players.
     */
    private static final class CollectItemsGoal extends Goal {
        private final AIPlayerEntity mob;
        private final double speed;
        private ItemEntity target;

        private CollectItemsGoal(AIPlayerEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.level().isClientSide()) return false;
            target = mob.level().getEntitiesOfClass(
                            ItemEntity.class,
                            mob.getBoundingBox().inflate(ITEM_SEARCH_RADIUS),
                            item -> !item.isRemoved()
                                    && !item.hasPickUpDelay()
                                    && !item.getItem().isEmpty()
                                    && mob.inventory.canAddItem(item.getItem()))
                    .stream()
                    .min(Comparator.comparingDouble(mob::distanceToSqr))
                    .orElse(null);
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null
                    && target.isAlive()
                    && !target.hasPickUpDelay()
                    && mob.distanceToSqr(target) <= ITEM_SEARCH_RADIUS * ITEM_SEARCH_RADIUS
                    && mob.inventory.canAddItem(target.getItem());
        }

        @Override
        public void tick() {
            if (target == null) return;
            if (mob.distanceToSqr(target) <= ITEM_PICKUP_DISTANCE_SQUARED) {
                ItemStack remainder = mob.inventory.addItem(target.getItem().copy());
                if (remainder.isEmpty()) {
                    target.discard();
                } else {
                    target.setItem(remainder);
                }
                mob.getNavigation().stop();
                target = null;
                return;
            }
            if (mob.tickCount % 10 == 0) {
                mob.getNavigation().moveTo(target, speed);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            target = null;
        }
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
