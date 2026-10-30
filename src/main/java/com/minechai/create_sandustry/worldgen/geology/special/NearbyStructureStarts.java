package com.minechai.create_sandustry.worldgen.geology.special;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * 当前区块 3×3 世界生成缓存内的结构快照。
 *
 * <p>不使用 {@code StructureManager#startsForStructure}，因为该方法会通过
 * {@code fillStartsForStructure} 请求 structure reference 指向的远端起点区块；
 * 该区块可能不在 {@link WorldGenRegion} 的 3×3 缓存内并导致世界生成崩溃。</p>
 *
 * <p>这里只读取 {@link WorldGenRegion#hasChunk(int, int)} 已存在的区块，
 * 并且只解析起点区块同样位于该缓存内的 references。远端结构本次区块不参与生成，
 * 不会强制加载或间接生成任何区块。</p>
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

        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Set<StructureStart> starts = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int chunkX = center.x + dx;
                int chunkZ = center.z + dz;
                if (!region.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                // EMPTY 只请求缓存内区块对象，不触发结构起点区块的额外加载。
                ChunkAccess localChunk = region.getChunk(chunkX, chunkZ);
                for (StructureStart start : localChunk.getAllStarts().values()) {
                    addIfEligible(starts, start);
                }

                for (Map.Entry<Structure, LongSet> entry : localChunk.getAllReferences().entrySet()) {
                    Structure structure = entry.getKey();
                    if (!isEligible(structure)) {
                        continue;
                    }

                    for (long packedOrigin : entry.getValue()) {
                        ChunkPos origin = new ChunkPos(packedOrigin);
                        if (!region.hasChunk(origin.x, origin.z)) {
                            // 远端起点区块不可访问；绝不让 StructureManager 递归取块。
                            continue;
                        }

                        StructureStart start = region.getChunk(origin.x, origin.z)
                                .getStartForStructure(structure);
                        if (start != null) {
                            starts.add(start);
                        }
                    }
                }
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

    private static void addIfEligible(Set<StructureStart> starts, StructureStart start) {
        if (start.isValid() && isEligible(start.getStructure())) {
            starts.add(start);
        }
    }

    private static boolean isEligible(Structure structure) {
        return structure.step().ordinal() <= GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
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
