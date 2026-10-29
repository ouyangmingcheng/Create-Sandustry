package com.minechai.create_sandustry.worldgen.geology.rules;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.worldgen.geology.CandidateSampler;
import com.minechai.create_sandustry.worldgen.geology.GeologyConfigSnapshot;
import com.minechai.create_sandustry.worldgen.geology.GeologyConfigSnapshot.VolatileSandSettings;
import com.minechai.create_sandustry.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.worldgen.geology.GeologyRuleProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 规则 B：把天然岩浆区域附近的普通沙子，以及原版岩浆湖 LakeFeature 生成的紧邻岩浆石头，
 * 替换为烈性沙子。
 *
 * <p>该规则在 VEGETAL_DECORATION 阶段运行，此时岩浆湖、岩浆泉和洞穴装饰已经生成。
 * 石头屏障只按六个正方向与岩浆相邻来识别；普通地下石头不会因为邻近洞穴空气而转换。</p>
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
        boolean replaceSand = !settings.targets().isEmpty() && settings.maxReplacements() > 0;
        boolean replaceStone = settings.replaceLavaLakeStone()
                && !settings.lavaLakeStoneTargets().isEmpty()
                && settings.lavaLakeStoneMaxReplacements() > 0;
        if (!replaceSand && !replaceStone) {
            return false;
        }

        CandidateSampler sandSampler = new CandidateSampler(replaceSand ? settings.maxReplacements() : 0);
        CandidateSampler stoneSampler = new CandidateSampler(
                replaceStone ? settings.lavaLakeStoneMaxReplacements() : 0);
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
                    boolean canReplaceAsSand = replaceSand && settings.targets().contains(state.getBlock());
                    boolean canReplaceAsStone = replaceStone
                            && settings.lavaLakeStoneTargets().contains(state.getBlock());
                    if (!canReplaceAsSand && !canReplaceAsStone) {
                        continue;
                    }

                    if (canReplaceAsStone && touchesLava(context, candidate, probe)) {
                        stoneSampler.offer(candidate, context.random());
                    }
                    if (canReplaceAsSand && hasEnoughNearbyHeat(context, candidate, settings, probe)) {
                        sandSampler.offer(candidate, context.random());
                    }
                }
            }
        }

        boolean changed = false;
        for (BlockPos pos : stoneSampler.selected()) {
            changed |= context.replaceIfAllowed(
                    pos, settings.lavaLakeStoneTargets(), ModBlocks.VOLATILE_SAND.get());
        }
        for (BlockPos pos : sandSampler.selected()) {
            changed |= context.replaceIfAllowed(pos, settings.targets(), ModBlocks.VOLATILE_SAND.get());
        }
        return changed;
    }

    /**
     * LakeFeature 用六邻接判定构建屏障，因此同样只检查六个正方向。
     * 邻区块只在已存在于当前 WorldGenRegion 缓存时读取，不触发生成或加载。
     */
    static boolean touchesLava(
            GeologyGenerationContext context,
            BlockPos candidate,
            BlockPos.MutableBlockPos probe) {
        for (Direction direction : Direction.values()) {
            probe.setWithOffset(candidate, direction);
            if (!context.canRead(probe)) {
                continue;
            }
            if (!context.consumeCheck()) {
                return false;
            }
            if (context.level().getBlockState(probe).getFluidState().is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
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
