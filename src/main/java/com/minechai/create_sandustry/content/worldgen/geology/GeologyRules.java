package com.minechai.create_sandustry.content.worldgen.geology;

import java.util.List;
import com.minechai.create_sandustry.content.worldgen.geology.rules.HardenedSandRule;
import com.minechai.create_sandustry.content.worldgen.geology.rules.PetrifiedSandRule;
import com.minechai.create_sandustry.content.worldgen.geology.rules.SandifiedOreRule;
import com.minechai.create_sandustry.content.worldgen.geology.rules.VolatileSandRule;
import com.minechai.create_sandustry.content.worldgen.geology.rules.WetSandRule;

/**
 * 地质规则顺序：矿石映射 -> 浅层/深层硬化 -> 岩浆湖烈性沙辐射 -> 湿沙。
 */
public final class GeologyRules {
    public static final List<GeologyRuleProcessor> ALL = List.of(
            new SandifiedOreRule(),
            new HardenedSandRule(),
            new PetrifiedSandRule(),
            new VolatileSandRule(),
            new WetSandRule());

    private GeologyRules() {
    }
}
