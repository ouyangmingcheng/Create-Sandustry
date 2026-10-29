package com.minechai.create_sandustry.config;

import java.util.List;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 特殊沙子与考古团簇的独立配置。
 *
 * <p>这些 Feature 在 TOP_LAYER_MODIFICATION 阶段执行，晚于基础地质替换和结构放置。
 * 配置只在新区块首次生成时读取，不触发已加载区块的重新扫描。</p>
 */
public final class SpecialSandConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("特殊沙子和考古团簇系统总开关。默认关闭。",
                    "关闭后可疑沙、可疑沙砾、村庄猫砂和建筑活沙规则都不运行。")
            .define("special_sand.enabled", false);

    public static final ModConfigSpec.IntValue MAX_BLOCK_CHECKS_PER_FEATURE = BUILDER
            .comment("每个特殊 Feature 在每个区块允许执行的最大方块检查次数。",
                    "默认 50000，用于限制异常大范围或高尝试次数配置的开销。")
            .defineInRange("special_sand.performance.max_block_checks_per_feature", 50000, 256, 2000000);

    // 可疑沙子
    public static final ModConfigSpec.BooleanValue SUSPICIOUS_SAND_ENABLED = BUILDER
            .comment("是否启用可疑沙子自然团簇。默认关闭。")
            .define("special_sand.suspicious_sand.enabled", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SUSPICIOUS_SAND_BIOMES = BUILDER
            .comment("允许生成可疑沙子团簇的群系 ID 白名单。默认仅沙漠。",
                    "Feature 会加入全部主世界群系，但会在运行时严格检查此列表。")
            .defineListAllowEmpty("special_sand.suspicious_sand.biomes",
                    List.of("minecraft:desert"), value -> value instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SUSPICIOUS_SAND_TARGETS = BUILDER
            .comment("可疑沙子团簇允许替换的精确方块 ID。默认仅 minecraft:sand。",
                    "不会匹配红沙、砂岩、特殊沙子或其他模组方块。")
            .defineListAllowEmpty("special_sand.suspicious_sand.target_blocks",
                    List.of("minecraft:sand"), value -> value instanceof String);

    public static final ModConfigSpec.IntValue SUSPICIOUS_SAND_ATTEMPTS = BUILDER
            .comment("每个区块尝试生成可疑沙子团簇的次数。默认 10。")
            .defineInRange("special_sand.suspicious_sand.attempts_per_chunk", 10, 0, 128);

    public static final ModConfigSpec.DoubleValue SUSPICIOUS_SAND_CHANCE = BUILDER
            .comment("每次可疑沙子生成尝试成功执行的概率。默认 0.55。")
            .defineInRange("special_sand.suspicious_sand.spawn_chance", 0.55D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue SUSPICIOUS_SAND_MAX_CLUSTERS = BUILDER
            .comment("每个区块最多生成的可疑沙子团簇数量。默认 4。")
            .defineInRange("special_sand.suspicious_sand.max_clusters_per_chunk", 4, 0, 64);

    public static final ModConfigSpec.IntValue SUSPICIOUS_SAND_CLUSTER_SIZE = BUILDER
            .comment("单个可疑沙子团簇的目标方块数量。默认 8。")
            .defineInRange("special_sand.suspicious_sand.cluster_size", 8, 1, 64);

    public static final ModConfigSpec.IntValue SUSPICIOUS_SAND_MIN_Y = BUILDER
            .comment("可疑沙子允许生成的最低 Y（含）。默认 50。")
            .defineInRange("special_sand.suspicious_sand.min_y", 50, -2032, 2031);

    public static final ModConfigSpec.IntValue SUSPICIOUS_SAND_MAX_Y = BUILDER
            .comment("可疑沙子允许生成的最高 Y（含）。默认 120。")
            .defineInRange("special_sand.suspicious_sand.max_y", 120, -2032, 2031);

    public static final ModConfigSpec.IntValue SUSPICIOUS_SAND_SCAN_DEPTH = BUILDER
            .comment("从地表向下寻找普通沙子的最大深度。默认 12。")
            .defineInRange("special_sand.suspicious_sand.surface_scan_depth", 12, 1, 64);

    public static final ModConfigSpec.ConfigValue<String> SUSPICIOUS_SAND_LOOT_TABLE = BUILDER
            .comment("可疑沙子方块实体使用的考古战利品表 ID。",
                    "默认 minecraft:archaeology/desert_pyramid。此配置只影响新生成的方块。")
            .define("special_sand.suspicious_sand.loot_table", "minecraft:archaeology/desert_pyramid");

    // 可疑沙砾
    public static final ModConfigSpec.BooleanValue SUSPICIOUS_GRAVEL_ENABLED = BUILDER
            .comment("是否启用可疑沙砾自然团簇。默认关闭。")
            .define("special_sand.suspicious_gravel.enabled", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SUSPICIOUS_GRAVEL_BIOMES = BUILDER
            .comment("允许生成可疑沙砾团簇的群系 ID 白名单。默认砾石丘陵和石岸。")
            .defineListAllowEmpty("special_sand.suspicious_gravel.biomes",
                    List.of("minecraft:windswept_gravelly_hills", "minecraft:stony_shore"),
                    value -> value instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SUSPICIOUS_GRAVEL_TARGETS = BUILDER
            .comment("可疑沙砾团簇允许替换的精确方块 ID。默认仅 minecraft:gravel。",
                    "不会替换石头、砂岩、深板岩或其他方块，也不会整体替换原版沙砾矿脉。")
            .defineListAllowEmpty("special_sand.suspicious_gravel.target_blocks",
                    List.of("minecraft:gravel"), value -> value instanceof String);

    public static final ModConfigSpec.IntValue SUSPICIOUS_GRAVEL_ATTEMPTS = BUILDER
            .comment("每个区块尝试生成可疑沙砾团簇的次数。默认 5。")
            .defineInRange("special_sand.suspicious_gravel.attempts_per_chunk", 5, 0, 128);

    public static final ModConfigSpec.DoubleValue SUSPICIOUS_GRAVEL_CHANCE = BUILDER
            .comment("每次可疑沙砾生成尝试成功执行的概率。默认 0.30。")
            .defineInRange("special_sand.suspicious_gravel.spawn_chance", 0.30D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue SUSPICIOUS_GRAVEL_MAX_CLUSTERS = BUILDER
            .comment("每个区块最多生成的可疑沙砾团簇数量。默认 2。")
            .defineInRange("special_sand.suspicious_gravel.max_clusters_per_chunk", 2, 0, 64);

    public static final ModConfigSpec.IntValue SUSPICIOUS_GRAVEL_CLUSTER_SIZE = BUILDER
            .comment("单个可疑沙砾团簇的目标方块数量。默认 6。")
            .defineInRange("special_sand.suspicious_gravel.cluster_size", 6, 1, 64);

    public static final ModConfigSpec.IntValue SUSPICIOUS_GRAVEL_MIN_Y = BUILDER
            .comment("可疑沙砾允许生成的最低 Y（含）。默认 40。")
            .defineInRange("special_sand.suspicious_gravel.min_y", 40, -2032, 2031);

    public static final ModConfigSpec.IntValue SUSPICIOUS_GRAVEL_MAX_Y = BUILDER
            .comment("可疑沙砾允许生成的最高 Y（含）。默认 110。")
            .defineInRange("special_sand.suspicious_gravel.max_y", 110, -2032, 2031);

    public static final ModConfigSpec.IntValue SUSPICIOUS_GRAVEL_SCAN_DEPTH = BUILDER
            .comment("从地表向下寻找普通沙砾的最大深度。默认 12。")
            .defineInRange("special_sand.suspicious_gravel.surface_scan_depth", 12, 1, 64);

    public static final ModConfigSpec.ConfigValue<String> SUSPICIOUS_GRAVEL_LOOT_TABLE = BUILDER
            .comment("可疑沙砾方块实体使用的考古战利品表 ID。",
                    "默认 minecraft:archaeology/trail_ruins_common。")
            .define("special_sand.suspicious_gravel.loot_table", "minecraft:archaeology/trail_ruins_common");

    // 村庄猫砂
    public static final ModConfigSpec.BooleanValue CAT_LITTER_ENABLED = BUILDER
            .comment("是否启用村庄周边的猫砂生成。默认关闭。")
            .define("special_sand.cat_litter.enabled", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CAT_LITTER_STRUCTURES = BUILDER
            .comment("被视为村庄的结构 ID 白名单。默认包含五种原版村庄。",
                    "结构 ID 取自 minecraft:worldgen/structure 注册表，不是群系或模板池 ID。")
            .defineListAllowEmpty("special_sand.cat_litter.structures",
                    List.of("minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna",
                            "minecraft:village_snowy", "minecraft:village_taiga"),
                    value -> value instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CAT_LITTER_TARGETS = BUILDER
            .comment("猫砂规则允许替换的精确方块 ID。默认仅 minecraft:sand。")
            .defineListAllowEmpty("special_sand.cat_litter.target_blocks",
                    List.of("minecraft:sand"), value -> value instanceof String);

    public static final ModConfigSpec.IntValue CAT_LITTER_RADIUS = BUILDER
            .comment("村庄包围盒外侧允许生成猫砂的水平距离，单位：方块。默认 12。",
                    "村庄建筑包围盒内部始终受保护，不会替换任何方块。")
            .defineInRange("special_sand.cat_litter.structure_radius", 12, 1, 16);

    public static final ModConfigSpec.IntValue CAT_LITTER_ATTEMPTS = BUILDER
            .comment("每个区块寻找村庄周边沙地的尝试次数。默认 8。")
            .defineInRange("special_sand.cat_litter.attempts_per_chunk", 8, 1, 128);

    public static final ModConfigSpec.DoubleValue CAT_LITTER_CHANCE = BUILDER
            .comment("每次猫砂生成尝试成功执行的概率。默认 0.45。")
            .defineInRange("special_sand.cat_litter.spawn_chance", 0.45D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue CAT_LITTER_MAX_CLUSTERS = BUILDER
            .comment("每个区块最多生成的猫砂团簇数量。默认 2。")
            .defineInRange("special_sand.cat_litter.max_clusters_per_chunk", 2, 0, 32);

    public static final ModConfigSpec.IntValue CAT_LITTER_CLUSTER_SIZE = BUILDER
            .comment("单个猫砂团簇的目标方块数量。默认 5。")
            .defineInRange("special_sand.cat_litter.cluster_size", 5, 1, 32);

    public static final ModConfigSpec.IntValue CAT_LITTER_MIN_Y = BUILDER
            .comment("猫砂允许生成的最低 Y（含）。默认 50。")
            .defineInRange("special_sand.cat_litter.min_y", 50, -2032, 2031);

    public static final ModConfigSpec.IntValue CAT_LITTER_MAX_Y = BUILDER
            .comment("猫砂允许生成的最高 Y（含）。默认 100。")
            .defineInRange("special_sand.cat_litter.max_y", 100, -2032, 2031);

    public static final ModConfigSpec.IntValue CAT_LITTER_SCAN_DEPTH = BUILDER
            .comment("从地表向下寻找普通沙子的最大深度。默认 12。")
            .defineInRange("special_sand.cat_litter.surface_scan_depth", 12, 1, 64);

    // 建筑活沙
    public static final ModConfigSpec.BooleanValue LIVING_SAND_ENABLED = BUILDER
            .comment("是否启用建筑周边的活沙生成。默认关闭，生成频率远低于可疑沙子。")
            .define("special_sand.living_sand.enabled", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LIVING_SAND_STRUCTURES = BUILDER
            .comment("允许触发活沙生成的结构 ID 白名单。默认包含原版村庄、神殿、前哨站、 mansion 等建筑。",
                    "可加入其他模组结构 ID；结构必须使用 Minecraft Structure 系统生成。")
            .defineListAllowEmpty("special_sand.living_sand.structures",
                    List.of("minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna",
                            "minecraft:village_snowy", "minecraft:village_taiga", "minecraft:desert_pyramid",
                            "minecraft:jungle_pyramid", "minecraft:pillager_outpost", "minecraft:mansion",
                            "minecraft:swamp_hut", "minecraft:igloo", "minecraft:shipwreck",
                            "minecraft:shipwreck_beached", "minecraft:trail_ruins"),
                    value -> value instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LIVING_SAND_TARGETS = BUILDER
            .comment("活沙规则允许替换的精确方块 ID。默认仅 minecraft:sand。",
                    "不会把砂岩、石头、建筑方块或其他模组方块识别为建筑材质。")
            .defineListAllowEmpty("special_sand.living_sand.target_blocks",
                    List.of("minecraft:sand"), value -> value instanceof String);

    public static final ModConfigSpec.IntValue LIVING_SAND_RADIUS = BUILDER
            .comment("结构包围盒外侧允许生成活沙的水平距离，单位：方块。默认 16。",
                    "结构包围盒内部及其地基范围不会写入。")
            .defineInRange("special_sand.living_sand.structure_radius", 16, 1, 16);

    public static final ModConfigSpec.IntValue LIVING_SAND_ATTEMPTS = BUILDER
            .comment("每个区块寻找建筑周边沙地的尝试次数。默认 4。")
            .defineInRange("special_sand.living_sand.attempts_per_chunk", 4, 1, 64);

    public static final ModConfigSpec.DoubleValue LIVING_SAND_CHANCE = BUILDER
            .comment("每次活沙生成尝试成功执行的概率。默认 0.06，明显低于可疑沙子。")
            .defineInRange("special_sand.living_sand.spawn_chance", 0.06D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue LIVING_SAND_MAX_CLUSTERS = BUILDER
            .comment("每个区块最多生成的活沙团簇数量。默认 1。")
            .defineInRange("special_sand.living_sand.max_clusters_per_chunk", 1, 0, 16);

    public static final ModConfigSpec.IntValue LIVING_SAND_CLUSTER_SIZE = BUILDER
            .comment("单个活沙团簇的目标方块数量。默认 1，可调高形成极小团簇。")
            .defineInRange("special_sand.living_sand.cluster_size", 1, 1, 8);

    public static final ModConfigSpec.IntValue LIVING_SAND_MIN_Y = BUILDER
            .comment("活沙允许生成的最低 Y（含）。默认 50。")
            .defineInRange("special_sand.living_sand.min_y", 50, -2032, 2031);

    public static final ModConfigSpec.IntValue LIVING_SAND_MAX_Y = BUILDER
            .comment("活沙允许生成的最高 Y（含）。默认 120。")
            .defineInRange("special_sand.living_sand.max_y", 120, -2032, 2031);

    public static final ModConfigSpec.IntValue LIVING_SAND_SCAN_DEPTH = BUILDER
            .comment("从地表向下寻找普通沙子的最大深度。默认 12。")
            .defineInRange("special_sand.living_sand.surface_scan_depth", 12, 1, 64);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().getActiveContainer().registerConfig(
                ModConfig.Type.COMMON,
                SPEC,
                "createsandustry-special-sand-common.toml");
    }

    private SpecialSandConfig() {
    }
}
