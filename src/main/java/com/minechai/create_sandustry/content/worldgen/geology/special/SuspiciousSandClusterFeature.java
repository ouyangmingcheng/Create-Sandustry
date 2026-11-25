package com.minechai.create_sandustry.content.worldgen.geology.special;

import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** 沙漠等白名单群系中的可疑沙子自然团簇。 */
public final class SuspiciousSandClusterFeature extends SpecialClusterFeature {
    @Override
    protected boolean placeSpecial(
            FeaturePlaceContext<SpecialClusterFeatureConfiguration> featureContext,
            SpecialClusterConfigSnapshot config) {
        if (!config.suspiciousSand().enabled()) {
            return false;
        }
        GeologyGenerationContext context = generationContext(featureContext, config);
        return TerrainClusterSupport.generate(context, config.suspiciousSand(), Blocks.SUSPICIOUS_SAND);
    }
}
