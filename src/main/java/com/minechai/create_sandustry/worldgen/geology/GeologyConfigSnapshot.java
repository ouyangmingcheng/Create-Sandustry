package com.minechai.create_sandustry.worldgen.geology;

import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModBlocks;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 基础地质生成设置快照。
 *
 * <p>除唯一总开关外，其余参数采用代码固定默认值。</p>
 */
public record GeologyConfigSnapshot(
        boolean enabled,
        int maxBlockChecksPerRule,
        WetSandSettings wet,
        VolatileSandSettings volatileSand,
        DepthRockSettings hardened,
        DepthRockSettings petrified) {

    private static volatile GeologyConfigSnapshot cached;

    public static GeologyConfigSnapshot current() {
        boolean enabled = ModConfigs.WORLDGEN_ENABLED.get();
        GeologyConfigSnapshot snapshot = cached;
        if (snapshot != null && snapshot.enabled == enabled) {
            return snapshot;
        }

        synchronized (GeologyConfigSnapshot.class) {
            snapshot = cached;
            if (snapshot != null && snapshot.enabled == enabled) {
                return snapshot;
            }
            snapshot = build(enabled);
            cached = snapshot;
            return snapshot;
        }
    }

    public boolean anyRuleEnabled() {
        return enabled;
    }

    private static GeologyConfigSnapshot build(boolean enabled) {
        WetSandSettings wet = new WetSandSettings(
                enabled,
                8,
                4,
                16,
                true,
                Set.of(Blocks.SAND, Blocks.RED_SAND),
                512);

        VolatileSandSettings volatileSand = new VolatileSandSettings(
                enabled,
                6,
                8,
                4,
                true,
                true,
                Set.of(Blocks.STONE, ModBlocks.HARDENED_SAND.get()),
                2048,
                Set.of(Blocks.SAND, Blocks.RED_SAND, ModBlocks.HARDENED_SAND.get()),
                2048);

        DepthRockSettings hardened = new DepthRockSettings(
                enabled,
                GeologyHeightMode.FIXED,
                0,
                320,
                0,
                8,
                Set.of(Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.STONE),
                131072);

        DepthRockSettings petrified = new DepthRockSettings(
                enabled,
                GeologyHeightMode.FIXED,
                -64,
                -16,
                32,
                64,
                Set.of(Blocks.DEEPSLATE),
                16384);

        return new GeologyConfigSnapshot(
                enabled,
                200000,
                wet,
                volatileSand,
                hardened,
                petrified);
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
            GeologyHeightMode heightMode,
            int fixedMinY,
            int fixedMaxY,
            int relativeMinDepth,
            int relativeMaxDepth,
            Set<Block> targets,
            int maxReplacements) {
    }
}
