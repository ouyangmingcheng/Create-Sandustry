package com.minechai.create_sandustry.config;

import java.util.List;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 沙金工艺地质生成独立配置。
 *
 * <p>该配置只影响新生成区块中的世界生成 Feature，不会在世界加载时扫描或转换已有区块。</p>
 */
public final class GeologyConfig {
    public enum HeightMode {
        /** 使用固定 Y 坐标范围。 */
        FIXED,
        /** 从 WORLD_SURFACE_WG 地表向下计算相对深度范围。 */
        SURFACE_RELATIVE
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue GEOLOGY_ENABLED = BUILDER
            .comment("地质生成系统总开关。关闭后四条规则都不运行。",
                    "该 Feature 只在新区块生成阶段执行，不会改写已加载或已有区块。")
            .define("geology.enabled", true);

    public static final ModConfigSpec.IntValue MAX_BLOCK_CHECKS_PER_RULE = BUILDER
            .comment("每条规则在每个区块内允许执行的最大方块检查次数，用于限制最坏情况下的生成开销。",
                    "默认 200000。四条规则各自拥有独立预算，互不抢占。")
            .defineInRange("geology.performance.max_block_checks_per_rule", 200000, 256, 4000000);

    // 规则 A：湿沙子
    public static final ModConfigSpec.BooleanValue WET_ENABLED = BUILDER
            .comment("是否启用湿沙子地质规则。默认开启。")
            .define("geology.wet.enabled", true);

    public static final ModConfigSpec.IntValue WET_HORIZONTAL_RADIUS = BUILDER
            .comment("湿沙子检测天然水体时使用的水平半径，单位：方块。默认 8。",
                    "检测范围限制在当前生成区块内，不会为了寻找水体而加载邻区块。")
            .defineInRange("geology.wet.horizontal_radius", 8, 1, 32);

    public static final ModConfigSpec.IntValue WET_VERTICAL_RADIUS = BUILDER
            .comment("湿沙子检测天然水体时使用的垂直半径，单位：方块。默认 4。")
            .defineInRange("geology.wet.vertical_radius", 4, 0, 32);

    public static final ModConfigSpec.IntValue WET_SURFACE_SCAN_DEPTH = BUILDER
            .comment("每个区块列从 WORLD_SURFACE_WG 向下扫描沙子的深度，用于优先覆盖河岸、湖岸和海岸。",
                    "默认 16。该限制避免每区块无条件遍历全部建筑高度。")
            .defineInRange("geology.wet.surface_scan_depth", 16, 1, 64);

    public static final ModConfigSpec.BooleanValue WET_INCLUDE_RED_SAND = BUILDER
            .comment("湿沙子规则是否允许替换 minecraft:red_sand。",
                    "即使目标列表包含红沙，也必须将此项设为 true 才会替换。默认开启。")
            .define("geology.wet.include_red_sand", true);

    public static final ModConfigSpec.IntValue WET_MAX_REPLACEMENTS = BUILDER
            .comment("每个区块中湿沙子规则最多替换的方块数。默认 512。")
            .defineInRange("geology.wet.max_replacements_per_chunk", 512, 0, 65536);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> WET_TARGET_BLOCKS = BUILDER
            .comment("湿沙子规则允许替换的精确方块 ID 列表。默认普通沙子和红沙。",
                    "只接受完整 ID，不支持通配符或名称模糊匹配。")
            .defineListAllowEmpty("geology.wet.target_blocks",
                    List.of("minecraft:sand", "minecraft:red_sand"),
                    value -> value instanceof String);

    // 规则 B：烈性沙子
    public static final ModConfigSpec.BooleanValue VOLATILE_ENABLED = BUILDER
            .comment("是否启用烈性沙子地质规则。默认开启。")
            .define("geology.volatile.enabled", true);

    public static final ModConfigSpec.IntValue VOLATILE_HORIZONTAL_RADIUS = BUILDER
            .comment("烈性沙子检测岩浆热源时使用的水平半径，单位：方块。默认 6。",
                    "只读取当前区块；跨区块边界的热源不会触发邻区块加载。")
            .defineInRange("geology.volatile.horizontal_radius", 6, 1, 32);

    public static final ModConfigSpec.IntValue VOLATILE_VERTICAL_RADIUS = BUILDER
            .comment("烈性沙子检测岩浆热源时使用的垂直半径，单位：方块。默认 8。")
            .defineInRange("geology.volatile.vertical_radius", 8, 0, 32);

    public static final ModConfigSpec.IntValue VOLATILE_MINIMUM_HEAT_BLOCKS = BUILDER
            .comment("目标沙子邻域内至少存在多少个岩浆/热源方块才允许转换。",
                    "默认 4，用于避免单个岩浆泉把周围全部沙子无条件转换为烈性沙子。")
            .defineInRange("geology.volatile.minimum_heat_blocks", 4, 1, 256);

    public static final ModConfigSpec.BooleanValue VOLATILE_INCLUDE_MAGMA_BLOCKS = BUILDER
            .comment("是否把 minecraft:magma_block 也视为合格热源。默认开启。")
            .define("geology.volatile.include_magma_blocks", true);

    public static final ModConfigSpec.IntValue VOLATILE_MAX_REPLACEMENTS = BUILDER
            .comment("每个区块中烈性沙子规则最多替换的方块数。默认 128。")
            .defineInRange("geology.volatile.max_replacements_per_chunk", 128, 0, 65536);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> VOLATILE_TARGET_BLOCKS = BUILDER
            .comment("烈性沙子规则允许替换的精确方块 ID 列表。默认普通沙子和红沙。",
                    "只接受完整 ID，不支持通配符或名称模糊匹配。")
            .defineListAllowEmpty("geology.volatile.target_blocks",
                    List.of("minecraft:sand", "minecraft:red_sand"),
                    value -> value instanceof String);

