package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.thermalbuffer.ThermalBufferBlockItem;
import com.minechai.create_sandustry.content.sandsword.SandSwordItem;
import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordItem;
import com.minechai.create_sandustry.content.sand.SandBallItem;
import com.minechai.create_sandustry.content.sandheart.SandHeartItem;
import com.minechai.create_sandustry.content.sandheart.GoldenHeartItem;
import com.minechai.create_sandustry.content.materials.MysteriousLumpItem;
import com.minechai.create_sandustry.content.mooncrystal.MoonCrystalItem;
import com.minechai.create_sandustry.registry.ModTiers;
import com.minechai.create_sandustry.content.livingsand.LivingSandItem;
import com.minechai.create_sandustry.content.sandsword.BrokenSandSwordItem;
import com.minechai.create_sandustry.content.sandmastersword.BrokenSandMasterSwordItem;
import com.minechai.create_sandustry.CreateSandustry;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiscFragmentItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateSandustry.MODID);
public static final DeferredItem<BlockItem> GOLDEN_EXPERIENCE_BLOCK = ITEMS.registerItem("golden_experience_block", properties -> new BlockItem(ModBlocks.GOLDEN_EXPERIENCE_BLOCK.get(), properties) {
        @Override
        public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack stack) {
            return super.getName(stack).copy().withStyle(net.minecraft.ChatFormatting.GOLD);
        }
    });
