package dev.aiplayer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Uses the world's recipe manager, so vanilla and mod-registered crafting
 * recipes are handled by the same path.
 */
public final class BlockyCrafting {
    private BlockyCrafting() {}

    public static String craftOne(SimpleContainer inventory, ServerLevel level) {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            grid.add(inventory.getItem(slot).copy());
        }

        CraftingInput input = CraftingInput.of(3, 3, grid);
        Optional<RecipeHolder<?>> match = level.recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (match.isEmpty()) {
            return "Не знайшов рецепта у перших 9 слотах інвентарю. Поклади інгредієнти у верхні 9 слотів і повтори.";
        }

        var recipe = match.get().value();
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) return "Рецепт знайдено, але він не створив предмет.";

        // Compute all remainders before mutating the inventory.
        List<ItemStack> remainders = recipe.getRemainingItems(input);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack ingredient = inventory.getItem(slot);
            if (!ingredient.isEmpty()) {
                ingredient.shrink(1);
                if (ingredient.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
            }
            ItemStack remainder = slot < remainders.size() ? remainders.get(slot) : ItemStack.EMPTY;
            if (!remainder.isEmpty()) {
                ItemStack leftover = inventory.addItem(remainder.copy());
                if (!leftover.isEmpty()) {
                    // Do not lose container items when the inventory is full.
                    return "Місця не вистачило для предмета, що повертається рецептом. Звільни інвентар і повтори.";
                }
            }
        }

        ItemStack overflow = inventory.addItem(result.copy());
        if (!overflow.isEmpty()) {
            // Keep the crafted result safe if there is no room.
            return "Рецепт готовий, але немає місця для результату. Звільни інвентар і повтори.";
        }

        String namespace = result.getItemHolder().unwrapKey()
                .map(key -> key.identifier().getNamespace()).orElse("unknown");
        return "Готово: " + result.getCount() + "x " + result.getHoverName().getString()
                + " (namespace: " + namespace + ").";
    }
}
