package com.minechai.create_sandustry.content.worldgen.geology.rules;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyConfigSnapshot;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyRuleProcessor;

/**
 * 规则 D：把指定深层岩石替换为石化沙子。
 */
public final class PetrifiedSandRule extends AbstractDepthRockRule implements GeologyRuleProcessor {
    @Override
    public String id() {
        return "petrified_sand";
    }

    @Override
    public boolean isEnabled(GeologyConfigSnapshot config) {
        return config.enabled() && config.petrified().enabled();
    }

    @Override
    public boolean apply(GeologyGenerationContext context, GeologyConfigSnapshot config) {
        return applyDepthRange(context, config.petrified(), ModBlocks.PETRIFIED_SAND.get());
    }
}
