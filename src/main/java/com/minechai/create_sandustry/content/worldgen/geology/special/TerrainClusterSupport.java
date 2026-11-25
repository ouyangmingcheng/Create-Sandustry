package com.minechai.create_sandustry.content.worldgen.geology.special;

import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.content.worldgen.geology.special.SpecialClusterConfigSnapshot.TerrainClusterSettings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 不依赖结构的普通沙/沙砾团簇生成。
 */
final class TerrainClusterSupport {
    private TerrainClusterSupport() {
    }

    static boolean generate(
            GeologyGenerationContext context,
            TerrainClusterSettings settings,
            Block replacement) {
        if (settings.targets().isEmpty() || settings.maxClustersPerChunk() == 0 || settings.biomes().isEmpty()) {
            return false;
        }
        if (!mayContainAllowedBiome(context, settings.biomes())) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(context.chunkMinX() >> 4, context.chunkMinZ() >> 4);
        NearbyStructureStarts structures = NearbyStructureStarts.findInCurrentChunk(context.level(), chunkPos);

        int generated = 0;
        boolean changed = false;
        for (int attempt = 0;
                attempt < settings.attemptsPerChunk() && generated < settings.maxClustersPerChunk();
                attempt++) {
            if (context.random().nextDouble() >= settings.spawnChance()) {
                continue;
            }

            BlockPos anchor = findAnchor(context, settings);
            if (anchor == null
                    || !structures.isOutsideAllFootprints(anchor)
                    || !isBiomeAllowed(context, anchor, settings.biomes())) {
                continue;
            }

            if (placeCluster(context, anchor, settings, replacement, structures::isOutsideAllFootprints)) {
                generated++;
                changed = true;
            }
        }
        return changed;
    }

    private static BlockPos findAnchor(GeologyGenerationContext context, TerrainClusterSettings settings) {
        int x = context.chunkMinX() + context.random().nextInt(16);
        int z = context.chunkMinZ() + context.random().nextInt(16);
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
            if (settings.targets().contains(state.getBlock())) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static boolean placeCluster(
            GeologyGenerationContext context,
            BlockPos anchor,
            TerrainClusterSettings settings,
            Block replacement,
            PositionGuard guard) {
        List<BlockPos> frontier = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        int maxSteps = settings.clusterSize() * 8;
        int placed = 0;

        if (tryPlace(context, anchor, settings, replacement, guard)) {
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

            if (tryPlace(context, candidate, settings, replacement, guard)) {
                frontier.add(candidate.immutable());
                placed++;
            }
        }
        return placed > 0;
    }

    private static boolean tryPlace(
            GeologyGenerationContext context,
            BlockPos pos,
            TerrainClusterSettings settings,
            Block replacement,
            PositionGuard guard) {
        if (!context.contains(pos.getX(), pos.getY(), pos.getZ())
                || pos.getY() < settings.minY()
                || pos.getY() > settings.maxY()
                || !guard.test(pos)
                || !isBiomeAllowed(context, pos, settings.biomes())) {
            return false;
        }
        return context.replaceIfAllowedWithLoot(pos, settings.targets(), replacement, settings.lootTable());
    }

    private static boolean mayContainAllowedBiome(
            GeologyGenerationContext context,
            Set<ResourceLocation> allowedBiomes) {
        int[] xs = {context.chunkMinX(), context.chunkMaxX(), context.chunkMinX(), context.chunkMaxX()};
        int[] zs = {context.chunkMinZ(), context.chunkMinZ(), context.chunkMaxZ(), context.chunkMaxZ()};
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            int z = zs[i];
            if (isBiomeAllowed(context, new BlockPos(x, context.surfaceY(x, z), z), allowedBiomes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBiomeAllowed(
            GeologyGenerationContext context,
            BlockPos pos,
            Set<ResourceLocation> allowedBiomes) {
        Registry<Biome> registry = context.level().registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> biome = context.level().getBiome(pos);
        ResourceLocation id = registry.getKey(biome.value());
        return id != null && allowedBiomes.contains(id);
    }

    @FunctionalInterface
    interface PositionGuard {
        boolean test(BlockPos pos);
    }
}
