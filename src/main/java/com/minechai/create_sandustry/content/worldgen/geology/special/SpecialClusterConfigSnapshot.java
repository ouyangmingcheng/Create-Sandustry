package com.minechai.create_sandustry.content.worldgen.geology.special;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 特殊团簇设置快照。
 *
 * <p>除唯一总开关外，其余参数采用代码固定默认值。</p>
 */
public record SpecialClusterConfigSnapshot(
        boolean enabled,
        int maxBlockChecksPerFeature,
        TerrainClusterSettings suspiciousSand,
        TerrainClusterSettings suspiciousGravel,
        StructureClusterSettings catLitter,
        StructureClusterSettings livingSand) {

    private static final Set<ResourceLocation> SUSPICIOUS_SAND_BIOMES = Set.of(
            id("minecraft:desert"));
    private static final Set<ResourceLocation> SUSPICIOUS_GRAVEL_BIOMES = Set.of(
            id("minecraft:windswept_gravelly_hills"),
            id("minecraft:stony_shore"));
    private static final Set<ResourceLocation> VILLAGES = Set.of(
            id("minecraft:village_plains"),
            id("minecraft:village_desert"),
            id("minecraft:village_savanna"),
            id("minecraft:village_snowy"),
            id("minecraft:village_taiga"));
    private static final Set<ResourceLocation> LIVING_SAND_STRUCTURES = Set.of(
            id("minecraft:village_plains"),
            id("minecraft:village_desert"),
            id("minecraft:village_savanna"),
            id("minecraft:village_snowy"),
            id("minecraft:village_taiga"),
            id("minecraft:desert_pyramid"),
            id("minecraft:jungle_pyramid"),
            id("minecraft:pillager_outpost"),
            id("minecraft:mansion"),
            id("minecraft:swamp_hut"),
            id("minecraft:igloo"),
            id("minecraft:shipwreck"),
            id("minecraft:shipwreck_beached"),
            id("minecraft:trail_ruins"));

    private static volatile SpecialClusterConfigSnapshot cached;

    public static SpecialClusterConfigSnapshot current() {
        boolean enabled = ModConfigs.WORLDGEN_ENABLED.get();
        SpecialClusterConfigSnapshot snapshot = cached;
        if (snapshot != null && snapshot.enabled == enabled) {
            return snapshot;
        }

        synchronized (SpecialClusterConfigSnapshot.class) {
            snapshot = cached;
            if (snapshot != null && snapshot.enabled == enabled) {
                return snapshot;
            }
            snapshot = build(enabled);
            cached = snapshot;
            return snapshot;
        }
    }

    private static SpecialClusterConfigSnapshot build(boolean enabled) {
        TerrainClusterSettings suspiciousSand = new TerrainClusterSettings(
                enabled,
                SUSPICIOUS_SAND_BIOMES,
                Set.of(Blocks.SAND),
                10,
                0.55D,
                4,
                8,
                50,
                120,
                12,
                lootTable("minecraft:archaeology/desert_pyramid"));

        TerrainClusterSettings suspiciousGravel = new TerrainClusterSettings(
                enabled,
                SUSPICIOUS_GRAVEL_BIOMES,
                Set.of(Blocks.GRAVEL),
                5,
                0.30D,
                2,
                6,
                40,
                110,
                12,
                lootTable("minecraft:archaeology/trail_ruins_common"));

        StructureClusterSettings catLitter = new StructureClusterSettings(
                enabled,
                VILLAGES,
                Set.of(Blocks.SAND),
                Set.of(Blocks.SAND, Blocks.SMOOTH_SANDSTONE, Blocks.DIRT_PATH, Blocks.GRAVEL, Blocks.COARSE_DIRT),
                true,
                12,
                8,
                0.45D,
                2,
                5,
                50,
                100,
                12);

        StructureClusterSettings livingSand = new StructureClusterSettings(
                enabled,
                LIVING_SAND_STRUCTURES,
                Set.of(Blocks.SAND),
                Set.of(),
                false,
                16,
                4,
                0.06D,
                1,
                1,
                50,
                120,
                12);

        return new SpecialClusterConfigSnapshot(
                enabled,
                50000,
                suspiciousSand,
                suspiciousGravel,
                catLitter,
                livingSand);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    private static ResourceKey<LootTable> lootTable(String value) {
        return ResourceKey.create(Registries.LOOT_TABLE, id(value));
    }

    public record TerrainClusterSettings(
            boolean enabled,
            Set<ResourceLocation> biomes,
            Set<Block> targets,
            int attemptsPerChunk,
            double spawnChance,
            int maxClustersPerChunk,
            int clusterSize,
            int minY,
            int maxY,
            int surfaceScanDepth,
            ResourceKey<LootTable> lootTable) {
    }

    public record StructureClusterSettings(
            boolean enabled,
            Set<ResourceLocation> structures,
            Set<Block> targets,
            Set<Block> roadTargets,
            boolean includeTargetRoads,
            int structureRadius,
            int attemptsPerChunk,
            double spawnChance,
            int maxClustersPerChunk,
            int clusterSize,
            int minY,
            int maxY,
            int surfaceScanDepth) {
    }
}
