package dev.aiplayer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Uses the world's recipe manager for vanilla and mod-registered recipes. */
public final class BlockyCrafting {
    private BlockyCrafting() {}

    public static String craftOne(SimpleContainer inventory, ServerLevel level) {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            grid.add(inventory.getItem(slot).copy());
        }

        CraftingInput input = CraftingInput.of(3, 3, grid);
        Optional<RecipeHolder<CraftingRecipe>> match = level.recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (match.isEmpty()) {
            return "Не знайшов рецепта у перших 9 слотах. Розклади інгредієнти як у рецепті й повтори.";
        }

        CraftingRecipe recipe = match.get().value();
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) return "Рецепт знайдено, але він не створив предмет.";

        // Simulate the whole craft first, including recipe-specific remainders.
        // This prevents losing buckets, bottles, or other returned items.
        List<ItemStack> remainders = recipe.getRemainingItems(input);
        SimpleContainer preview = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            preview.setItem(slot, inventory.getItem(slot).copy());
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack ingredient = preview.getItem(slot);
            if (!ingredient.isEmpty()) {
                ingredient.shrink(1);
                if (ingredient.isEmpty()) preview.setItem(slot, ItemStack.EMPTY);
            }

            ItemStack remainder = remainders.get(slot);
            if (!remainder.isEmpty()) {
                ItemStack remainderCopy = remainder.copy();
                if (preview.getItem(slot).isEmpty()) {
                    preview.setItem(slot, remainderCopy);
                } else {
                    ItemStack overflow = preview.addItem(remainderCopy);
                    if (!overflow.isEmpty()) {
                        return "Немає місця для предметів, що залишаються після рецепта. Звільни інвентар і повтори.";
                    }
                }
            }
        }

        ItemStack overflow = preview.addItem(result.copy());
        if (!overflow.isEmpty()) return "Немає місця для результату. Звільни інвентар і повтори.";

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, preview.getItem(slot).copy());
        }
        return "Готово: " + result.getCount() + "x " + result.getHoverName().getString() + ".";
    }
}
