package com.minechai.create_sandustry.worldgen.geology;

import java.util.List;
import com.minechai.create_sandustry.worldgen.geology.rules.HardenedSandRule;
import com.minechai.create_sandustry.worldgen.geology.rules.PetrifiedSandRule;
import com.minechai.create_sandustry.worldgen.geology.rules.SandifiedOreRule;
import com.minechai.create_sandustry.worldgen.geology.rules.VolatileSandRule;
import com.minechai.create_sandustry.worldgen.geology.rules.WetSandRule;

/**
 * 地质规则执行顺序：先完成矿石精确映射，再执行岩浆、湿沙、浅层和深层替换。
 */
public final class GeologyRules {
    public static final List<GeologyRuleProcessor> ALL = List.of(
            new SandifiedOreRule(),
            new VolatileSandRule(),
            new WetSandRule(),
            new HardenedSandRule(),
            new PetrifiedSandRule());

    private GeologyRules() {
    }
}
