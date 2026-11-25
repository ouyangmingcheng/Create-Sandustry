package com.minechai.create_sandustry.content.worldgen.geology.special;

import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.content.worldgen.geology.special.SpecialClusterConfigSnapshot.StructureClusterSettings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 结构外围的普通沙子团簇生成。
 */
final class StructureClusterSupport {
    private StructureClusterSupport() {
    }

    static boolean generate(
            GeologyGenerationContext context,
            StructureClusterSettings settings,
            Block replacement) {
        if (settings.targets().isEmpty() || settings.structures().isEmpty()
                || settings.maxClustersPerChunk() == 0) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(context.chunkMinX() >> 4, context.chunkMinZ() >> 4);
        NearbyStructureStarts structures = NearbyStructureStarts.find(
                context.level(), chunkPos, settings.structures());
        if (!structures.hasTargets()) {
            return false;
        }

        List<BlockPos> candidateColumns = collectCandidateColumns(
                context, structures, settings.structureRadius(), settings.includeTargetRoads());
        if (candidateColumns.isEmpty()) {
            return false;
        }

        int generated = 0;
        boolean changed = false;
        for (int attempt = 0;
                attempt < settings.attemptsPerChunk() && generated < settings.maxClustersPerChunk();
                attempt++) {
            if (context.random().nextDouble() >= settings.spawnChance()) {
                continue;
            }

            BlockPos column = candidateColumns.get(context.random().nextInt(candidateColumns.size()));
            BlockPos anchor = findAnchor(context, structures, column.getX(), column.getZ(), settings);
            if (anchor == null) {
                continue;
            }

            if (placeCluster(context, structures, anchor, settings, replacement,
                    pos -> structures.isWithinTargetRadius(
                            pos.getX(), pos.getZ(), settings.structureRadius(), settings.includeTargetRoads()))) {
                generated++;
                changed = true;
            }
        }
        return changed;
    }

    private static List<BlockPos> collectCandidateColumns(
            GeologyGenerationContext context,
            NearbyStructureStarts structures,
            int radius,
            boolean includeTargetRoads) {
        List<BlockPos> columns = new ArrayList<>();
        for (int x = context.chunkMinX(); x <= context.chunkMaxX(); x++) {
            for (int z = context.chunkMinZ(); z <= context.chunkMaxZ(); z++) {
                if (structures.isWithinTargetRadius(x, z, radius, includeTargetRoads)) {
                    columns.add(new BlockPos(x, context.minY(), z));
                }
            }
        }
        return columns;
    }

    private static BlockPos findAnchor(
            GeologyGenerationContext context,
            NearbyStructureStarts structures,
            int x,
            int z,
            StructureClusterSettings settings) {
        int surfaceY = context.surfaceY(x, z);
        int topY = Math.min(surfaceY, settings.maxY());
        int bottomY = Math.max(settings.minY(), surfaceY - settings.surfaceScanDepth() + 1);
        if (topY < bottomY) {
            return null;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = topY; y >= bottomY; y--) {
            if (!context.consumeCheck()) {
                return null;
            }
            cursor.set(x, y, z);
            BlockState state = context.level().getBlockState(cursor);
            if (targetsAt(structures, settings, x, z).contains(state.getBlock())) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static boolean placeCluster(
            GeologyGenerationContext context,
            NearbyStructureStarts structures,
            BlockPos anchor,
            StructureClusterSettings settings,
            Block replacement,
            StructurePositionGuard guard) {
        List<BlockPos> frontier = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        int maxSteps = settings.clusterSize() * 8;
        int placed = 0;

        if (tryPlace(context, structures, anchor, settings, replacement, guard)) {
            frontier.add(anchor);
            visited.add(anchor);
            placed++;
        }

        for (int step = 0; step < maxSteps && placed < settings.clusterSize() && !frontier.isEmpty(); step++) {
            if (!context.consumeCheck()) {
                break;
            }

            BlockPos base = frontier.get(context.random().nextInt(frontier.size()));
            int yOffset = context.random().nextInt(5) == 0 ? context.random().nextInt(3) - 1 : 0;
            BlockPos candidate = base.offset(
                    context.random().nextInt(3) - 1,
                    yOffset,
                    context.random().nextInt(3) - 1);
            if (candidate.getY() < settings.minY() || candidate.getY() > settings.maxY()) {
                continue;
            }
            if (!visited.add(candidate.immutable())) {
                continue;
            }

            if (tryPlace(context, structures, candidate, settings, replacement, guard)) {
                frontier.add(candidate.immutable());
                placed++;
            }
        }
        return placed > 0;
    }

    private static boolean tryPlace(
            GeologyGenerationContext context,
            NearbyStructureStarts structures,
            BlockPos pos,
            StructureClusterSettings settings,
            Block replacement,
            StructurePositionGuard guard) {
        if (!context.contains(pos.getX(), pos.getY(), pos.getZ())
                || pos.getY() < settings.minY()
                || pos.getY() > settings.maxY()
                || !guard.test(pos)) {
            return false;
        }
        return context.replaceIfAllowed(pos, targetsAt(structures, settings, pos.getX(), pos.getZ()), replacement);
    }

    private static Set<Block> targetsAt(
            NearbyStructureStarts structures,
            StructureClusterSettings settings,
            int x,
            int z) {
        return settings.includeTargetRoads() && structures.isInsideTargetRoad(x, z)
                ? settings.roadTargets()
                : settings.targets();
    }

    @FunctionalInterface
    interface StructurePositionGuard {
        boolean test(BlockPos pos);
    }
}
