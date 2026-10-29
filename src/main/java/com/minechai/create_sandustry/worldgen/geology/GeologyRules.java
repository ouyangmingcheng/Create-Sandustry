package com.minechai.create_sandustry.worldgen.geology;

import java.util.List;
import com.minechai.create_sandustry.worldgen.geology.rules.HardenedSandRule;
import com.minechai.create_sandustry.worldgen.geology.rules.PetrifiedSandRule;
import com.minechai.create_sandustry.worldgen.geology.rules.VolatileSandRule;
import com.minechai.create_sandustry.worldgen.geology.rules.WetSandRule;

/**
 * 地质规则执行顺序。稀有且受岩浆约束的规则先执行，避免同一沙子被湿沙规则提前消费。
 */
public final class GeologyRules {
    public static final List<GeologyRuleProcessor> ALL = List.of(
            new VolatileSandRule(),
            new WetSandRule(),
            new HardenedSandRule(),
            new PetrifiedSandRule());

    private GeologyRules() {
    }
}
