package com.minechai.create_sandustry.config;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 沙金工艺世界生成唯一总开关。
 *
 * <p>基础地质、可疑方块团簇、村庄猫砂和建筑活沙都读取此开关。
 * 关闭时相关 Feature 会立即返回，不执行方块扫描或结构查询。</p>
 */
public final class WorldgenConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("沙金工艺所有自然世界生成的总开关。默认关闭。",
                    "开启后统一启用基础地质替换、可疑沙/沙砾、猫砂和活沙团簇。",
                    "关闭后所有相关 Feature 立即返回，不扫描方块、读取结构引用或生成方块。",
                    "只影响新生成区块，不会回写已有区块。")
            .define("worldgen.enabled", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().getActiveContainer().registerConfig(
                ModConfig.Type.COMMON,
                SPEC,
                "createsandustry-worldgen-common.toml");
    }

    private WorldgenConfig() {
    }
}
