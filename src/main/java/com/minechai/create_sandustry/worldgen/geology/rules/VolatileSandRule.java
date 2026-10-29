package com.minechai.create_sandustry.worldgen.geology.rules;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.worldgen.geology.CandidateSampler;
import com.minechai.create_sandustry.worldgen.geology.GeologyConfigSnapshot;
import com.minechai.create_sandustry.worldgen.geology.GeologyConfigSnapshot.VolatileSandSettings;
import com.minechai.create_sandustry.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.worldgen.geology.GeologyRuleProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 规则 B：把已生成岩浆区域附近的普通沙子替换为烈性沙子。
 *
 * <p>该规则在 TOP_LAYER_MODIFICATION 阶段运行，此时原版岩浆湖、岩浆泉和洞穴装饰已经生成。
 * 热源检测仍限制在当前区块内，不请求邻区块。</p>
 */
public final class VolatileSandRule implements GeologyRuleProcessor {
    @Override
    public String id() {
        return "volatile_sand";
    }

    @Override
    public boolean isEnabled(GeologyConfigSnapshot config) {
        return config.enabled() && config.volatileSand().enabled();
    }

    @Override
    public boolean apply(GeologyGenerationContext context, GeologyConfigSnapshot config) {
        VolatileSandSettings settings = config.volatileSand();
        if (settings.targets().isEmpty() || settings.maxReplacements() == 0) {
            return false;
        }

        CandidateSampler sampler = new CandidateSampler(settings.maxReplacements());
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        outer:
        for (int y = context.minY(); y <= context.maxY(); y++) {
            for (int x = context.chunkMinX(); x <= context.chunkMaxX(); x++) {
                for (int z = context.chunkMinZ(); z <= context.chunkMaxZ(); z++) {
                    if (!context.consumeCheck()) {
                        break outer;
                    }

                    candidate.set(x, y, z);
                    BlockState state = context.level().getBlockState(candidate);
                    if (!settings.targets().contains(state.getBlock())) {
                        continue;
                    }

                    if (hasEnoughNearbyHeat(context, candidate, settings, probe)) {
                        sampler.offer(candidate, context.random());
                    }
                }
            }
        }

        boolean changed = false;
        for (BlockPos pos : sampler.selected()) {
            changed |= context.replaceIfAllowed(pos, settings.targets(), ModBlocks.VOLATILE_SAND.get());
        }
        return changed;
    }

    private static boolean hasEnoughNearbyHeat(
            GeologyGenerationContext context,
            BlockPos.MutableBlockPos candidate,
            VolatileSandSettings settings,
            BlockPos.MutableBlockPos probe) {
        int horizontalRadius = settings.horizontalRadius();
        int horizontalRadiusSquared = horizontalRadius * horizontalRadius;
        int verticalRadius = settings.verticalRadius();
        int requiredHeatBlocks = settings.minimumHeatBlocks();
        int found = 0;

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                if (dx * dx + dz * dz > horizontalRadiusSquared) {
                    continue;
                }

                int x = candidate.getX() + dx;
                int z = candidate.getZ() + dz;
                if (x < context.chunkMinX() || x > context.chunkMaxX()
                        || z < context.chunkMinZ() || z > context.chunkMaxZ()) {
                    continue;
                }

                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                    int y = candidate.getY() + dy;
                    if (y < context.minY() || y > context.maxY()) {
                        continue;
                    }
                    if (!context.consumeCheck()) {
                        return false;
                    }

                    probe.set(x, y, z);
                    BlockState state = context.level().getBlockState(probe);
                    if (state.getFluidState().is(FluidTags.LAVA)
                            || (settings.includeMagmaBlocks() && state.is(Blocks.MAGMA_BLOCK))) {
                        found++;
                        if (found >= requiredHeatBlocks) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
