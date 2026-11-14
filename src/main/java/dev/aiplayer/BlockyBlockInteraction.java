package dev.aiplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;

/**
 * Safe, generic inventory interaction for vanilla and modded block entities
 * that expose Minecraft's standard Container interface.
 */
public final class BlockyBlockInteraction {
    private static final int SEARCH_RADIUS = 5;

    private BlockyBlockInteraction() {}

    public static String serviceNearestFurnace(SimpleContainer inventory,
                                                ServerLevel level,
                                                BlockPos origin) {
        AbstractFurnaceBlockEntity furnace = BlockPos.betweenClosedStream(
                        origin.offset(-SEARCH_RADIUS, -2, -SEARCH_RADIUS),
                        origin.offset(SEARCH_RADIUS, 2, SEARCH_RADIUS))
                .map(level::getBlockEntity)
                .filter(entity -> entity instanceof AbstractFurnaceBlockEntity)
                .map(entity -> (AbstractFurnaceBlockEntity) entity)
                .min(Comparator.comparingDouble(entity ->
                        entity.getBlockPos().distToCenterSqr(origin.getX() + 0.5,
                                origin.getY() + 0.5, origin.getZ() + 0.5)))
                .orElse(null);

        if (furnace == null) return "Поруч не знайшов пічку.";

        int taken = moveSlotToInventory(furnace, 2, inventory);
        int fuelAdded = 0;

        // Only move items recognized by the vanilla furnace-fuel tag.
        // Input items are deliberately left untouched until recipe-aware smelting is added.
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.is(ItemTags.FURNACE_FUELS)) continue;
            fuelAdded += insertIntoSlot(inventory, slot, furnace, 1);
            if (fuelAdded > 0) break;
        }

        furnace.setChanged();
        if (taken == 0 && fuelAdded == 0) {
            return "Знайшов пічку, але немає готового результату для забирання або палива для завантаження.";
        }
        return "Пічка обслужена: забрав " + taken + " предметів і додав " + fuelAdded + " одиниць палива.";
    }

    /**
     * Stores only surplus: at least one item from every stack stays with Blocky.
     * Works with vanilla and modded containers implementing Container.
     */
    public static String storeExcessInNearestContainer(SimpleContainer inventory,
                                                       ServerLevel level,
                                                       BlockPos origin) {
        BlockEntity target = BlockPos.betweenClosedStream(
                        origin.offset(-SEARCH_RADIUS, -2, -SEARCH_RADIUS),
                        origin.offset(SEARCH_RADIUS, 2, SEARCH_RADIUS))
                .map(level::getBlockEntity)
                .filter(entity -> entity instanceof Container
                        && !(entity instanceof AbstractFurnaceBlockEntity))
                .min(Comparator.comparingDouble(entity ->
                        entity.getBlockPos().distToCenterSqr(origin.getX() + 0.5,
                                origin.getY() + 0.5, origin.getZ() + 0.5)))
                .orElse(null);

        if (target == null) return "Поруч не знайшов сумісну скриню, бочку або модовий контейнер.";

        Container container = (Container) target;
        int moved = 0;
        for (int sourceSlot = 0; sourceSlot < inventory.getContainerSize(); sourceSlot++) {
            ItemStack stack = inventory.getItem(sourceSlot);
            if (stack.isEmpty() || stack.getCount() <= 1) continue;

            // Keep one unit in Blocky's inventory; store only the surplus.
            int surplus = stack.getCount() - 1;
            for (int destinationSlot = 0;
                 destinationSlot < container.getContainerSize() && surplus > 0;
                 destinationSlot++) {
                moved += insertAmount(inventory, sourceSlot, container, destinationSlot, surplus);
                ItemStack updated = inventory.getItem(sourceSlot);
                surplus = updated.isEmpty() ? 0 : updated.getCount() - 1;
            }
        }

        container.setChanged();
        return moved == 0
                ? "Знайшов контейнер, але зайвих предметів для безпечного зберігання немає."
                : "Зберіг у контейнері " + moved + " зайвих предметів; по одному залишив у своєму інвентарі.";
    }

    /** Backwards-compatible entry point; no longer empties containers blindly. */
    public static String interactWithNearestContainer(SimpleContainer inventory,
                                                       ServerLevel level,
                                                       BlockPos origin) {
        return storeExcessInNearestContainer(inventory, level, origin);
    }

    private static int insertIntoSlot(SimpleContainer source, int sourceSlot,
                                      Container destination, int destinationSlot) {
        ItemStack stack = source.getItem(sourceSlot);
        return insertAmount(source, sourceSlot, destination, destinationSlot, stack.getCount());
    }

    private static int insertAmount(SimpleContainer source, int sourceSlot,
                                    Container destination, int destinationSlot, int requested) {
        ItemStack sourceStack = source.getItem(sourceSlot);
        if (sourceStack.isEmpty() || requested <= 0
                || !destination.canPlaceItem(destinationSlot, sourceStack)) return 0;

        ItemStack targetStack = destination.getItem(destinationSlot);
        if (!targetStack.isEmpty()
                && !ItemStack.isSameItemSameComponents(targetStack, sourceStack)) return 0;

        int capacity = targetStack.isEmpty()
                ? Math.min(sourceStack.getMaxStackSize(), destination.getMaxStackSize())
                : Math.min(targetStack.getMaxStackSize(), destination.getMaxStackSize()) - targetStack.getCount();
        int amount = Math.min(Math.min(sourceStack.getCount(), requested), Math.max(0, capacity));
        if (amount <= 0) return 0;

        if (targetStack.isEmpty()) {
            ItemStack inserted = sourceStack.copy();
            inserted.setCount(amount);
            destination.setItem(destinationSlot, inserted);
        } else {
            targetStack.grow(amount);
            destination.setItem(destinationSlot, targetStack);
        }

        sourceStack.shrink(amount);
        if (sourceStack.isEmpty()) source.setItem(sourceSlot, ItemStack.EMPTY);
        source.setChanged();
        destination.setChanged();
        return amount;
    }

    private static int moveSlotToInventory(Container source, int sourceSlot,
                                           SimpleContainer inventory) {
        ItemStack stack = source.getItem(sourceSlot);
        if (stack.isEmpty()) return 0;
        int before = stack.getCount();
        ItemStack remainder = inventory.addItem(stack.copy());
        int moved = before - remainder.getCount();
        if (moved > 0) {
            stack.shrink(moved);
            if (stack.isEmpty()) source.setItem(sourceSlot, ItemStack.EMPTY);
            source.setChanged();
            inventory.setChanged();
        }
        return moved;
    }
}
