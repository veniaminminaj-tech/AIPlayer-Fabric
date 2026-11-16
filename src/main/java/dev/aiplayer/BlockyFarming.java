package dev.aiplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;

/**
 * Generic crop harvesting for vanilla crops and mod crops that extend CropBlock.
 * No Farmer's Delight classes are linked at compile time.
 */
public final class BlockyFarming {
    private static final int SEARCH_RADIUS = 8;

    private BlockyFarming() {}

    public static String harvestNearestCrop(ServerLevel level, BlockPos origin) {
        BlockPos target = BlockPos.betweenClosedStream(
                        origin.offset(-SEARCH_RADIUS, -2, -SEARCH_RADIUS),
                        origin.offset(SEARCH_RADIUS, 3, SEARCH_RADIUS))
                .filter(pos -> {
                    BlockState state = level.getBlockState(pos);
                    return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
                })
                .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(
                        origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5)))
                .map(BlockPos::immutable)
                .orElse(null);

        if (target == null) {
            return "Поруч не знайшов стиглих культур типу CropBlock.";
        }

        BlockState cropState = level.getBlockState(target);
        if (!(cropState.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(cropState)) {
            return "Культура вже не готова до збору.";
        }

        // Normal block drops are spawned; Blocky's existing item-collection goal can pick them up.
        boolean harvested = level.destroyBlock(target, true);
        return harvested
                ? "Зібрав стиглу культуру. Тепер підберу предмети, що випали."
                : "Не вдалося зібрати культуру.";
    }
}
