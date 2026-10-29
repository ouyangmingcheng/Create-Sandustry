package com.minechai.create_sandustry.worldgen.geology.special;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * 当前区块 3×3 世界生成缓存内的结构快照。
 *
 * <p>只查询 {@link WorldGenRegion#hasChunk(int, int)} 已存在的区块，
 * 不会通过 StructureManager 请求或生成邻区块。</p>
 */
public record NearbyStructureStarts(
        List<BoundingBox> allFootprints,
        List<BoundingBox> targetFootprints) {

    public static NearbyStructureStarts find(
            WorldGenLevel level,
            ChunkPos center,
            Set<ResourceLocation> targetStructures) {
        return find(level, center, targetStructures, 1);
    }

    public static NearbyStructureStarts findInCurrentChunk(WorldGenLevel level, ChunkPos center) {
        return find(level, center, Set.of(), 0);
    }

    private static NearbyStructureStarts find(
            WorldGenLevel level,
            ChunkPos center,
            Set<ResourceLocation> targetStructures,
            int chunkRadius) {
        if (!(level instanceof WorldGenRegion region)) {
            return new NearbyStructureStarts(List.of(), List.of());
        }

        StructureManager structureManager = level.getLevel().structureManager().forWorldGenRegion(region);
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Set<StructureStart> starts = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int chunkX = center.x + dx;
                int chunkZ = center.z + dz;
                if (!region.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                starts.addAll(structureManager.startsForStructure(new ChunkPos(chunkX, chunkZ),
                        structure -> structure.step().ordinal() <= GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal()));
            }
        }

        List<BoundingBox> allFootprints = new ArrayList<>();
        List<BoundingBox> targetFootprints = new ArrayList<>();
        for (StructureStart start : starts) {
            if (!start.isValid()) {
                continue;
            }
            BoundingBox box = start.getBoundingBox();
            allFootprints.add(box);
            ResourceLocation id = registry.getKey(start.getStructure());
            if (id != null && targetStructures.contains(id)) {
                targetFootprints.add(box);
            }
        }
        return new NearbyStructureStarts(List.copyOf(allFootprints), List.copyOf(targetFootprints));
    }

    public boolean hasTargets() {
        return !targetFootprints.isEmpty();
    }

    public boolean isOutsideAllFootprints(BlockPos pos) {
        return isOutsideAllFootprints(pos.getX(), pos.getZ());
    }

    public boolean isOutsideAllFootprints(int x, int z) {
        for (BoundingBox box : allFootprints) {
            if (containsHorizontally(box, x, z)) {
                return false;
            }
        }
        return true;
    }

    public boolean isWithinTargetRadius(int x, int z, int radius) {
        if (!isOutsideAllFootprints(x, z)) {
            return false;
        }
        long radiusSquared = (long) radius * radius;
        for (BoundingBox box : targetFootprints) {
            if (horizontalDistanceSquared(box, x, z) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsHorizontally(BoundingBox box, int x, int z) {
        return x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();
    }

    private static long horizontalDistanceSquared(BoundingBox box, int x, int z) {
        long dx = 0L;
        if (x < box.minX()) {
            dx = box.minX() - x;
        } else if (x > box.maxX()) {
            dx = x - box.maxX();
        }

        long dz = 0L;
        if (z < box.minZ()) {
            dz = box.minZ() - z;
        } else if (z > box.maxZ()) {
            dz = z - box.maxZ();
        }
        return dx * dx + dz * dz;
    }
}
