package com.minechai.create_sandustry.content.worldgen.geology.rules;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.CandidateSampler;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyConfigSnapshot;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyConfigSnapshot.WetSandSettings;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyRuleProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 规则 A：把天然水体附近的普通沙子替换为湿沙子。
 */
public final class WetSandRule implements GeologyRuleProcessor {
    @Override
    public String id() {
        return "wet_sand";
    }

    @Override
    public boolean isEnabled(GeologyConfigSnapshot config) {
        return config.enabled() && config.wet().enabled();
    }

    @Override
    public boolean apply(GeologyGenerationContext context, GeologyConfigSnapshot config) {
        WetSandSettings settings = config.wet();
        if (settings.targets().isEmpty() || settings.maxReplacements() == 0) {
            return false;
        }

        CandidateSampler sampler = new CandidateSampler(settings.maxReplacements());
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        for (int x = context.chunkMinX(); x <= context.chunkMaxX(); x++) {
            for (int z = context.chunkMinZ(); z <= context.chunkMaxZ(); z++) {
                int surfaceY = context.surfaceY(x, z);
                int startY = Math.max(context.minY(), surfaceY - settings.surfaceScanDepth() + 1);
                for (int y = startY; y <= surfaceY; y++) {
                    if (!context.consumeCheck()) {
                        return replaceSelected(context, settings, sampler);
                    }

                    candidate.set(x, y, z);
                    BlockState state = context.level().getBlockState(candidate);
                    if (!isAllowedTarget(state, settings)) {
                        continue;
                    }

                    if (hasNearbyWater(context, candidate, settings, probe)) {
                        sampler.offer(candidate, context.random());
                    }
                }
            }
        }

        return replaceSelected(context, settings, sampler);
    }

    private static boolean hasNearbyWater(
            GeologyGenerationContext context,
            BlockPos.MutableBlockPos candidate,
            WetSandSettings settings,
            BlockPos.MutableBlockPos probe) {
        int horizontalRadius = settings.horizontalRadius();
        int horizontalRadiusSquared = horizontalRadius * horizontalRadius;
        int verticalRadius = settings.verticalRadius();

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
                    if (state.getFluidState().is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean replaceSelected(
            GeologyGenerationContext context,
            WetSandSettings settings,
            CandidateSampler sampler) {
        boolean changed = false;
        for (BlockPos pos : sampler.selected()) {
            BlockState current = context.level().getBlockState(pos);
            if (!isAllowedTarget(current, settings)) {
                continue;
            }
            changed |= context.replaceIfAllowed(pos, settings.targets(), ModBlocks.WET_SAND.get());
        }
        return changed;
    }

    private static boolean isAllowedTarget(BlockState state, WetSandSettings settings) {
        return settings.targets().contains(state.getBlock())
                && (settings.includeRedSand() || !state.is(Blocks.RED_SAND));
    }
}
