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
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * 当前区块 3×3 世界生成缓存内的结构快照。
 *
 * <p>不调用 StructureManager#startsForStructure，避免 fillStartsForStructure
 * 请求远端 structure reference 指向的区块。这里只解析起点区块也在缓存内的引用。</p>
 *
 * <p>结构 Piece 被拆分为普通保护盒和目标道路盒。道路通过真实的
 * {@link StructureTemplatePool.Projection#TERRAIN_MATCHING} 投影识别；
 * 建筑、地基和其他 Piece 仍全部受保护。</p>
 */
public record NearbyStructureStarts(
        List<BoundingBox> protectedFootprints,
        List<BoundingBox> targetFootprints,
        List<BoundingBox> targetRoadFootprints) {

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
            return empty();
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

        List<BoundingBox> protectedFootprints = new ArrayList<>();
        List<BoundingBox> targetFootprints = new ArrayList<>();
        List<BoundingBox> targetRoadFootprints = new ArrayList<>();
        for (StructureStart start : starts) {
            if (!start.isValid()) {
                continue;
            }

            ResourceLocation id = registry.getKey(start.getStructure());
            boolean target = id != null && targetStructures.contains(id);
            for (StructurePiece piece : start.getPieces()) {
                BoundingBox box = piece.getBoundingBox();
                boolean road = target && isRoadPiece(piece);
                if (road) {
                    targetRoadFootprints.add(box);
                } else {
                    protectedFootprints.add(box);
                }
                if (target) {
                    targetFootprints.add(box);
                }
            }
        }

        return new NearbyStructureStarts(
                List.copyOf(protectedFootprints),
                List.copyOf(targetFootprints),
                List.copyOf(targetRoadFootprints));
    }

    private static NearbyStructureStarts empty() {
        return new NearbyStructureStarts(List.of(), List.of(), List.of());
    }

    private static void addIfEligible(Set<StructureStart> starts, StructureStart start) {
        if (start.isValid() && isEligible(start.getStructure())) {
            starts.add(start);
        }
    }

    private static boolean isEligible(Structure structure) {
        return structure.step().ordinal() <= GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
    }

    private static boolean isRoadPiece(StructurePiece piece) {
        return piece instanceof PoolElementStructurePiece poolPiece
                && poolPiece.getElement().getProjection() == StructureTemplatePool.Projection.TERRAIN_MATCHING;
    }

    public boolean hasTargets() {
        return !targetFootprints.isEmpty();
    }

    public boolean isOutsideAllFootprints(BlockPos pos) {
        return isOutsideAllFootprints(pos.getX(), pos.getZ());
    }

    public boolean isOutsideAllFootprints(int x, int z) {
        for (BoundingBox box : protectedFootprints) {
            if (containsHorizontally(box, x, z)) {
                return false;
            }
        }
        return true;
    }

    public boolean isInsideTargetRoad(int x, int z) {
        for (BoundingBox box : targetRoadFootprints) {
            if (containsHorizontally(box, x, z)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWithinTargetRadius(int x, int z, int radius) {
        return isWithinTargetRadius(x, z, radius, false);
    }

    public boolean isWithinTargetRadius(int x, int z, int radius, boolean includeTargetRoads) {
        if (!isOutsideAllFootprints(x, z)) {
            return false;
        }
        if (includeTargetRoads && isInsideTargetRoad(x, z)) {
            return true;
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
