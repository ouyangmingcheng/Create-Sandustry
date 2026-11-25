package com.minechai.create_sandustry.content.worldgen.geology.rules;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyConfigSnapshot;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyRuleProcessor;

/**
 * 规则 C：按固定 Y 或地表相对深度把指定浅层岩石替换为硬化沙子。
 */
public final class HardenedSandRule extends AbstractDepthRockRule implements GeologyRuleProcessor {
    @Override
    public String id() {
        return "hardened_sand";
    }

    @Override
    public boolean isEnabled(GeologyConfigSnapshot config) {
        return config.enabled() && config.hardened().enabled();
    }

    @Override
    public boolean apply(GeologyGenerationContext context, GeologyConfigSnapshot config) {
        return applyDepthRange(context, config.hardened(), ModBlocks.HARDENED_SAND.get());
    }
}
