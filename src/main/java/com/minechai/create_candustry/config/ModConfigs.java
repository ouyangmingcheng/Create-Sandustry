package com.minechai.create_candustry.config;

import java.util.List;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfigs {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Florinol Battery
    public static final ModConfigSpec.IntValue FLORINOL_BATTERY_CAPACITY_PER_BLOCK = BUILDER
            .comment("单个弗洛林电池方块可以存储的弗洛林电池液数量。",
                    "单位：mB。",
                    "默认 50000 mB = 50 B = 50 kFE。")
            .defineInRange("florinol_battery.capacity_per_block", 50000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FLORINOL_BATTERY_MAX_HEIGHT = BUILDER
            .comment("弗洛林电池多方块结构允许的最大高度（层数），单位：方块。",
                    "水平占地固定为 3x3，宽度不可通过配置调整，只能调整高度。",
                    "默认 6，对应 3x3x6 = 54 个方块的完整结构。")
            .defineInRange("florinol_battery.max_height", 6, 1, 320);

    public static final ModConfigSpec.IntValue FLORINOL_BATTERY_FE_PER_MB = BUILDER
            .comment("每 1 mB 弗洛林电池液可以提供多少 FE 能量。",
                    "默认 1，表示 1 mB Florinol = 1 FE，1 B = 1 kFE。")
            .defineInRange("florinol_battery.fe_per_mb", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FLORINOL_BATTERY_MB_PER_SOIL = BUILDER
            .comment("生成 1 个弗洛林土壤需要消耗多少 mB 弗洛林电池液。",
                    "同时，1 个弗洛林土壤在电池内部占用相同数量的容量。",
                    "默认 1000 mB。")
            .defineInRange("florinol_battery.mb_per_soil", 1000, 1, Integer.MAX_VALUE);

    // Living Sand
    public static final ModConfigSpec.DoubleValue LIVING_SAND_SPAWN_FROM_SAND = BUILDER
            .comment("玩家破坏普通沙子时生成活沙的概率。",
                    "默认 0.0001 = 0.01%。")
            .defineInRange("living_sand.spawn_chance_from_sand", 0.0001D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue LIVING_SAND_SPAWN_FROM_HARDENED_SAND = BUILDER
            .comment("玩家破坏硬化沙时生成活沙的概率。",
                    "默认 0.008 = 0.8%。")
            .defineInRange("living_sand.spawn_chance_from_hardened_sand", 0.0001D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue LIVING_SAND_SPAWN_FROM_SUSPICIOUS_SAND = BUILDER
            .comment("玩家破坏可疑沙时生成活沙的概率。",
                    "默认 0.5 = 50%。")
            .defineInRange("living_sand.spawn_chance_from_suspicious_sand", 0.5D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue LIVING_SAND_ACTIVATION_DISTANCE = BUILDER
            .comment("玩家进入此距离后，活沙会从伪装状态苏醒并开始行动。",
                    "单位：方块。默认 3 格。")
            .defineInRange("living_sand.activation_distance", 3, 1, 64);

    public static final ModConfigSpec.IntValue LIVING_SAND_DEACTIVATION_DISTANCE = BUILDER
            .comment("活沙已经激活后，玩家距离超过此距离时会重新进入伪装状态。",
                    "单位：方块。默认 8 格。")
            .defineInRange("living_sand.deactivation_distance", 8, 1, 64);

    public static final ModConfigSpec.DoubleValue LIVING_SAND_ATTACK_DAMAGE = BUILDER
            .comment("活沙直接攻击玩家时造成的基础伤害。默认 3 点。")
            .defineInRange("living_sand.attack_damage", 3.0D, 0.0D, 1024.0D);

    public static final ModConfigSpec.DoubleValue LIVING_SAND_HEAD_ATTACH_CHANCE = BUILDER
            .comment("活沙攻击成功后附着到玩家头部装备槽的概率。默认 0.5 = 50%。")
            .defineInRange("living_sand.head_attach_chance", 0.5D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue LIVING_SAND_JUMP_STRENGTH = BUILDER
            .comment("活沙的跳跃力量。提高此值会让活沙跳得更高，从而更容易跨越 1 格高度差。",
                    "默认 0.55。")
            .defineInRange("living_sand.jump_strength", 0.55D, 0.0D, 4.0D);

    public static final ModConfigSpec.DoubleValue LIVING_SAND_ACTIVE_MOVEMENT_SPEED = BUILDER
            .comment("活沙激活并追踪玩家时的水平跳跃移动能力。默认 1.6。")
            .defineInRange("living_sand.active_movement_speed", 1.6D, 0.1D, 8.0D);

    public static final ModConfigSpec.IntValue LIVING_SAND_DORMANT_CHECK_INTERVAL = BUILDER
            .comment("活沙处于伪装状态时检查附近玩家的间隔，单位：tick。默认 10。")
            .defineInRange("living_sand.dormant_check_interval", 10, 1, 200);

    // Special sands
    public static final ModConfigSpec.DoubleValue VOLATILE_SAND_EXPLOSION_CHANCE = BUILDER
            .comment("挖掘烈性沙子时触发爆炸的概率。默认 0.1 = 10%。")
            .defineInRange("volatile_sand.explosion_chance", 0.1D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue VOLATILE_SAND_CHAIN_TRIGGER_CHANCE = BUILDER
            .comment("烈性沙子爆炸影响其他特殊沙子时，触发其特殊效果的概率。",
                    "默认 0.8 = 80%。该概率与烈性沙子自身的 10% 爆炸概率相互独立。")
            .defineInRange("volatile_sand.chain_trigger_chance", 0.8D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue VOLATILE_SAND_MAX_CHAIN_DEPTH = BUILDER
            .comment("烈性沙子链式爆炸允许达到的最大深度。",
                    "达到该深度后不再产生新的链式爆炸，防止无限递归。默认 32。")
            .defineInRange("volatile_sand.max_chain_depth", 32, 1, 512);

    public static final ModConfigSpec.DoubleValue THUNDER_SAND_TRIGGER_CHANCE = BUILDER
            .comment("挖掘雷霆沙子时触发特殊雷击效果的概率。",
                    "默认 0.1 = 10%。")
            .defineInRange("thunder_sand.trigger_chance", 0.1D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue THUNDER_SAND_MIN_STRIKES = BUILDER
            .comment("雷霆沙子触发特殊效果时最少生成的闪电次数。默认 1。")
            .defineInRange("thunder_sand.min_strikes", 1, 1, 100);

    public static final ModConfigSpec.IntValue THUNDER_SAND_MAX_STRIKES = BUILDER
            .comment("雷霆沙子触发特殊效果时最多生成的闪电次数。默认 10。")
            .defineInRange("thunder_sand.max_strikes", 10, 1, 100);

    public static final ModConfigSpec.IntValue THUNDER_SAND_LIGHTNING_ROD_RANGE = BUILDER
            .comment("雷霆沙子寻找附近避雷针时使用的最大距离，单位：方块。默认 5。")
            .defineInRange("thunder_sand.lightning_rod_range", 5, 1, 64);

    // Kaleidoscope Cookery quality compatibility
    public static final ModConfigSpec.BooleanValue KALEIDOSCOPE_GOLDEN_SELECTION_ENABLED = BUILDER
            .comment("是否启用森罗物语最高品质菜品的金选联动。默认开启。")
            .define("kaleidoscope_cookery.golden_selection_enabled", true);

    public static final ModConfigSpec.DoubleValue GOLDEN_SELECTION_CHANCE = BUILDER
            .comment("森罗物语最高品质菜品成为金选的概率。默认 0.5 = 50%。")
            .defineInRange("kaleidoscope_cookery.golden_selection_chance", 0.5D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue GOLDEN_SELECTION_GLINT_COLOR = BUILDER
            .comment("金选物品附魔光效的 RGB 颜色。默认 0xFFD54A，暖金色。")
            .defineInRange("golden_selection.glint_color", 0xFFD54A, 0, 0xFFFFFF);
    // Golden Selection status effect
    public static final ModConfigSpec.IntValue GOLDEN_SELECTION_DURATION = BUILDER
            .comment("食用金选食物后获得的金选状态效果持续时间，单位：游戏刻。",
                    "默认 1200 = 60 秒。")
            .defineInRange("golden_selection.duration", 1200, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue GOLDEN_SELECTION_SAND_MAX_NUGGETS = BUILDER
            .comment("玩家具有金选状态效果时，破坏沙子类方块额外掉落金粒的最大数量。",
                    "实际数量为 0 到该值之间的均匀随机整数，默认 2，即 0~2。")
            .defineInRange("golden_selection.sand_max_nuggets", 2, 0, 64);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> GOLDEN_SELECTION_EXTRA_SAND_BLOCKS = BUILDER
            .comment("金选状态效果期间，额外视为沙子并掉落金粒的方块 ID 列表。",
                    "格式为 modid:block_id。默认包含沙金工艺自身的所有沙子类方块。",
                    "修改后重新加载即可让后续添加的方块生效。")
            .defineListAllowEmpty("golden_selection.extra_sand_blocks",
                    List.of(
                            "createsandustry:wet_sand",
                            "createsandustry:hardened_sand",
                            "createsandustry:petrified_sand",
                            "createsandustry:volatile_sand",
                            "createsandustry:thunder_sand",
                            "createsandustry:cat_litter",
                            "createsandustry:living_sand"),
                    o -> o instanceof String);
    // PoopSky compatibility
    public static final ModConfigSpec.BooleanValue POOPSKY_ENABLED = BUILDER
            .comment("是否启用 PoopSky 金选联动总开关。仅在安装了 PoopSky 时生效。默认开启。")
            .define("poopsky.enabled", true);

    public static final ModConfigSpec.BooleanValue POOPSKY_FECAL_INCONTINENCE_ENABLED = BUILDER
            .comment("是否启用 PoopSky 失禁效果的粪便替换金粒联动。默认开启。")
            .define("poopsky.fecal_incontinence_enabled", true);

    public static final ModConfigSpec.DoubleValue POOPSKY_FECAL_GOLD_CHANCE = BUILDER
            .comment("PoopSky 失禁效果每次生成粪便时替换为金粒的概率。默认 0.5 = 50%。")
            .defineInRange("poopsky.fecal_gold_chance", 0.5D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue POOPSKY_ON_THE_VERGE_ENABLED = BUILDER
            .comment("是否启用 PoopSky 临界状态效果生成的粪便块替换为金块的联动。默认开启。")
            .define("poopsky.on_the_verge_enabled", true);
    // Falling gravity block drop rules
    public static final ModConfigSpec.BooleanValue NO_DROPS_WHEN_FALLING_BLOCK_DISPLACED = BUILDER
            .comment("控制重力方块在下落过程中，因落点被不完整方块、台阶、火把等不允许其正常放置的方块",
                    "阻挡而无法落地、最终进入原版“将下落方块转化为物品掉落”的处理流程时，是否产生掉落物。",
                    "作用范围：所有使用原版下落机制的方块——沙子、红沙、沙砾、各色混凝土粉末、",
                    "沙金工艺的特殊沙子，以及其它模组的重力方块。",
                    "排除：铁砧、开裂的铁砧、损坏的铁砧（#minecraft:anvil），它们保持原版下落/损坏/掉落行为。",
                    "true：不生成任何物品掉落（直接结束下落方块实体）。",
                    "false：保留原版行为，按原版规则生成物品掉落。",
                    "默认 false。")
            .define("no_drops_when_falling_block_displaced", false);
    // Brushable blocks stay intact while falling
    public static final ModConfigSpec.BooleanValue PREVENT_BRUSHABLE_BLOCKS_BREAKING_FROM_FALLING = BUILDER
            .comment("防止可疑沙子、可疑沙砾和猫砂因重力下落、落地冲击而摔碎。",
                    "开启后，这些可刷方块下落并落地时会保持为原方块，并保留其考古战利品表与刷掘数据（含刷掘阶段），",
                    "不会像原版那样在落地时被直接销毁。",
                    "true：下落后保持完整，不因坠落而摔碎。",
                    "false：恢复原版/模组原有的坠落破坏行为。",
                    "默认 true。",
                    "作用范围仅限本身具有重力下落行为的可刷方块（可疑沙子、可疑沙砾、猫砂及同类方块）；",
                    "普通沙子、沙砾、本模组其它特殊沙子与铁砧的下落/掉落行为不受影响。")
            .define("prevent_brushable_blocks_breaking_from_falling", true);
    // Universal archaeology loot pool
    public static final ModConfigSpec.BooleanValue UNIVERSAL_ARCHAEOLOGY_LOOT = BUILDER
            .comment("是否启用全局通用考古战利品池。",
                    "开启后，可疑沙子与可疑沙砾不再局限于遗迹指定的战利品表，",
                    "而是从当前服务器加载的全部有效考古战利品表中等概率抽取奖励；",
                    "手动放置（未预设 LootTable）的可疑方块也会使用该全局池。",
                    "数据包或战利品表变更后执行 /reload 即会让新增或被覆盖的考古战利品表生效；",
                    "修改本开关后需要重启服务器或重新加载世界。",
                    "多人服务器中以服务器实际生效的配置为准。",
                    "true：启用全局考古战利品池（默认）。",
                    "false：恢复原版及其他模组原有的可疑方块战利品表选择行为。")
            .define("universal_archaeology_loot", true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    private ModConfigs() {
    }
}








