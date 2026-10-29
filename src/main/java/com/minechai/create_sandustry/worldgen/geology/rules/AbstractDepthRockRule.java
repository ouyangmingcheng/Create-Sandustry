package com.minechai.create_sandustry.worldgen.geology.rules;

import com.minechai.create_sandustry.config.GeologyConfig.HeightMode;
import com.minechai.create_sandustry.worldgen.geology.GeologyConfigSnapshot.DepthRockSettings;
import com.minechai.create_sandustry.worldgen.geology.GeologyGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 硬化沙子和石化沙子共用的高度范围/精确白名单替换逻辑。
 */
abstract class AbstractDepthRockRule {
    protected boolean applyDepthRange(
            GeologyGenerationContext context,
            DepthRockSettings settings,
            Block replacement) {
        if (settings.targets().isEmpty() || settings.maxReplacements() == 0) {
            return false;
        }

        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();
        int replaced = 0;
        boolean changed = false;

        outer:
        for (int x = context.chunkMinX(); x <= context.chunkMaxX(); x++) {
            for (int z = context.chunkMinZ(); z <= context.chunkMaxZ(); z++) {
                int minY = minY(context, settings, x, z);
                int maxY = maxY(context, settings, x, z);
                for (int y = minY; y <= maxY; y++) {
                    if (replaced >= settings.maxReplacements() || !context.consumeCheck()) {
                        break outer;
                    }

                    candidate.set(x, y, z);
                    BlockState state = context.level().getBlockState(candidate);
                    if (!settings.targets().contains(state.getBlock())) {
                        continue;
                    }

                    if (context.replaceIfAllowed(candidate, settings.targets(), replacement)) {
                        replaced++;
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    private static int minY(
            GeologyGenerationContext context,
            DepthRockSettings settings,
            int x,
            int z) {
        int minY;
        if (settings.heightMode() == HeightMode.FIXED) {
            minY = Math.min(settings.fixedMinY(), settings.fixedMaxY());
        } else {
            int maxDepth = Math.max(settings.relativeMinDepth(), settings.relativeMaxDepth());
            minY = context.surfaceY(x, z) - maxDepth;
        }
        return context.clampY(Math.max(minY, context.minY()));
    }

    private static int maxY(
            GeologyGenerationContext context,
            DepthRockSettings settings,
            int x,
            int z) {
        int maxY;
        if (settings.heightMode() == HeightMode.FIXED) {
            maxY = Math.max(settings.fixedMinY(), settings.fixedMaxY());
        } else {
            int minDepth = Math.min(settings.relativeMinDepth(), settings.relativeMaxDepth());
            maxY = context.surfaceY(x, z) - minDepth;
        }
        return context.clampY(Math.min(maxY, context.maxY()));
    }
}
