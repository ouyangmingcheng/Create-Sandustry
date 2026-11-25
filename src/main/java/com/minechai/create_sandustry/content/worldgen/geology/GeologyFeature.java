package com.minechai.create_sandustry.content.worldgen.geology;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * 沙金工艺地质替换 Feature。
 *
 * <p>该 Feature 在 VEGETAL_DECORATION 阶段每个区块执行一次，只处理当前区块；
 * 特殊沙子和考古团簇会在之后的 TOP_LAYER_MODIFICATION 阶段执行。
 * 各规则独立扫描，并共享相同的区块边界和安全写入约束。</p>
 */
public final class GeologyFeature extends Feature<GeologyFeatureConfiguration> {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public GeologyFeature() {
        super(GeologyFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GeologyFeatureConfiguration> featureContext) {
        if (featureContext.config().schemaVersion() != CURRENT_SCHEMA_VERSION) {
            return false;
        }

        WorldGenLevel level = featureContext.level();
        BlockPos origin = featureContext.origin();
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        GeologyConfigSnapshot config = GeologyConfigSnapshot.current();
        if (!config.anyRuleEnabled()) {
            return false;
        }

        boolean changed = false;
        for (GeologyRuleProcessor rule : GeologyRules.ALL) {
            if (!rule.isEnabled(config)) {
                continue;
            }

            GeologyGenerationContext context = new GeologyGenerationContext(
                    level,
                    featureContext.random(),
                    chunkX,
                    chunkZ,
                    config.maxBlockChecksPerRule());
            changed |= rule.apply(context, config);
        }
        return changed;
    }
}
