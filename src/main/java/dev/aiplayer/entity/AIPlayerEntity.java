package dev.aiplayer.entity;

import dev.aiplayer.AIPlayerConfig;
import dev.aiplayer.AIPlayerMod;
import dev.aiplayer.AIProviderClient;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.entity.FakePlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.BlockState;
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
    private static final int LOG_SEARCH_RADIUS = 5;
    private static final double LOG_REACH_DISTANCE_SQUARED = 4.0;
    // Four chunks correspond to 64 blocks in each horizontal direction.
    private static final float AI_VIEW_RANGE = 64.0F;

    private final SimpleContainer inventory = new SimpleContainer(27);
    private boolean gatherMode;
    private String brainObjective = "FOLLOW_OWNER";
    private boolean plannerBusy;
    private int lastPlannerTick;
    private boolean pvpTrainingMode;

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

    public boolean isGatherMode() {
        return gatherMode;
    }

    public boolean toggleGatherMode() {
        gatherMode = !gatherMode;
        return gatherMode;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.45));
        goalSelector.addGoal(2, new OwnerPvpGoal(this, 1.15));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.15, 1.45));
        goalSelector.addGoal(4, new GatherLogsGoal(this, 1.0));
        goalSelector.addGoal(5, new CollectItemsGoal(this, 1.05));
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.15));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, AI_VIEW_RANGE));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        inventory.storeAsItemList(output.list("Inventory", ItemStack.CODEC));
        output.putBoolean("GatherMode", gatherMode);
        output.putString("BrainObjective", brainObjective);
        output.putBoolean("PvpTrainingMode", pvpTrainingMode);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        inventory.fromItemList(input.listOrEmpty("Inventory", ItemStack.CODEC));
        gatherMode = input.getBooleanOr("GatherMode", false);
        brainObjective = input.getStringOr("BrainObjective", "FOLLOW_OWNER");
        pvpTrainingMode = input.getBooleanOr("PvpTrainingMode", false);
    }


    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel && !isRemoved()
                && tickCount - lastPlannerTick >= 200 && !plannerBusy) {
            lastPlannerTick = tickCount;
            requestStrategicDecision(serverLevel);
        }
    }

    public String getBrainObjective() {
        return brainObjective;
    }

    public boolean isPvpTrainingMode() {
        return pvpTrainingMode;
    }

    public void setPvpTrainingMode(boolean enabled) {
        pvpTrainingMode = enabled;
        if (enabled) {
            gatherMode = false;
            brainObjective = "PVP_TRAINING_MEDIUM";
        } else if ("PVP_TRAINING_MEDIUM".equals(brainObjective)) {
            brainObjective = "FOLLOW_OWNER";
        }
        if (!enabled) getNavigation().stop();
    }

    private void requestStrategicDecision(ServerLevel serverLevel) {
        Player owner = findOwner(serverLevel);
        if (owner == null || !owner.isAlive() || distanceToSqr(owner) > 64.0 * 64.0) return;

        plannerBusy = true;
        StringBuilder inventorySummary = new StringBuilder();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!inventorySummary.isEmpty()) inventorySummary.append(", ");
            inventorySummary.append(stack.getCount()).append("x ").append(stack.getHoverName().getString());
        }
        if (inventorySummary.isEmpty()) inventorySummary.append("empty");

        long nearbyHostiles = serverLevel.getEntitiesOfClass(
                Monster.class, getBoundingBox().inflate(16.0), Entity::isAlive).size();
        long nearbyLogs = BlockPos.betweenClosedStream(
                        blockPosition().offset(-8, -2, -8),
                        blockPosition().offset(8, 5, 8))
                .filter(pos -> serverLevel.getBlockState(pos).is(BlockTags.LOGS))
                .count();

        String observation = """
                You are the long-term planner for Blocky, an autonomous Minecraft survival companion.
                Use a two-level agent design: you are the slow strategic planner; the mod handles movement and actions.
                Choose exactly ONE action token, with no explanation:
                ACTION=GATHER_WOOD
                ACTION=FOLLOW_OWNER
                ACTION=REST
                Current objective: %s
                Owner: %s
                Health: %.1f/%.1f
                Inventory: %s
                Nearby logs: %d
                Nearby hostile mobs: %d
                Rules: prioritize survival and useful progress; do not repeat a failed action endlessly.
                Choose GATHER_WOOD only if nearby logs exist and inventory is not already full.
                Choose FOLLOW_OWNER if the owner is far away or the area has threats.
                Otherwise choose REST.
                """.formatted(brainObjective, owner.getName().getString(), getHealth(),
                getMaxHealth(), inventorySummary, nearbyLogs, nearbyHostiles);

        AIProviderClient.ask(observation).whenComplete((reply, error) ->
                serverLevel.getServer().execute(() -> {
                    plannerBusy = false;
                    if (isRemoved() || !isAlive()) return;
                    if (error != null) {
                        AIPlayerMod.LOGGER.warn("Blocky planner request failed; keeping current objective.", error);
                        return;
                    }
                    String decision = reply == null ? "" : reply.trim().toUpperCase(java.util.Locale.ROOT);
                    if (decision.contains("ACTION=GATHER_WOOD")) {
                        brainObjective = "GATHER_WOOD";
                        if (!gatherMode) toggleGatherMode();
                    } else if (decision.contains("ACTION=FOLLOW_OWNER")) {
                        brainObjective = "FOLLOW_OWNER";
                        gatherMode = false;
                    } else if (decision.contains("ACTION=REST")) {
                        brainObjective = "REST";
                        gatherMode = false;
                    } else {
                        AIPlayerMod.LOGGER.warn("Blocky planner returned an unknown action: {}", reply);
                    }
                }));
    }

    private Player findOwner(ServerLevel serverLevel) {
        for (String tag : getTags()) {
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

    private FakePlayer createBlockyActor(ServerLevel serverLevel) {
        for (String tag : getTags()) {
            if (!tag.startsWith(OWNER_TAG_PREFIX)) continue;
            try {
                UUID ownerId = UUID.fromString(tag.substring(OWNER_TAG_PREFIX.length()));
                Player owner = serverLevel.getPlayerByUUID(ownerId);
                String ownerName = owner == null ? "Player" : owner.getName().getString();
                String profileName = "[Blocky]";
                FakePlayer actor = FakePlayer.get(serverLevel, new GameProfile(ownerId, profileName));
                actor.setPos(getX(), getY(), getZ());
                actor.setYRot(getYRot());
                return actor;
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed owner tags.
            }
        }
        return null;
    }

    private static final class OwnerPvpGoal extends Goal {
        private final AIPlayerEntity mob;
        private final double speed;
        private Player owner;
        private int attackCooldown;

        private OwnerPvpGoal(AIPlayerEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!mob.pvpTrainingMode || !(mob.level() instanceof ServerLevel serverLevel)) return false;
            owner = mob.findOwner(serverLevel);
            return owner != null && owner.isAlive() && owner.level() == mob.level()
                    && mob.distanceToSqr(owner) <= 24.0 * 24.0;
        }

        @Override
        public boolean canContinueToUse() {
            return mob.pvpTrainingMode && owner != null && owner.isAlive()
                    && owner.level() == mob.level()
                    && mob.distanceToSqr(owner) <= 24.0 * 24.0;
        }

        @Override
        public void start() {
            attackCooldown = 12;
        }

        @Override
        public void tick() {
            if (owner == null) return;
            mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);
            if (mob.tickCount % 5 == 0 && mob.distanceToSqr(owner) > 3.2 * 3.2) {
                mob.getNavigation().moveTo(owner, speed);
            }
            if (attackCooldown > 0) attackCooldown--;
            if (mob.distanceToSqr(owner) <= 3.2 * 3.2
                    && attackCooldown <= 0 && mob.hasLineOfSight(owner)) {
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                if (mob.level() instanceof ServerLevel serverLevel) {
                    mob.doHurtTarget(serverLevel, owner);
                }
                // Medium difficulty: a readable reaction window, not instant attacks.
                attackCooldown = 18 + mob.getRandom().nextInt(9);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            owner = null;
            attackCooldown = 0;
        }
    }

    private static final class GatherLogsGoal extends Goal {
        private final AIPlayerEntity mob;
        private final double speed;
        private BlockPos target;
        private int miningTicks;
        private int ticksToMine;

        private GatherLogsGoal(AIPlayerEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!mob.gatherMode || mob.level().isClientSide()) return false;
            target = findNearestLog();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return mob.gatherMode && target != null
                    && mob.level().getBlockState(target).is(BlockTags.LOGS)
                    && mob.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5)
                    <= 12.0 * 12.0;
        }

        @Override
        public void start() {
            miningTicks = 0;
            BlockState state = mob.level().getBlockState(target);
            float hardness = state.getDestroySpeed(mob.level(), target);
            ticksToMine = Math.max(20, Math.min(100, (int) (hardness * 30.0F)));
        }

        @Override
        public void tick() {
            if (target == null) return;
            BlockState state = mob.level().getBlockState(target);
            if (!state.is(BlockTags.LOGS) || state.requiresCorrectToolForDrops()) {
                stop();
                return;
            }

            double distance = mob.distanceToSqr(
                    target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
            if (distance > LOG_REACH_DISTANCE_SQUARED) {
                if (mob.tickCount % 10 == 0) {
                    mob.getNavigation().moveTo(
                            target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
                }
                return;
            }

            mob.getNavigation().stop();
            miningTicks++;
            if (mob.level() instanceof ServerLevel serverLevel) {
                int stage = Math.min(9, miningTicks * 10 / ticksToMine);
                serverLevel.destroyBlockProgress(mob.getId(), target, stage);
                if (miningTicks >= ticksToMine) {
                    serverLevel.destroyBlockProgress(mob.getId(), target, -1);
                    FakePlayer blockyPlayer = mob.createBlockyActor(serverLevel);
                    if (blockyPlayer != null) {
                        blockyPlayer.setPos(mob.getX(), mob.getY(), mob.getZ());
                        blockyPlayer.setYRot(mob.getYRot());
                        blockyPlayer.gameMode.destroyBlock(target);
                    }
                    target = null;
                }
            }
        }

        @Override
        public void stop() {
            if (target != null && mob.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlockProgress(mob.getId(), target, -1);
            }
            mob.getNavigation().stop();
            target = null;
            miningTicks = 0;
            ticksToMine = 0;
        }

        private BlockPos findNearestLog() {
            BlockPos origin = mob.blockPosition();
            return BlockPos.betweenClosedStream(
                            origin.offset(-LOG_SEARCH_RADIUS, -1, -LOG_SEARCH_RADIUS),
                            origin.offset(LOG_SEARCH_RADIUS, 4, LOG_SEARCH_RADIUS))
                    .filter(pos -> {
                        BlockState state = mob.level().getBlockState(pos);
                        if (!state.is(BlockTags.LOGS) || state.requiresCorrectToolForDrops()) return false;
                        if (!mob.inventory.canAddItem(new ItemStack(state.getBlock().asItem()))) return false;
                        return !mob.level().getBlockState(pos.below()).is(BlockTags.LOGS);
                    })
                    .min(Comparator.comparingDouble(pos -> mob.distanceToSqr(
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
                    .map(BlockPos::immutable)
                    .orElse(null);
        }
    }

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
