package com.minechai.create_sandustry.worldgen.geology;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.config.GeologyConfig;
import com.minechai.create_sandustry.config.GeologyConfig.HeightMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 地质生成配置的不可变快照。
 *
 * <p>世界生成可能并行执行，因此 Feature 不直接反复解析配置列表和方块 ID；
 * 配置值变化时会自动重建快照。</p>
 */
public record GeologyConfigSnapshot(
        boolean enabled,
        int maxBlockChecksPerRule,
        WetSandSettings wet,
        VolatileSandSettings volatileSand,
        DepthRockSettings hardened,
        DepthRockSettings petrified,
        List<Object> fingerprint) {

    private static volatile GeologyConfigSnapshot cached;

    public static GeologyConfigSnapshot current() {
        List<Object> key = createFingerprint();
        GeologyConfigSnapshot snapshot = cached;
        if (snapshot != null && snapshot.fingerprint.equals(key)) {
            return snapshot;
        }

        synchronized (GeologyConfigSnapshot.class) {
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
        return enabled && (wet.enabled || volatileSand.enabled || hardened.enabled || petrified.enabled);
    }

    private static GeologyConfigSnapshot build(List<Object> fingerprint) {
        WetSandSettings wet = new WetSandSettings(
                GeologyConfig.WET_ENABLED.get(),
                GeologyConfig.WET_HORIZONTAL_RADIUS.get(),
                GeologyConfig.WET_VERTICAL_RADIUS.get(),
                GeologyConfig.WET_SURFACE_SCAN_DEPTH.get(),
                GeologyConfig.WET_INCLUDE_RED_SAND.get(),
                Set.copyOf(resolveBlocks("wet_sand", GeologyConfig.WET_TARGET_BLOCKS.get())),
                GeologyConfig.WET_MAX_REPLACEMENTS.get());

        VolatileSandSettings volatileSand = new VolatileSandSettings(
                GeologyConfig.VOLATILE_ENABLED.get(),
                GeologyConfig.VOLATILE_HORIZONTAL_RADIUS.get(),
                GeologyConfig.VOLATILE_VERTICAL_RADIUS.get(),
                GeologyConfig.VOLATILE_MINIMUM_HEAT_BLOCKS.get(),
                GeologyConfig.VOLATILE_INCLUDE_MAGMA_BLOCKS.get(),
                GeologyConfig.VOLATILE_REPLACE_LAVA_LAKE_STONE.get(),
                Set.copyOf(resolveBlocks("volatile_lava_lake_stone", GeologyConfig.VOLATILE_LAVA_LAKE_STONE_TARGETS.get())),
                GeologyConfig.VOLATILE_LAVA_LAKE_STONE_MAX_REPLACEMENTS.get(),
                Set.copyOf(resolveBlocks("volatile_sand", GeologyConfig.VOLATILE_TARGET_BLOCKS.get())),
                GeologyConfig.VOLATILE_MAX_REPLACEMENTS.get());

        DepthRockSettings hardened = new DepthRockSettings(
                GeologyConfig.HARDENED_ENABLED.get(),
                GeologyConfig.HARDENED_HEIGHT_MODE.get(),
                GeologyConfig.HARDENED_FIXED_MIN_Y.get(),
                GeologyConfig.HARDENED_FIXED_MAX_Y.get(),
                GeologyConfig.HARDENED_RELATIVE_MIN_DEPTH.get(),
                GeologyConfig.HARDENED_RELATIVE_MAX_DEPTH.get(),
                Set.copyOf(resolveBlocks("hardened_sand", GeologyConfig.HARDENED_TARGET_BLOCKS.get())),
                GeologyConfig.HARDENED_MAX_REPLACEMENTS.get());

        DepthRockSettings petrified = new DepthRockSettings(
                GeologyConfig.PETRIFIED_ENABLED.get(),
                GeologyConfig.PETRIFIED_HEIGHT_MODE.get(),
                GeologyConfig.PETRIFIED_FIXED_MIN_Y.get(),
                GeologyConfig.PETRIFIED_FIXED_MAX_Y.get(),
                GeologyConfig.PETRIFIED_RELATIVE_MIN_DEPTH.get(),
                GeologyConfig.PETRIFIED_RELATIVE_MAX_DEPTH.get(),
                Set.copyOf(resolveBlocks("petrified_sand", GeologyConfig.PETRIFIED_TARGET_BLOCKS.get())),
                GeologyConfig.PETRIFIED_MAX_REPLACEMENTS.get());

        return new GeologyConfigSnapshot(
                GeologyConfig.GEOLOGY_ENABLED.get(),
                GeologyConfig.MAX_BLOCK_CHECKS_PER_RULE.get(),
                wet,
                volatileSand,
                hardened,
                petrified,
                List.copyOf(fingerprint));
    }

    private static List<Object> createFingerprint() {
        List<Object> values = new ArrayList<>();
        values.add(GeologyConfig.GEOLOGY_ENABLED.get());
        values.add(GeologyConfig.MAX_BLOCK_CHECKS_PER_RULE.get());
        values.add(GeologyConfig.WET_ENABLED.get());
        values.add(GeologyConfig.WET_HORIZONTAL_RADIUS.get());
        values.add(GeologyConfig.WET_VERTICAL_RADIUS.get());
        values.add(GeologyConfig.WET_SURFACE_SCAN_DEPTH.get());
        values.add(GeologyConfig.WET_INCLUDE_RED_SAND.get());
        values.add(List.copyOf(GeologyConfig.WET_TARGET_BLOCKS.get()));
        values.add(GeologyConfig.WET_MAX_REPLACEMENTS.get());
        values.add(GeologyConfig.VOLATILE_ENABLED.get());
        values.add(GeologyConfig.VOLATILE_HORIZONTAL_RADIUS.get());
        values.add(GeologyConfig.VOLATILE_VERTICAL_RADIUS.get());
        values.add(GeologyConfig.VOLATILE_MINIMUM_HEAT_BLOCKS.get());
        values.add(GeologyConfig.VOLATILE_INCLUDE_MAGMA_BLOCKS.get());
        values.add(GeologyConfig.VOLATILE_REPLACE_LAVA_LAKE_STONE.get());
        values.add(List.copyOf(GeologyConfig.VOLATILE_LAVA_LAKE_STONE_TARGETS.get()));
        values.add(GeologyConfig.VOLATILE_LAVA_LAKE_STONE_MAX_REPLACEMENTS.get());
        values.add(List.copyOf(GeologyConfig.VOLATILE_TARGET_BLOCKS.get()));
        values.add(GeologyConfig.VOLATILE_MAX_REPLACEMENTS.get());
        values.add(GeologyConfig.HARDENED_ENABLED.get());
        values.add(GeologyConfig.HARDENED_HEIGHT_MODE.get());
        values.add(GeologyConfig.HARDENED_FIXED_MIN_Y.get());
        values.add(GeologyConfig.HARDENED_FIXED_MAX_Y.get());
        values.add(GeologyConfig.HARDENED_RELATIVE_MIN_DEPTH.get());
        values.add(GeologyConfig.HARDENED_RELATIVE_MAX_DEPTH.get());
        values.add(List.copyOf(GeologyConfig.HARDENED_TARGET_BLOCKS.get()));
        values.add(GeologyConfig.HARDENED_MAX_REPLACEMENTS.get());
        values.add(GeologyConfig.PETRIFIED_ENABLED.get());
        values.add(GeologyConfig.PETRIFIED_HEIGHT_MODE.get());
        values.add(GeologyConfig.PETRIFIED_FIXED_MIN_Y.get());
        values.add(GeologyConfig.PETRIFIED_FIXED_MAX_Y.get());
        values.add(GeologyConfig.PETRIFIED_RELATIVE_MIN_DEPTH.get());
        values.add(GeologyConfig.PETRIFIED_RELATIVE_MAX_DEPTH.get());
        values.add(List.copyOf(GeologyConfig.PETRIFIED_TARGET_BLOCKS.get()));
        values.add(GeologyConfig.PETRIFIED_MAX_REPLACEMENTS.get());
        return List.copyOf(values);
    }

    private static Set<Block> resolveBlocks(String ruleName, List<? extends String> configuredIds) {
        Set<Block> blocks = new HashSet<>();
        for (String configuredId : configuredIds) {
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            if (id == null) {
                CreateSandustry.LOGGER.warn("地质规则 {} 忽略无效方块 ID：{}", ruleName, configuredId);
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == Blocks.AIR) {
                CreateSandustry.LOGGER.warn("地质规则 {} 忽略未注册或不安全的方块 ID：{}", ruleName, configuredId);
                continue;
            }
            blocks.add(block);
        }
        return blocks;
    }

    public record WetSandSettings(
            boolean enabled,
            int horizontalRadius,
            int verticalRadius,
            int surfaceScanDepth,
            boolean includeRedSand,
            Set<Block> targets,
            int maxReplacements) {
    }

    public record VolatileSandSettings(
            boolean enabled,
            int horizontalRadius,
            int verticalRadius,
            int minimumHeatBlocks,
            boolean includeMagmaBlocks,
            boolean replaceLavaLakeStone,
            Set<Block> lavaLakeStoneTargets,
            int lavaLakeStoneMaxReplacements,
            Set<Block> targets,
            int maxReplacements) {
    }

    public record DepthRockSettings(
            boolean enabled,
            HeightMode heightMode,
            int fixedMinY,
            int fixedMaxY,
            int relativeMinDepth,
            int relativeMaxDepth,
            Set<Block> targets,
            int maxReplacements) {
    }
}
