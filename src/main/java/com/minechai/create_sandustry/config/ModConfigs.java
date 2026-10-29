package com.minechai.create_sandustry.config;

import java.util.List;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfigs {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Florinol Battery
    public static final ModConfigSpec.IntValue FLORINOL_BATTERY_FE_PER_MB = BUILDER
            .comment("每 1 mB 弗洛林电池液可以提供多少 FE 能量。",
                    "默认 1，表示 1 mB Florinol = 1 FE，1 B = 1 kFE。",
                    "多方块电池以“格”为单位放电：1 格 = 1000 mB。")
            .defineInRange("florinol_battery.fe_per_mb", 1, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FLORINOL_BATTERY_MB_PER_SOIL = BUILDER
            .comment("生成 1 个弗洛林土壤需要消耗多少 mB 弗洛林电池液。",
                    "默认 1000 mB，与“1 格 = 1000 mB”一致，故 1 格弗洛林液 = 1 个弗洛林土壤。")
            .defineInRange("florinol_battery.mb_per_soil", 1000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FLORINOL_BATTERY_SCAN_INTERVAL = BUILDER
            .comment("总控方块周期性重新校验多方块结构的间隔，单位：tick。",
                    "默认 20（约每秒 1 次）。")
            .defineInRange("florinol_battery.scan_interval", 20, 1, Integer.MAX_VALUE);

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
    // Qunyou phase-three pig ejection
    public static final ModConfigSpec.IntValue QUNYOU_PIG_EJECTION_COOLDOWN = BUILDER
            .comment("第三阶段“猪弹射”的独立冷却，单位：tick。默认 800 = 40 秒。")
            .defineInRange("qunyou.pig_ejection.cooldown", 800, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_EJECTION_CHARGE_TICKS = BUILDER
            .comment("猪弹射蓄力时间，单位：tick。默认 12。")
            .defineInRange("qunyou.pig_ejection.charge_ticks", 12, 1, 100);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_EJECTION_BOOST_STAGES = BUILDER
            .comment("猪弹射包含的烟花助推段数。默认 3。")
            .defineInRange("qunyou.pig_ejection.boost_stages", 3, 1, 10);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_EJECTION_BOOST_INTERVAL = BUILDER
            .comment("猪弹射相邻助推段之间的间隔，单位：tick。默认 6。")
            .defineInRange("qunyou.pig_ejection.boost_interval", 6, 1, 100);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_INITIAL_SPEED = BUILDER
            .comment("猪弹射第一段助推后的水平速度，单位：格/tick。默认 1.35。")
            .defineInRange("qunyou.pig_ejection.initial_speed", 1.35D, 0.1D, 10.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_ACCELERATION = BUILDER
            .comment("每段后续烟花助推增加的水平速度。默认 0.35。")
            .defineInRange("qunyou.pig_ejection.acceleration", 0.35D, 0.0D, 10.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_MAX_SPEED = BUILDER
            .comment("猪弹射最大水平速度。默认 2.6。")
            .defineInRange("qunyou.pig_ejection.max_speed", 2.6D, 0.1D, 20.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_MAX_DISTANCE = BUILDER
            .comment("猪弹射单次冲撞的最大水平距离。默认 28 格。")
            .defineInRange("qunyou.pig_ejection.max_distance", 28.0D, 1.0D, 256.0D);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_EJECTION_MAX_TICKS = BUILDER
            .comment("猪弹射单次冲撞的最长时间，单位：tick。默认 45。")
            .defineInRange("qunyou.pig_ejection.max_ticks", 45, 1, 400);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_TURN_LERP = BUILDER
            .comment("猪弹射每 tick 允许朝目标修正方向的比例。默认 0.055。")
            .defineInRange("qunyou.pig_ejection.turn_lerp", 0.055D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_DAMAGE = BUILDER
            .comment("猪弹射命中伤害。默认 8 点。")
            .defineInRange("qunyou.pig_ejection.damage", 8.0D, 0.0D, 1024.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_KNOCKBACK = BUILDER
            .comment("猪弹射命中时的水平击退强度。默认 1.6。")
            .defineInRange("qunyou.pig_ejection.knockback", 1.6D, 0.0D, 10.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_UPWARD = BUILDER
            .comment("猪弹射命中时的向上击飞强度。默认 0.8。")
            .defineInRange("qunyou.pig_ejection.upward", 0.8D, 0.0D, 5.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_CHASE_RANGE = BUILDER
            .comment("猪弹射命中玩家后继续追击的最大目标距离。默认 48 格。")
            .defineInRange("qunyou.pig_ejection.chase_range", 48.0D, 1.0D, 256.0D);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_EJECTION_MAX_CHASE_HITS = BUILDER
            .comment("玩家被连续命中并刷新猪弹射时的最大命中次数。默认 3。")
            .defineInRange("qunyou.pig_ejection.max_chase_hits", 3, 1, 20);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_EJECTION_CHASE_RECOVERY_TICKS = BUILDER
            .comment("命中玩家后再次蓄力前的最短间隔，单位：tick。默认 10。")
            .defineInRange("qunyou.pig_ejection.chase_recovery_ticks", 10, 1, 100);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_ASCEND_ANGLE = BUILDER
            .comment("未命中时斜向升空的仰角，单位：度。默认 42。")
            .defineInRange("qunyou.pig_ejection.ascend_angle", 42.0D, 0.0D, 85.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_ASCEND_HEIGHT = BUILDER
            .comment("未命中时猪弹射升空的高度增量。默认 10 格。")
            .defineInRange("qunyou.pig_ejection.ascend_height", 10.0D, 1.0D, 64.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_EJECTION_ASCEND_SPEED = BUILDER
            .comment("猪弹射斜向升空的初速度。默认 1.0。")
            .defineInRange("qunyou.pig_ejection.ascend_speed", 1.0D, 0.1D, 10.0D);

    // Qunyou phase-three golden carrot missiles
    public static final ModConfigSpec.IntValue QUNYOU_PIG_MISSILE_COOLDOWN = BUILDER
            .comment("猪导弹独立冷却，单位：tick。默认 80 = 4 秒。")
            .defineInRange("qunyou.pig_missile.cooldown", 80, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_MISSILE_COUNT = BUILDER
            .comment("每次猪导弹召唤数量。默认 3。")
            .defineInRange("qunyou.pig_missile.count", 3, 1, 12);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_MISSILE_LAUNCH_INTERVAL = BUILDER
            .comment("三枚黄金胡萝卜之间的发射间隔，单位：tick。默认 4。")
            .defineInRange("qunyou.pig_missile.launch_interval", 4, 1, 100);
    public static final ModConfigSpec.DoubleValue QUNYOU_GOLDEN_CARROT_SPEED_RATIO = BUILDER
            .comment("黄金胡萝卜速度相对普通胡萝卜的速度比例。默认 0.42。")
            .defineInRange("qunyou.pig_missile.speed_ratio", 0.42D, 0.1D, 1.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_GOLDEN_CARROT_TURN_LERP = BUILDER
            .comment("黄金胡萝卜每 tick 追踪转向插值。默认 0.12。")
            .defineInRange("qunyou.pig_missile.turn_lerp", 0.12D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue QUNYOU_GOLDEN_CARROT_PREDICTION_TICKS = BUILDER
            .comment("黄金胡萝卜追踪目标时使用的前置预测 tick 数。默认 8。")
            .defineInRange("qunyou.pig_missile.prediction_ticks", 8, 0, 100);
    public static final ModConfigSpec.DoubleValue QUNYOU_GOLDEN_CARROT_MAX_TRACK_RANGE = BUILDER
            .comment("黄金胡萝卜最大持续追踪距离。默认 64 格。")
            .defineInRange("qunyou.pig_missile.max_track_range", 64.0D, 1.0D, 256.0D);
    public static final ModConfigSpec.IntValue QUNYOU_GOLDEN_CARROT_TRAIL_INTERVAL = BUILDER
            .comment("黄金胡萝卜火焰拖尾粒子生成间隔，单位：tick。默认 2。")
            .defineInRange("qunyou.pig_missile.trail_interval", 2, 1, 20);
    public static final ModConfigSpec.DoubleValue QUNYOU_GOLDEN_CARROT_EXPLOSION_RADIUS = BUILDER
            .comment("黄金胡萝卜非破坏性小爆炸半径。默认 3 格。")
            .defineInRange("qunyou.pig_missile.explosion_radius", 3.0D, 0.1D, 12.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_GOLDEN_CARROT_DAMAGE_MULTIPLIER = BUILDER
            .comment("黄金胡萝卜对直接命中目标的伤害倍率，以普通胡萝卜伤害为基准。默认 2.0。")
            .defineInRange("qunyou.pig_missile.damage_multiplier", 2.0D, 1.0D, 10.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_GOLDEN_CARROT_EXPLOSION_DAMAGE = BUILDER
            .comment("黄金胡萝卜爆炸对周围其他有效目标的中心伤害。默认 6 点，边缘衰减至 0。")
            .defineInRange("qunyou.pig_missile.explosion_damage", 6.0D, 0.0D, 1024.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_GOLDEN_CARROT_EXPLOSION_KNOCKBACK = BUILDER
            .comment("黄金胡萝卜爆炸击退强度。默认 0.65。")
            .defineInRange("qunyou.pig_missile.explosion_knockback", 0.65D, 0.0D, 10.0D);
    public static final ModConfigSpec.BooleanValue QUNYOU_GOLDEN_CARROT_AFFECT_OTHER_TARGETS = BUILDER
            .comment("黄金胡萝卜爆炸是否伤害周围其他有效目标。默认开启。")
            .define("qunyou.pig_missile.affect_other_targets", true);

    // Qunyou phase-three pig retreat
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_RETREAT_REPLACE_CHANCE = BUILDER
            .comment("第三阶段准备执行猪逃离时替换为猪撤退的概率。默认 0.5 = 50%。")
            .defineInRange("qunyou.pig_retreat.replace_chance", 0.5D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_RETREAT_CHARGE_TICKS = BUILDER
            .comment("猪撤退蓄力时间，单位：tick。默认 6。")
            .defineInRange("qunyou.pig_retreat.charge_ticks", 6, 1, 100);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_RETREAT_HORIZONTAL_TICKS = BUILDER
            .comment("猪撤退水平助推持续时间，单位：tick。默认 14。")
            .defineInRange("qunyou.pig_retreat.horizontal_ticks", 14, 1, 100);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_RETREAT_HORIZONTAL_SPEED = BUILDER
            .comment("猪撤退水平助推初速度。默认 1.1。")
            .defineInRange("qunyou.pig_retreat.horizontal_speed", 1.1D, 0.1D, 10.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_RETREAT_ACCELERATION = BUILDER
            .comment("猪撤退水平助推每 tick 加速度。默认 0.05。")
            .defineInRange("qunyou.pig_retreat.acceleration", 0.05D, 0.0D, 10.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_RETREAT_DISTANCE = BUILDER
            .comment("猪撤退水平段目标逃离距离。默认 12 格。")
            .defineInRange("qunyou.pig_retreat.distance", 12.0D, 1.0D, 64.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_RETREAT_ASCEND_ANGLE = BUILDER
            .comment("猪撤退第二段斜向升空角度，单位：度。默认 50。")
            .defineInRange("qunyou.pig_retreat.ascend_angle", 50.0D, 0.0D, 85.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_RETREAT_ASCEND_HEIGHT = BUILDER
            .comment("猪撤退斜向升空的高度上限。默认 10 格。")
            .defineInRange("qunyou.pig_retreat.ascend_height", 10.0D, 1.0D, 64.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_RETREAT_ASCEND_SPEED = BUILDER
            .comment("猪撤退斜向升空速度。默认 1.0。")
            .defineInRange("qunyou.pig_retreat.ascend_speed", 1.0D, 0.1D, 10.0D);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_RETREAT_BURST_PARTICLES = BUILDER
            .comment("猪撤退每次助推爆发的烟花粒子数量。默认 24。")
            .defineInRange("qunyou.pig_retreat.burst_particles", 24, 1, 256);

    // Qunyou phase-three air maneuver
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_MANEUVER_CHANCE = BUILDER
            .comment("第三阶段滑翔时每次判定触发猪机动的概率。默认 0.30。")
            .defineInRange("qunyou.pig_maneuver.chance", 0.30D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_MANEUVER_CHECK_INTERVAL = BUILDER
            .comment("猪机动概率判定间隔，单位：tick。默认 40。")
            .defineInRange("qunyou.pig_maneuver.check_interval", 40, 1, 1200);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_MANEUVER_COOLDOWN = BUILDER
            .comment("猪机动独立冷却，单位：tick。默认 240 = 12 秒。")
            .defineInRange("qunyou.pig_maneuver.cooldown", 240, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_MANEUVER_DURATION = BUILDER
            .comment("猪机动完整滚转和爬升的持续时间，单位：tick。默认 20。")
            .defineInRange("qunyou.pig_maneuver.duration", 20, 4, 100);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_MANEUVER_CLIMB_HEIGHT = BUILDER
            .comment("猪机动期望爬升高度。默认 6 格。")
            .defineInRange("qunyou.pig_maneuver.climb_height", 6.0D, 1.0D, 32.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_MANEUVER_HORIZONTAL_SPEED = BUILDER
            .comment("猪机动期间维持的水平速度。默认 0.75。")
            .defineInRange("qunyou.pig_maneuver.horizontal_speed", 0.75D, 0.0D, 10.0D);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_MANEUVER_VERTICAL_SPEED = BUILDER
            .comment("猪机动期间爬升速度倍率。默认 1.0。")
            .defineInRange("qunyou.pig_maneuver.vertical_speed_multiplier", 1.0D, 0.1D, 5.0D);

    // Qunyou phase-three airborne eating
    public static final ModConfigSpec.DoubleValue QUNYOU_AIR_EATING_CHANCE = BUILDER
            .comment("第三阶段滑翔时每次判定触发猪空中进食的概率。默认 0.25。")
            .defineInRange("qunyou.air_eating.chance", 0.25D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue QUNYOU_AIR_EATING_CHECK_INTERVAL = BUILDER
            .comment("猪空中进食概率判定间隔，单位：tick。默认 60。")
            .defineInRange("qunyou.air_eating.check_interval", 60, 1, 1200);
    public static final ModConfigSpec.IntValue QUNYOU_AIR_EATING_COOLDOWN = BUILDER
            .comment("猪空中进食独立冷却，单位：tick。默认 500 = 25 秒。")
            .defineInRange("qunyou.air_eating.cooldown", 500, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue QUNYOU_AIR_EATING_DURATION = BUILDER
            .comment("猪空中进食持续时间，单位：tick。默认 36。")
            .defineInRange("qunyou.air_eating.duration", 36, 1, 200);
    public static final ModConfigSpec.IntValue QUNYOU_AIR_EATING_STRENGTH_DURATION = BUILDER
            .comment("空中进食完成后的力量 V 持续时间，单位：tick。默认 400 = 20 秒。")
            .defineInRange("qunyou.air_eating.strength_duration", 400, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue QUNYOU_AIR_EATING_REGENERATION_DURATION = BUILDER
            .comment("空中进食完成后的生命恢复 III 持续时间，单位：tick。默认 100 = 5 秒。")
            .defineInRange("qunyou.air_eating.regeneration_duration", 100, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue QUNYOU_AIR_EATING_TURN_LERP = BUILDER
            .comment("空中进食期间的飞行转向幅度。默认 0.065，低于正常滑翔。")
            .defineInRange("qunyou.air_eating.turn_lerp", 0.065D, 0.0D, 1.0D);

    // Qunyou unified escape
    public static final ModConfigSpec.DoubleValue QUNYOU_ESCAPE_ON_HURT_CHANCE = BUILDER
            .comment("群友受到实际伤害且处于地面时，以该概率立即进入逃离。默认 0.3 = 30%。")
            .defineInRange("qunyou.escape.on_hurt_chance", 0.3D, 0.0D, 1.0D);

    // Qunyou pig clone
    public static final ModConfigSpec.BooleanValue QUNYOU_PIG_CLONE_ENABLED = BUILDER
            .comment("是否启用群友真正进入猪逃离时召唤猪分身。默认开启。")
            .define("qunyou.pig_clone.enabled", true);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_CLONE_CHANCE = BUILDER
            .comment("真正开始猪逃离时召唤分身的概率。默认 0.5 = 50%。")
            .defineInRange("qunyou.pig_clone.chance", 0.5D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue QUNYOU_PIG_CLONE_COUNT = BUILDER
            .comment("单次召唤只新增 1 只分身；同时存活上限为 4，达到上限后再次召唤会传送已有分身。")
            .defineInRange("qunyou.pig_clone.count", 1, 1, 1);
    public static final ModConfigSpec.DoubleValue QUNYOU_PIG_CLONE_MAX_HEALTH = BUILDER
            .comment("猪分身最大生命值。默认 20 点，即 10 颗心。")
            .defineInRange("qunyou.pig_clone.max_health", 20.0D, 1.0D, 1024.0D);

    // Moon Crystal
    public static final ModConfigSpec.DoubleValue MOONCRYSTAL_CONVERSION_CHANCE = BUILDER
            .comment("从含水置物台或溜槽取出海晶碎片时，每个海晶碎片转化为月晶石碎片的概率。",
                    "仅在主世界 17500-18500 tick、天气晴朗且方块能看到天空时生效。",
                    "默认 0.25 = 25%。")
            .defineInRange("mooncrystal_conversion.chance", 0.25D, 0.0D, 1.0D);

    // Moon Worship
    public static final ModConfigSpec.IntValue MOON_WORSHIP_JUMPS_REQUIRED = BUILDER
            .comment("月亮崇拜需要完成的有效跳跃次数。默认 3，方便测试。")
            .defineInRange("moon_worship.jumps_required", 3, 1, 1000);

    public static final ModConfigSpec.IntValue MOON_WORSHIP_DURATION_SECONDS = BUILDER
            .comment("月亮崇拜持续时间，单位：秒。默认 180 = 3 分钟。")
            .defineInRange("moon_worship.duration_seconds", 180, 1, 3600);

    public static final ModConfigSpec.DoubleValue MOON_WORSHIP_PLAYER_REWARD_CHANCE = BUILDER
            .comment("玩家跳满次数后获得 1 个月跃石的概率。默认 1.0 = 100%。")
            .defineInRange("moon_worship.player_reward_chance", 1.0D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue MOON_WORSHIP_RABBIT_REWARD_CHANCE = BUILDER
            .comment("兔子跳满次数后掉落 1 个月跃石的概率。默认 0.5 = 50%。")
            .defineInRange("moon_worship.rabbit_reward_chance", 0.5D, 0.0D, 1.0D);

    // Sifting belt
    public static final ModConfigSpec.DoubleValue SIFTING_PROGRESS_JITTER = BUILDER
            .comment("筛振传送带的单件物品处理时间随机浮动比例。",
                    "实际阈值 = 配方 processing_time × [1 - jitter, 1 + jitter]。",
                    "默认 0.2，即实际处理时间在 80% 到 120% 之间随机。")
            .defineInRange("sifting.progress_jitter", 0.2D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue SIFTING_BRASS_PROGRESS_PER_TICK = BUILDER
            .comment("黄铜机壳筛振段每 tick 推进的处理进度。",
                    "安山机壳固定为 1；默认黄铜为 2，即黄铜机壳处理速度约为安山机壳的 2 倍。")
            .defineInRange("sifting.brass_progress_per_tick", 2, 1, 16);

    // Clockwork bearing time lock
    public static final ModConfigSpec.BooleanValue TIME_BEARING_ENABLED = BUILDER
            .comment("是否启用时钟轴承门控式时间锁。",
                    "启用后，世界默认停止昼夜时间流逝；已组装且获得动力的原版时钟轴承可以解除时间锁。",
                    "机械轴承不再参与时间控制。默认关闭以保持原版行为。")
            .define("time_bearing.enabled", false);

    public static final ModConfigSpec.BooleanValue TIME_BEARING_ALLOW_SLEEP_TIME_SKIP = BUILDER
            .comment("启用时间锁后，是否仍允许玩家通过睡觉跳过夜晚或雷雨。",
                    "开启时即使 doDaylightCycle=false，睡觉仍会进入次日清晨并结束雷雨。",
                    "默认开启。关闭后，睡觉不会改变世界时间。")
            .define("time_bearing.allow_sleep_time_skip", true);

    public static final ModConfigSpec.IntValue TIME_BEARING_MINIMUM_NETWORK_CAPACITY = BUILDER
            .comment("驱动时间锁所需的最低动力网络总应力容量，单位：SU。",
                    "默认 500000。低于此容量时，即使时钟轴承已组装并有转速，时间仍保持锁定。")
            .defineInRange("time_bearing.minimum_network_capacity", 500000, 0, 100_000_000);

    public static final ModConfigSpec.IntValue TIME_BEARING_RPM_DRIVEN_MINIMUM_NETWORK_CAPACITY = BUILDER
            .comment("切换到输入转速驱动的网络总应力容量阈值，单位：SU。",
                    "默认 4000000。容量严格超过此值时，昼夜时间速度改为 abs(输入 RPM) × 倍率。",
                    "容量不超过此值时仍使用固定速率。")
            .defineInRange("time_bearing.rpm_driven_minimum_network_capacity", 4_000_000, 0, 100_000_000);

    public static final ModConfigSpec.DoubleValue TIME_BEARING_RPM_DAY_TIME_MULTIPLIER = BUILDER
            .comment("输入转速驱动模式下，每 1 RPM 对应的昼夜时间刻/tick。",
                    "默认 1.0：32 RPM = 每游戏刻推进 32 昼夜时间刻。",
                    "该倍率只在网络容量严格超过 RPM 驱动阈值时生效。")
            .defineInRange("time_bearing.rpm_day_time_multiplier", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue TIME_BEARING_UNLOCKED_DAY_TIME_PER_TICK = BUILDER
            .comment("时间锁解除后每个游戏刻推进的昼夜时间刻数。",
                    "默认 1.0，等同于原版时间流逝速度；设置为 2.0 表示两倍速。",
                    "该速率只在时钟轴承满足解锁条件时生效。")
            .defineInRange("time_bearing.unlocked_day_time_per_tick", 1.0D, 0.0D, 24000.0D);

    public static final ModConfigSpec.IntValue TIME_BEARING_INITIAL_DAY_TIME = BUILDER
            .comment("启用时间锁后首次加载世界时设置的日内时间，单位：tick。",
                    "默认 6000，即中午 12:00；0 = 06:00，12000 = 18:00，18000 = 00:00。")
            .defineInRange("time_bearing.initial_day_time", 6000, 0, 23999);

    public static final ModConfigSpec.BooleanValue TIME_BEARING_APPLY_INITIAL_DAY_TIME_ONCE = BUILDER
            .comment("是否在启用时间锁后的首次世界加载时应用默认日内时间。默认开启。",
                    "该标记按维度持久化，后续服务器重启不会再次把时间重置为正午。")
            .define("time_bearing.apply_initial_day_time_once", true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void register() {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    private ModConfigs() {
    }
}