    // 规则 C：硬化沙子
    public static final ModConfigSpec.BooleanValue HARDENED_ENABLED = BUILDER
            .comment("是否启用硬化沙子地质规则。默认开启。")
            .define("geology.hardened.enabled", true);

    public static final ModConfigSpec.EnumValue<HeightMode> HARDENED_HEIGHT_MODE = BUILDER
            .comment("硬化沙子高度模式：FIXED 使用固定 Y 范围；SURFACE_RELATIVE 从地表向下计算深度。",
                    "默认 SURFACE_RELATIVE，便于在海平面、平原、山地与海岸保持相对一致的浅层表现。")
            .defineEnum("geology.hardened.height_mode", HeightMode.SURFACE_RELATIVE);

    public static final ModConfigSpec.IntValue HARDENED_FIXED_MIN_Y = BUILDER
            .comment("FIXED 模式下的最低 Y（含）。默认 -16。")
            .defineInRange("geology.hardened.fixed_min_y", -16, -2032, 2031);

    public static final ModConfigSpec.IntValue HARDENED_FIXED_MAX_Y = BUILDER
            .comment("FIXED 模式下的最高 Y（含）。默认 32。")
            .defineInRange("geology.hardened.fixed_max_y", 32, -2032, 2031);

    public static final ModConfigSpec.IntValue HARDENED_RELATIVE_MIN_DEPTH = BUILDER
            .comment("SURFACE_RELATIVE 模式下距离地表的最近深度。0 表示地表方块本身。默认 0。")
            .defineInRange("geology.hardened.relative_min_depth", 0, 0, 256);

    public static final ModConfigSpec.IntValue HARDENED_RELATIVE_MAX_DEPTH = BUILDER
            .comment("SURFACE_RELATIVE 模式下距离地表的最远深度。默认 8。")
            .defineInRange("geology.hardened.relative_max_depth", 8, 0, 256);

    public static final ModConfigSpec.IntValue HARDENED_MAX_REPLACEMENTS = BUILDER
            .comment("每个区块中硬化沙子规则最多替换的方块数。默认 4096。")
            .defineInRange("geology.hardened.max_replacements_per_chunk", 4096, 0, 65536);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HARDENED_TARGET_BLOCKS = BUILDER
            .comment("硬化沙子规则的精确岩石白名单。默认花岗岩、闪长岩、安山岩和石头。",
                    "不会替换矿石、结构方块、刷怪笼、基岩或任何未列出的方块。")
            .defineListAllowEmpty("geology.hardened.target_blocks",
                    List.of("minecraft:granite", "minecraft:diorite", "minecraft:andesite", "minecraft:stone"),
                    value -> value instanceof String);

    // 规则 D：石化沙子
    public static final ModConfigSpec.BooleanValue PETRIFIED_ENABLED = BUILDER
            .comment("是否启用石化沙子地质规则。默认开启。")
            .define("geology.petrified.enabled", true);

    public static final ModConfigSpec.EnumValue<HeightMode> PETRIFIED_HEIGHT_MODE = BUILDER
            .comment("石化沙子高度模式：FIXED 或 SURFACE_RELATIVE。默认 FIXED。",
                    "深度机制与硬化沙子一致，便于后续按需要切换。")
            .defineEnum("geology.petrified.height_mode", HeightMode.FIXED);

    public static final ModConfigSpec.IntValue PETRIFIED_FIXED_MIN_Y = BUILDER
            .comment("FIXED 模式下的最低 Y（含）。默认 -64。")
            .defineInRange("geology.petrified.fixed_min_y", -64, -2032, 2031);

    public static final ModConfigSpec.IntValue PETRIFIED_FIXED_MAX_Y = BUILDER
            .comment("FIXED 模式下的最高 Y（含）。默认 -16。")
            .defineInRange("geology.petrified.fixed_max_y", -16, -2032, 2031);

    public static final ModConfigSpec.IntValue PETRIFIED_RELATIVE_MIN_DEPTH = BUILDER
            .comment("SURFACE_RELATIVE 模式下距离地表的最近深度。默认 32。")
            .defineInRange("geology.petrified.relative_min_depth", 32, 0, 512);

    public static final ModConfigSpec.IntValue PETRIFIED_RELATIVE_MAX_DEPTH = BUILDER
            .comment("SURFACE_RELATIVE 模式下距离地表的最远深度。默认 64。")
            .defineInRange("geology.petrified.relative_max_depth", 64, 0, 512);

    public static final ModConfigSpec.IntValue PETRIFIED_MAX_REPLACEMENTS = BUILDER
            .comment("每个区块中石化沙子规则最多替换的方块数。默认 16384。")
            .defineInRange("geology.petrified.max_replacements_per_chunk", 16384, 0, 65536);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PETRIFIED_TARGET_BLOCKS = BUILDER
            .comment("石化沙子规则的精确深层岩石白名单。默认仅 minecraft:deepslate。",
                    "深板岩圆石、磨制深板岩、深板岩砖、深板岩瓦及矿石等必须手动加入才会生效。")
            .defineListAllowEmpty("geology.petrified.target_blocks",
                    List.of("minecraft:deepslate"),
                    value -> value instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().getActiveContainer().registerConfig(
                ModConfig.Type.COMMON,
                SPEC,
                "createsandustry-geology-common.toml");
    }

    private GeologyConfig() {
    }
}