package com.minechai.create_sandustry.content.worldgen.geology.special;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;

/**
 * 特殊团簇 Feature 的公共入口。所有子类只在当前生成区块内写入。
 */
public abstract class SpecialClusterFeature extends Feature<SpecialClusterFeatureConfiguration> {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    protected SpecialClusterFeature() {
        super(SpecialClusterFeatureConfiguration.CODEC);
    }

    @Override
    public final boolean place(FeaturePlaceContext<SpecialClusterFeatureConfiguration> featureContext) {
        if (featureContext.config().schemaVersion() != CURRENT_SCHEMA_VERSION) {
            return false;
        }

        SpecialClusterConfigSnapshot config = SpecialClusterConfigSnapshot.current();
        if (!config.enabled()) {
            return false;
        }
        return placeSpecial(featureContext, config);
    }

    protected abstract boolean placeSpecial(
            FeaturePlaceContext<SpecialClusterFeatureConfiguration> featureContext,
            SpecialClusterConfigSnapshot config);

    protected static GeologyGenerationContext generationContext(
            FeaturePlaceContext<SpecialClusterFeatureConfiguration> featureContext,
            SpecialClusterConfigSnapshot config) {
        WorldGenLevel level = featureContext.level();
        BlockPos origin = featureContext.origin();
        return new GeologyGenerationContext(
                level,
                featureContext.random(),
                SectionPos.blockToSectionCoord(origin.getX()),
                SectionPos.blockToSectionCoord(origin.getZ()),
                config.maxBlockChecksPerFeature());
    }
}
