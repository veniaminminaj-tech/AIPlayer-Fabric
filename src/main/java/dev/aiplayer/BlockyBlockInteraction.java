package dev.aiplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;

/**
 * Moves items between Blocky's inventory and nearby vanilla or modded block entities
 * that expose the standard Minecraft Container interface.
 */
public final class BlockyBlockInteraction {
    private static final int SEARCH_RADIUS = 5;

    private BlockyBlockInteraction() {}

    public static String interactWithNearestContainer(SimpleContainer inventory,
                                                       ServerLevel level,
                                                       BlockPos origin) {
        BlockEntity target = BlockPos.betweenClosedStream(
                        origin.offset(-SEARCH_RADIUS, -2, -SEARCH_RADIUS),
                        origin.offset(SEARCH_RADIUS, 2, SEARCH_RADIUS))
                .map(pos -> level.getBlockEntity(pos))
                .filter(entity -> entity instanceof Container)
                .min(Comparator.comparingDouble(entity ->
                        entity.getBlockPos().distToCenterSqr(origin.getX() + 0.5,
                                origin.getY() + 0.5, origin.getZ() + 0.5)))
                .orElse(null);

        if (target == null) {
            return "Поруч не знайшов пічку, бочку чи інший блок-контейнер.";
        }

        Container container = (Container) target;
        int movedIn = 0;
        int movedOut = 0;

        if (target instanceof AbstractFurnaceBlockEntity) {
            // Furnace-like blocks use the conventional input/fuel/output slots.
            movedOut += moveSlotToInventory(container, 2, inventory);
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()) continue;
                int destination = stack.is(ItemTags.FURNACE_FUELS) ? 1 : 0;
                movedIn += moveInventoryStack(inventory, slot, container, destination);
            }
        } else {
            // Barrels, chests and modded containers implementing Container.
            for (int source = 0; source < inventory.getContainerSize(); source++) {
                ItemStack stack = inventory.getItem(source);
                if (stack.isEmpty()) continue;
                for (int destination = 0; destination < container.getContainerSize()
                        && !stack.isEmpty(); destination++) {
                    movedIn += insertIntoSlot(inventory, source, container, destination);
                }
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                movedOut += moveSlotToInventory(container, slot, inventory);
            }
        }

        container.setChanged();
        if (movedIn == 0 && movedOut == 0) {
            return "Знайшов блок-контейнер, але зараз нічого не вдалося перемістити.";
        }
        return "Готово. Перемістив у блок: " + movedIn
                + " предметів, забрав до інвентаря: " + movedOut + ".";
    }

    private static int moveInventoryStack(SimpleContainer source, int sourceSlot,
                                          Container destination, int destinationSlot) {
        return insertIntoSlot(source, sourceSlot, destination, destinationSlot);
    }

    private static int insertIntoSlot(SimpleContainer source, int sourceSlot,
                                      Container destination, int destinationSlot) {
        ItemStack sourceStack = source.getItem(sourceSlot);
        if (sourceStack.isEmpty() || !destination.canPlaceItem(destinationSlot, sourceStack)) return 0;

        ItemStack targetStack = destination.getItem(destinationSlot);
        if (!targetStack.isEmpty()
                && !ItemStack.isSameItemSameComponents(targetStack, sourceStack)) return 0;

        int capacity = targetStack.isEmpty()
                ? Math.min(sourceStack.getMaxStackSize(), destination.getMaxStackSize())
                : Math.min(targetStack.getMaxStackSize(), destination.getMaxStackSize()) - targetStack.getCount();
        int amount = Math.min(sourceStack.getCount(), Math.max(0, capacity));
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
        destination.setChanged();
        source.setChanged();
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
