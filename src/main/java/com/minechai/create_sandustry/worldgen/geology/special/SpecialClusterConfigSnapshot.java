package com.minechai.create_sandustry.worldgen.geology.special;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.config.SpecialSandConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 特殊团簇配置的不可变快照，避免并行世界生成时重复解析 ID。
 */
public record SpecialClusterConfigSnapshot(
        boolean enabled,
        int maxBlockChecksPerFeature,
        TerrainClusterSettings suspiciousSand,
        TerrainClusterSettings suspiciousGravel,
        StructureClusterSettings catLitter,
        StructureClusterSettings livingSand,
        List<Object> fingerprint) {

    private static volatile SpecialClusterConfigSnapshot cached;

    public static SpecialClusterConfigSnapshot current() {
        List<Object> key = createFingerprint();
        SpecialClusterConfigSnapshot snapshot = cached;
        if (snapshot != null && snapshot.fingerprint.equals(key)) {
            return snapshot;
        }

        synchronized (SpecialClusterConfigSnapshot.class) {
            snapshot = cached;
            if (snapshot != null && snapshot.fingerprint.equals(key)) {
                return snapshot;
            }
            snapshot = build(key);
            cached = snapshot;
            return snapshot;
        }
    }

    public boolean anyRuleEnabled() {
        return enabled && (suspiciousSand.enabled || suspiciousGravel.enabled || catLitter.enabled || livingSand.enabled);
    }

    private static SpecialClusterConfigSnapshot build(List<Object> fingerprint) {
        TerrainClusterSettings suspiciousSand = new TerrainClusterSettings(
                SpecialSandConfig.SUSPICIOUS_SAND_ENABLED.get(),
                resolveLocations("suspicious_sand biomes", SpecialSandConfig.SUSPICIOUS_SAND_BIOMES.get()),
                resolveBlocks("suspicious_sand", SpecialSandConfig.SUSPICIOUS_SAND_TARGETS.get()),
                SpecialSandConfig.SUSPICIOUS_SAND_ATTEMPTS.get(),
                SpecialSandConfig.SUSPICIOUS_SAND_CHANCE.get(),
                SpecialSandConfig.SUSPICIOUS_SAND_MAX_CLUSTERS.get(),
                SpecialSandConfig.SUSPICIOUS_SAND_CLUSTER_SIZE.get(),
                Math.min(SpecialSandConfig.SUSPICIOUS_SAND_MIN_Y.get(), SpecialSandConfig.SUSPICIOUS_SAND_MAX_Y.get()),
                Math.max(SpecialSandConfig.SUSPICIOUS_SAND_MIN_Y.get(), SpecialSandConfig.SUSPICIOUS_SAND_MAX_Y.get()),
                SpecialSandConfig.SUSPICIOUS_SAND_SCAN_DEPTH.get(),
                resolveLootTable("suspicious_sand", SpecialSandConfig.SUSPICIOUS_SAND_LOOT_TABLE.get(),
                        "minecraft:archaeology/desert_pyramid"));

        TerrainClusterSettings suspiciousGravel = new TerrainClusterSettings(
                SpecialSandConfig.SUSPICIOUS_GRAVEL_ENABLED.get(),
                resolveLocations("suspicious_gravel biomes", SpecialSandConfig.SUSPICIOUS_GRAVEL_BIOMES.get()),
                resolveBlocks("suspicious_gravel", SpecialSandConfig.SUSPICIOUS_GRAVEL_TARGETS.get()),
                SpecialSandConfig.SUSPICIOUS_GRAVEL_ATTEMPTS.get(),
                SpecialSandConfig.SUSPICIOUS_GRAVEL_CHANCE.get(),
                SpecialSandConfig.SUSPICIOUS_GRAVEL_MAX_CLUSTERS.get(),
                SpecialSandConfig.SUSPICIOUS_GRAVEL_CLUSTER_SIZE.get(),
                Math.min(SpecialSandConfig.SUSPICIOUS_GRAVEL_MIN_Y.get(), SpecialSandConfig.SUSPICIOUS_GRAVEL_MAX_Y.get()),
                Math.max(SpecialSandConfig.SUSPICIOUS_GRAVEL_MIN_Y.get(), SpecialSandConfig.SUSPICIOUS_GRAVEL_MAX_Y.get()),
                SpecialSandConfig.SUSPICIOUS_GRAVEL_SCAN_DEPTH.get(),
                resolveLootTable("suspicious_gravel", SpecialSandConfig.SUSPICIOUS_GRAVEL_LOOT_TABLE.get(),
                        "minecraft:archaeology/trail_ruins_common"));

        StructureClusterSettings catLitter = new StructureClusterSettings(
                SpecialSandConfig.CAT_LITTER_ENABLED.get(),
                resolveLocations("cat_litter structures", SpecialSandConfig.CAT_LITTER_STRUCTURES.get()),
                resolveBlocks("cat_litter", SpecialSandConfig.CAT_LITTER_TARGETS.get()),
                SpecialSandConfig.CAT_LITTER_RADIUS.get(),
                SpecialSandConfig.CAT_LITTER_ATTEMPTS.get(),
                SpecialSandConfig.CAT_LITTER_CHANCE.get(),
                SpecialSandConfig.CAT_LITTER_MAX_CLUSTERS.get(),
                SpecialSandConfig.CAT_LITTER_CLUSTER_SIZE.get(),
                Math.min(SpecialSandConfig.CAT_LITTER_MIN_Y.get(), SpecialSandConfig.CAT_LITTER_MAX_Y.get()),
                Math.max(SpecialSandConfig.CAT_LITTER_MIN_Y.get(), SpecialSandConfig.CAT_LITTER_MAX_Y.get()),
                SpecialSandConfig.CAT_LITTER_SCAN_DEPTH.get());

        StructureClusterSettings livingSand = new StructureClusterSettings(
                SpecialSandConfig.LIVING_SAND_ENABLED.get(),
                resolveLocations("living_sand structures", SpecialSandConfig.LIVING_SAND_STRUCTURES.get()),
                resolveBlocks("living_sand", SpecialSandConfig.LIVING_SAND_TARGETS.get()),
                SpecialSandConfig.LIVING_SAND_RADIUS.get(),
                SpecialSandConfig.LIVING_SAND_ATTEMPTS.get(),
                SpecialSandConfig.LIVING_SAND_CHANCE.get(),
                SpecialSandConfig.LIVING_SAND_MAX_CLUSTERS.get(),
                SpecialSandConfig.LIVING_SAND_CLUSTER_SIZE.get(),
                Math.min(SpecialSandConfig.LIVING_SAND_MIN_Y.get(), SpecialSandConfig.LIVING_SAND_MAX_Y.get()),
                Math.max(SpecialSandConfig.LIVING_SAND_MIN_Y.get(), SpecialSandConfig.LIVING_SAND_MAX_Y.get()),
                SpecialSandConfig.LIVING_SAND_SCAN_DEPTH.get());

        return new SpecialClusterConfigSnapshot(
                SpecialSandConfig.ENABLED.get(),
                SpecialSandConfig.MAX_BLOCK_CHECKS_PER_FEATURE.get(),
                suspiciousSand,
                suspiciousGravel,
                catLitter,
                livingSand,
                List.copyOf(fingerprint));
    }

    private static List<Object> createFingerprint() {
        List<Object> values = new ArrayList<>();
        values.add(SpecialSandConfig.ENABLED.get());
        values.add(SpecialSandConfig.MAX_BLOCK_CHECKS_PER_FEATURE.get());

        values.add(SpecialSandConfig.SUSPICIOUS_SAND_ENABLED.get());
        values.add(List.copyOf(SpecialSandConfig.SUSPICIOUS_SAND_BIOMES.get()));
        values.add(List.copyOf(SpecialSandConfig.SUSPICIOUS_SAND_TARGETS.get()));
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_ATTEMPTS.get());
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_CHANCE.get());
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_MAX_CLUSTERS.get());
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_CLUSTER_SIZE.get());
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_MIN_Y.get());
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_MAX_Y.get());
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_SCAN_DEPTH.get());
        values.add(SpecialSandConfig.SUSPICIOUS_SAND_LOOT_TABLE.get());

        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_ENABLED.get());
        values.add(List.copyOf(SpecialSandConfig.SUSPICIOUS_GRAVEL_BIOMES.get()));
        values.add(List.copyOf(SpecialSandConfig.SUSPICIOUS_GRAVEL_TARGETS.get()));
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_ATTEMPTS.get());
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_CHANCE.get());
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_MAX_CLUSTERS.get());
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_CLUSTER_SIZE.get());
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_MIN_Y.get());
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_MAX_Y.get());
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_SCAN_DEPTH.get());
        values.add(SpecialSandConfig.SUSPICIOUS_GRAVEL_LOOT_TABLE.get());

        values.add(SpecialSandConfig.CAT_LITTER_ENABLED.get());
        values.add(List.copyOf(SpecialSandConfig.CAT_LITTER_STRUCTURES.get()));
        values.add(List.copyOf(SpecialSandConfig.CAT_LITTER_TARGETS.get()));
        values.add(SpecialSandConfig.CAT_LITTER_RADIUS.get());
        values.add(SpecialSandConfig.CAT_LITTER_ATTEMPTS.get());
        values.add(SpecialSandConfig.CAT_LITTER_CHANCE.get());
        values.add(SpecialSandConfig.CAT_LITTER_MAX_CLUSTERS.get());
        values.add(SpecialSandConfig.CAT_LITTER_CLUSTER_SIZE.get());
        values.add(SpecialSandConfig.CAT_LITTER_MIN_Y.get());
        values.add(SpecialSandConfig.CAT_LITTER_MAX_Y.get());
        values.add(SpecialSandConfig.CAT_LITTER_SCAN_DEPTH.get());

        values.add(SpecialSandConfig.LIVING_SAND_ENABLED.get());
        values.add(List.copyOf(SpecialSandConfig.LIVING_SAND_STRUCTURES.get()));
        values.add(List.copyOf(SpecialSandConfig.LIVING_SAND_TARGETS.get()));
        values.add(SpecialSandConfig.LIVING_SAND_RADIUS.get());
        values.add(SpecialSandConfig.LIVING_SAND_ATTEMPTS.get());
        values.add(SpecialSandConfig.LIVING_SAND_CHANCE.get());
        values.add(SpecialSandConfig.LIVING_SAND_MAX_CLUSTERS.get());
        values.add(SpecialSandConfig.LIVING_SAND_CLUSTER_SIZE.get());
        values.add(SpecialSandConfig.LIVING_SAND_MIN_Y.get());
        values.add(SpecialSandConfig.LIVING_SAND_MAX_Y.get());
        values.add(SpecialSandConfig.LIVING_SAND_SCAN_DEPTH.get());
        return List.copyOf(values);
    }

    private static Set<Block> resolveBlocks(String ruleName, List<? extends String> configuredIds) {
        Set<Block> blocks = new HashSet<>();
        for (String configuredId : configuredIds) {
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            if (id == null) {
                CreateSandustry.LOGGER.warn("特殊团簇规则 {} 忽略无效方块 ID：{}", ruleName, configuredId);
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == Blocks.AIR) {
                CreateSandustry.LOGGER.warn("特殊团簇规则 {} 忽略未注册或不安全的方块 ID：{}", ruleName, configuredId);
                continue;
            }
            blocks.add(block);
        }
        return Set.copyOf(blocks);
    }

    private static Set<ResourceLocation> resolveLocations(String ruleName, List<? extends String> configuredIds) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (String configuredId : configuredIds) {
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            if (id == null) {
                CreateSandustry.LOGGER.warn("特殊团簇规则 {} 忽略无效资源 ID：{}", ruleName, configuredId);
                continue;
            }
            ids.add(id);
        }
        return Set.copyOf(ids);
    }

    private static ResourceKey<LootTable> resolveLootTable(String ruleName, String configuredId, String fallbackId) {
        ResourceLocation id = ResourceLocation.tryParse(configuredId);
        if (id == null) {
            CreateSandustry.LOGGER.warn("特殊团簇规则 {} 使用无效战利品表 ID：{}，回退到 {}", ruleName, configuredId, fallbackId);
            id = ResourceLocation.parse(fallbackId);
        }
        return ResourceKey.create(Registries.LOOT_TABLE, id);
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
