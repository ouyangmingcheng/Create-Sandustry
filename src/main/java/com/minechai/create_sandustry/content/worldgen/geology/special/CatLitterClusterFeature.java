package com.minechai.create_sandustry.content.worldgen.geology.special;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** 原版村庄包围盒外围的稀疏猫砂团簇。 */
public final class CatLitterClusterFeature extends SpecialClusterFeature {
    @Override
    protected boolean placeSpecial(
            FeaturePlaceContext<SpecialClusterFeatureConfiguration> featureContext,
            SpecialClusterConfigSnapshot config) {
        if (!config.catLitter().enabled()) {
            return false;
        }
        GeologyGenerationContext context = generationContext(featureContext, config);
        return StructureClusterSupport.generate(context, config.catLitter(), ModBlocks.CAT_LITTER.get());
    }
}
