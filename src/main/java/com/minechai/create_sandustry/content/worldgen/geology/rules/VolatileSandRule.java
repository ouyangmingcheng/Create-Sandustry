package com.minechai.create_sandustry.content.worldgen.geology.rules;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.CandidateSampler;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyConfigSnapshot;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyConfigSnapshot.VolatileSandSettings;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyRuleProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 规则 B：天然岩浆区域附近的普通沙、红沙和硬化沙转换为烈性沙。
 *
 * <p>硬化规则先运行，因此地表岩浆湖周围的石头已经变成硬化沙；本规则随后按
 * 岩浆热源辐射范围转换它们。紧邻岩浆的屏障方块仍使用六邻接直接识别。</p>
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

        VolatileHeatMap heatMap = replaceSand
                ? VolatileHeatMap.build(context, settings.includeMagmaBlocks())
                : null;
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
                    if (canReplaceAsSand && heatMap != null
                            && heatMap.hasEnoughHeat(context, candidate, settings)) {
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

    /**
     * 当前区块的三维热源前缀表。构造时只读取当前区块一次，
     * 后续每个候选方块的热源数量查询为 O(1)，避免大量硬化沙候选耗尽扫描预算。
     */
    private static final class VolatileHeatMap {
        private static final int SIZE_X = 16;
        private static final int SIZE_Z = 16;

        private final int sizeY;
        private final int[] prefix;
        private final int minY;

        private VolatileHeatMap(int sizeY, int[] prefix, int minY) {
            this.sizeY = sizeY;
            this.prefix = prefix;
            this.minY = minY;
        }

        static VolatileHeatMap build(GeologyGenerationContext context, boolean includeMagmaBlocks) {
            int sizeY = context.maxY() - context.minY() + 1;
            int strideY = SIZE_Z + 1;
            int strideX = (sizeY + 1) * strideY;
            int[] prefix = new int[(SIZE_X + 1) * strideX];
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < SIZE_X; x++) {
                    for (int z = 0; z < SIZE_Z; z++) {
                        if (!context.consumeCheck()) {
                            return null;
                        }

                        cursor.set(context.chunkMinX() + x, context.minY() + y, context.chunkMinZ() + z);
                        BlockState state = context.level().getBlockState(cursor);
                        if (state.getFluidState().is(FluidTags.LAVA)
                                || (includeMagmaBlocks && state.is(Blocks.MAGMA_BLOCK))) {
                            prefix[index(x + 1, y + 1, z + 1, strideX, strideY)] = 1;
                        }
                    }
                }
            }

            for (int x = 1; x <= SIZE_X; x++) {
                for (int y = 1; y <= sizeY; y++) {
                    for (int z = 1; z <= SIZE_Z; z++) {
                        int value = prefix[index(x, y, z, strideX, strideY)]
                                + prefix[index(x - 1, y, z, strideX, strideY)]
                                + prefix[index(x, y - 1, z, strideX, strideY)]
                                + prefix[index(x, y, z - 1, strideX, strideY)]
                                - prefix[index(x - 1, y - 1, z, strideX, strideY)]
                                - prefix[index(x - 1, y, z - 1, strideX, strideY)]
                                - prefix[index(x, y - 1, z - 1, strideX, strideY)]
                                + prefix[index(x - 1, y - 1, z - 1, strideX, strideY)];
                        prefix[index(x, y, z, strideX, strideY)] = value;
                    }
                }
            }
            return new VolatileHeatMap(sizeY, prefix, context.minY());
        }

        boolean hasEnoughHeat(
                GeologyGenerationContext context,
                BlockPos candidate,
                VolatileSandSettings settings) {
            int minX = Math.max(context.chunkMinX(), candidate.getX() - settings.horizontalRadius());
            int maxX = Math.min(context.chunkMaxX(), candidate.getX() + settings.horizontalRadius());
            int minZ = Math.max(context.chunkMinZ(), candidate.getZ() - settings.horizontalRadius());
            int maxZ = Math.min(context.chunkMaxZ(), candidate.getZ() + settings.horizontalRadius());
            int minWorldY = Math.max(context.minY(), candidate.getY() - settings.verticalRadius());
            int maxWorldY = Math.min(context.maxY(), candidate.getY() + settings.verticalRadius());

            int x1 = minX - context.chunkMinX();
            int x2 = maxX - context.chunkMinX();
            int z1 = minZ - context.chunkMinZ();
            int z2 = maxZ - context.chunkMinZ();
            int y1 = minWorldY - minY;
            int y2 = maxWorldY - minY;
            int strideY = SIZE_Z + 1;
            int strideX = (sizeY + 1) * strideY;

            int count = boxSum(x2 + 1, y2 + 1, z2 + 1, strideX, strideY)
                    - boxSum(x1, y2 + 1, z2 + 1, strideX, strideY)
                    - boxSum(x2 + 1, y1, z2 + 1, strideX, strideY)
                    - boxSum(x2 + 1, y2 + 1, z1, strideX, strideY)
                    + boxSum(x1, y1, z2 + 1, strideX, strideY)
                    + boxSum(x1, y2 + 1, z1, strideX, strideY)
                    + boxSum(x2 + 1, y1, z1, strideX, strideY)
                    - boxSum(x1, y1, z1, strideX, strideY);
            return count >= settings.minimumHeatBlocks();
        }

        private int boxSum(int x, int y, int z, int strideX, int strideY) {
            return prefix[index(x, y, z, strideX, strideY)];
        }

        private static int index(int x, int y, int z, int strideX, int strideY) {
            return x * strideX + y * strideY + z;
        }
    }
}
