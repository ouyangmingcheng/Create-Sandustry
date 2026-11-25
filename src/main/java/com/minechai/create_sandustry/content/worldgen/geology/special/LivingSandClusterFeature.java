package com.minechai.create_sandustry.content.worldgen.geology.special;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** 白名单建筑结构外围的稀有活沙生成。 */
public final class LivingSandClusterFeature extends SpecialClusterFeature {
    @Override
    protected boolean placeSpecial(
            FeaturePlaceContext<SpecialClusterFeatureConfiguration> featureContext,
            SpecialClusterConfigSnapshot config) {
        if (!config.livingSand().enabled()) {
            return false;
        }
        GeologyGenerationContext context = generationContext(featureContext, config);
        return StructureClusterSupport.generate(context, config.livingSand(), ModBlocks.LIVING_SAND.get());
    }
}