public static final DeferredItem<com.minechai.create_sandustry.content.experience.GoldenExperienceNuggetItem> GOLDEN_EXPERIENCE_NUGGET = ITEMS.registerItem("golden_experience_nugget", com.minechai.create_sandustry.content.experience.GoldenExperienceNuggetItem::new);

    /** 沙合金：普通物品，仅用于物品、纹理与标签。 */
    public static final DeferredItem<Item> SAND_ALLOY = ITEMS.registerSimpleItem("sand_alloy");

    /** 月晶石碎片：恢复 1 点饱食度、不恢复饱和度，并获得月亮崇拜。 */
    public static final DeferredItem<MoonCrystalItem> MOONCRYSTAL = ITEMS.registerItem(
            "mooncrystal",
            properties -> new MoonCrystalItem(properties.food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0.0F)
                    .alwaysEdible()
                    .build())));

    /** 月跃石：恢复 4 点饱食度、6 点饱和度，并给予 90 秒跳跃提升。 */
    public static final DeferredItem<Item> MOONHOP = ITEMS.registerItem(
            "moonhop",
            properties -> new Item(properties.food(new FoodProperties.Builder()
                    .nutrition(4)
                    // 饱和度公式：饱食度 4 × 0.75 × 2 = 6。
                    .saturationModifier(0.75F)
                    .alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.JUMP, 90 * 20, 0), 1.0F)
                    .build())));

    /** 可可跃石：恢复 6 点饱食度，并提供 30 秒跳跃提升、敏捷和力量。 */
    public static final DeferredItem<Item> CHOCOLATE_MOONHOP = ITEMS.registerItem(
            "chocolate_moonhop",
            properties -> new Item(properties.food(new FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.6F)
                    .alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.JUMP, 30 * 20, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 0), 1.0F)
                    .build())));

    /** 月跃石板：使用新母图粉紫渐变动画的完成品。 */
    public static final DeferredItem<Item> MOONHOP_BOARD = ITEMS.registerSimpleItem("moonhop_board");

    public static final DeferredItem<Item> GRAVITY_MECHANISM = ITEMS.registerSimpleItem("gravity_mechanism");

    /** Create supplies the native progress bar; the recipe supplies the native assembly tooltip. */
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_GRAVITY_MECHANISM = ITEMS.registerItem(
            "incomplete_gravity_mechanism", SequencedAssemblyItem::new);

    /** 月跃石块：粉紫渐变动画方块对应的 BlockItem。 */
    public static final DeferredItem<BlockItem> MOONHOP_BLOCK = ITEMS.registerSimpleBlockItem(
            "moonhop_block",
            ModBlocks.MOONHOP_BLOCK);

    /** 未完成月跃石板：机械压机累计高度过程中的不可堆叠中间产物。 */
    public static final DeferredItem<Item> INCOMPLETE_MOONHOP_BOARD = ITEMS.registerItem(
            "incomplete_moonhop_board",
            properties -> new Item(properties.stacksTo(1)));

    public static final DeferredItem<BlockItem> SAND_CASING = ITEMS.registerSimpleBlockItem("sand_casing", ModBlocks.SAND_CASING);
    public static final DeferredItem<BlockItem> WET_SAND = ITEMS.registerSimpleBlockItem("wet_sand", ModBlocks.WET_SAND);
    public static final DeferredItem<BlockItem> HARDENED_SAND = ITEMS.registerSimpleBlockItem("hardened_sand", ModBlocks.HARDENED_SAND);
    public static final DeferredItem<BlockItem> PETRIFIED_SAND = ITEMS.registerSimpleBlockItem("petrified_sand", ModBlocks.PETRIFIED_SAND);
    public static final Map<String, DeferredItem<BlockItem>> SANDIFIED_ORE_ITEMS = createSandifiedOreItems();
    public static final DeferredItem<BlockItem> SAND_MASTER_SWORD_PEDESTAL = ITEMS.registerSimpleBlockItem(
            "sand_master_sword_pedestal",
            ModBlocks.SAND_MASTER_SWORD_PEDESTAL);
    public static final DeferredItem<BlockItem> GOLDEN_OAK_SAPLING = ITEMS.registerSimpleBlockItem("golden_oak_sapling", ModBlocks.GOLDEN_OAK_SAPLING);
    public static final DeferredItem<BlockItem> GOLDEN_OAK_LEAVES = ITEMS.registerSimpleBlockItem("golden_oak_leaves", ModBlocks.GOLDEN_OAK_LEAVES);
    public static final DeferredItem<BlockItem> VOLATILE_SAND = ITEMS.registerSimpleBlockItem("volatile_sand", ModBlocks.VOLATILE_SAND);
    public static final DeferredItem<BlockItem> CAT_LITTER = ITEMS.registerSimpleBlockItem("cat_litter", ModBlocks.CAT_LITTER);
    public static final DeferredItem<BlockItem> THUNDER_SAND = ITEMS.registerSimpleBlockItem("thunder_sand", ModBlocks.THUNDER_SAND);
    public static final DeferredItem<LivingSandItem> LIVING_SAND = ITEMS.registerItem(
            "living_sand",
            properties -> new LivingSandItem(ModBlocks.LIVING_SAND.get(), properties));
    /** 群友刷怪蛋：沿用原版猪刷怪蛋的模型、纹理与颜色。 */
    public static final DeferredItem<DeferredSpawnEggItem> QUNYOU_SPAWN_EGG = ITEMS.registerItem(
            "qunyou_spawn_egg",
            properties -> new DeferredSpawnEggItem(ModEntityTypes.QUNYOU, 0xF0A5A2, 0xDB635F, properties));
    /** 沙团：可食用，恢复随机饱食度，并有 50% 概率施加 3 秒的“沙化”状态。 */
    public static final DeferredItem<SandBallItem> SAND_BALL = ITEMS.registerItem(
            "sand_ball",
            properties -> new SandBallItem(properties.food(new FoodProperties.Builder()
                    .nutrition(0)
                    .saturationModifier(0.0F)
                    // 满饱食度时也能食用（沙团还会施加“沙化”状态）。
                    .alwaysEdible()
                    .build())));

    /** 沙之心：右键使用后立刻增加一颗完整沙心。 */
    public static final DeferredItem<SandHeartItem> SAND_HEART = ITEMS.registerItem(
            "sand_heart",
            SandHeartItem::new);

    /** 金之心：右键使用后获得一颗无持续时间的伤害吸收心。 */
    public static final DeferredItem<GoldenHeartItem> GOLDEN_HEART = ITEMS.registerItem(
            "golden_heart",
            GoldenHeartItem::new);

    /** 火箭土豆：机械动力土豆加农炮的高速弹药。 */
    public static final DeferredItem<com.minechai.create_sandustry.content.potatorocket.RocketPotatoItem> POTATO_ROCKET =
            ITEMS.registerItem("potato_rocket", properties ->
                    new com.minechai.create_sandustry.content.potatorocket.RocketPotatoItem(
                            properties.food(net.minecraft.world.food.Foods.BAKED_POTATO)));

    /** 沙噬剑（完整形态）：攻击力 3 点，命中施加沙化，耐久耗尽后变为破损形态。 */
    public static final DeferredItem<SandMasterSwordItem> SAND_MASTER_SWORD = ITEMS.registerItem(
            "sand_master_sword",
            properties -> new SandMasterSwordItem(ModTiers.SAND_MASTER_SWORD,
                    SandMasterSwordItem.applyProperties(properties)));

    /** 沙噬剑（破损形态）：不能作为武器，放在背包里会随时间自动修复（不需要材料）。 */
    public static final DeferredItem<BrokenSandMasterSwordItem> SAND_MASTER_SWORD_BROKEN = ITEMS.registerItem(
            "sand_master_sword_broken",
            properties -> new BrokenSandMasterSwordItem(BrokenSandMasterSwordItem.applyProperties(properties)));

    /** 沙金剑（完整形态）：攻击力 3 点、攻击速度 1.6、最大耐久 40。 */
    public static final DeferredItem<SandSwordItem> SAND_SWORD = ITEMS.registerItem(
            "sand_sword",
            properties -> new SandSwordItem(ModTiers.SAND_SWORD, SandSwordItem.applyProperties(properties)));

    /** Opening 谜题唱片：可放入原版唱片机，并由 MelodyPuzzleManager 接管谜题流程。 */
    public static final DeferredItem<Item> MUSIC_DISC_OPENING = ITEMS.registerItem(
            "music_disc_opening",
            properties -> new Item(properties
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .jukeboxPlayable(ModJukeboxSongs.OPENING_CLUE)
                    .component(ModDataComponents.MELODY_PUZZLE_ID.get(), "opening")));

    /** Song of Awakening 谜题唱片。 */
    public static final DeferredItem<Item> MUSIC_DISC_AWAKENING = ITEMS.registerItem(
            "music_disc_awakening",
            properties -> new Item(properties
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .jukeboxPlayable(ModJukeboxSongs.AWAKENING_CLUE)
                    .component(ModDataComponents.MELODY_PUZZLE_ID.get(), "awakening")));

    /** 唱片残片：所有曲目共用 disc_fragment 纹理，通过描述区分曲名。 */
    public static final DeferredItem<DiscFragmentItem> DISC_FRAGMENT_AWAKENING = ITEMS.registerItem(
            "disc_fragment_awakening",
            DiscFragmentItem::new);

    /** Song of Healing 谜题唱片。 */
    public static final DeferredItem<Item> MUSIC_DISC_HEALING = ITEMS.registerItem(
            "music_disc_healing",
            properties -> new Item(properties
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .jukeboxPlayable(ModJukeboxSongs.HEALING_CLUE)
                    .component(ModDataComponents.MELODY_PUZZLE_ID.get(), "healing")));

    /** Song of Healing 唱片残片。 */
    public static final DeferredItem<DiscFragmentItem> DISC_FRAGMENT_HEALING = ITEMS.registerItem(
            "disc_fragment_healing",
            DiscFragmentItem::new);

    /** Song of Birds 谜题唱片。 */
    public static final DeferredItem<Item> MUSIC_DISC_BIRDS = ITEMS.registerItem(
            "music_disc_birds",
            properties -> new Item(properties
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .jukeboxPlayable(ModJukeboxSongs.BIRDS_CLUE)
                    .component(ModDataComponents.MELODY_PUZZLE_ID.get(), "birds")));

    /** Song of Birds 唱片残片。 */
    public static final DeferredItem<DiscFragmentItem> DISC_FRAGMENT_BIRDS = ITEMS.registerItem(
            "disc_fragment_birds",
            DiscFragmentItem::new);

    /** Wy0000-BigNortheastIsMyHomeland 谜题唱片，使用兔子脚纹理和原版附魔光效。 */
    public static final DeferredItem<Item> MUSIC_DISC_BIG_NORTHEAST = ITEMS.registerItem(
            "music_disc_bignortheastismyhomeland",
            properties -> new Item(properties
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .jukeboxPlayable(ModJukeboxSongs.BIG_NORTHEAST_CLUE)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                    .component(ModDataComponents.MELODY_PUZZLE_ID.get(), "bignortheastismyhomeland")));
    /** 沙金剑（破损形态）：不能作为武器，只能与沙团无序合成恢复耐久。 */
    public static final DeferredItem<BrokenSandSwordItem> SAND_SWORD_BROKEN = ITEMS.registerItem(
            "sand_sword_broken",
            properties -> new BrokenSandSwordItem(BrokenSandSwordItem.applyProperties(properties)));

    /** 沙之魂：暂无用途的独立物品。 */
    public static final DeferredItem<Item> SAND_SOUL = ITEMS.registerSimpleItem("sand_soul");
    public static final DeferredItem<Item> RESIDUE = ITEMS.registerSimpleItem("residue");
    public static final DeferredItem<MysteriousLumpItem> MYSTERIOUS_LUMP = ITEMS.registerItem("mysterious_lump", MysteriousLumpItem::new);
    public static final DeferredItem<BucketItem> FLORINOL_BUCKET = ITEMS.registerItem(
            "florinol_bucket",
            properties -> new BucketItem(ModFluids.FLORINOL.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<Item> BURNT_RESIDUE = ITEMS.registerSimpleItem("burnt_residue");
    public static final DeferredItem<BlockItem> FLORINOL_BATTERY = ITEMS.registerSimpleBlockItem(
            "florinol_battery",
            ModBlocks.FLORINOL_BATTERY);
    public static final DeferredItem<BlockItem> KINETIC_PRESS = ITEMS.registerSimpleBlockItem(
            "kinetic_press",
            ModBlocks.KINETIC_PRESS);
    public static final DeferredItem<BlockItem> FLORINOL_SOIL = ITEMS.registerSimpleBlockItem(
            "florinol_soil",
            ModBlocks.FLORINOL_SOIL);
    public static final DeferredItem<ThermalBufferBlockItem> THERMAL_RELAY = ITEMS.registerItem(
            "thermal_relay",
            properties -> new ThermalBufferBlockItem(ModBlocks.THERMAL_RELAY.get(), properties));
    public static final DeferredItem<ItemNameBlockItem> AMETHYST_FLOWER_SEEDS = ITEMS.registerItem(
            "amethyst_flower_seeds",
            properties -> new ItemNameBlockItem(ModBlocks.AMETHYST_FLOWER.get(), properties));
    public static final DeferredItem<Item> AMETHYST_PETAL = ITEMS.registerSimpleItem("amethyst_petal");
    public static final DeferredItem<BlockItem> AMETHYST_FLOWER = ITEMS.registerSimpleBlockItem(
            "amethyst_flower",
            ModBlocks.AMETHYST_FLOWER);

    public static final DeferredItem<BlockItem> GOLDEN_DIRT = ITEMS.registerSimpleBlockItem(
            "golden_dirt",
            ModBlocks.GOLDEN_DIRT);
    public static final DeferredItem<BlockItem> GOLDEN_FARMLAND = ITEMS.registerSimpleBlockItem(
            "golden_farmland",
            ModBlocks.GOLDEN_FARMLAND);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static Map<String, DeferredItem<BlockItem>> createSandifiedOreItems() {
        Map<String, DeferredItem<BlockItem>> items = new LinkedHashMap<>();
        ModBlocks.SANDIFIED_ORES.forEach((id, block) ->
                items.put(id, ITEMS.registerSimpleBlockItem(id, block)));
        return Collections.unmodifiableMap(items);
    }

    private ModItems() {
    }
}

