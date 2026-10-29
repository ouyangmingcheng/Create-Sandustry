package com.minechai.create_sandustry.worldgen.geology.special;

import com.minechai.create_sandustry.worldgen.geology.GeologyGenerationContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** 砾石丘陵等白名单群系中的可疑沙砾自然团簇。 */
public final class SuspiciousGravelClusterFeature extends SpecialClusterFeature {
    @Override
    protected boolean placeSpecial(
            FeaturePlaceContext<SpecialClusterFeatureConfiguration> featureContext,
            SpecialClusterConfigSnapshot config) {
        if (!config.suspiciousGravel().enabled()) {
            return false;
        }
        GeologyGenerationContext context = generationContext(featureContext, config);
        return TerrainClusterSupport.generate(context, config.suspiciousGravel(), Blocks.SUSPICIOUS_GRAVEL);
    }
}
