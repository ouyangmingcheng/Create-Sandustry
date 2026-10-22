package com.minechai.create_candustry.item;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.block.ModBlocks;
import com.minechai.create_candustry.fluid.ModFluids;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateSandustry.MODID);

    public static final DeferredItem<BlockItem> WET_SAND = ITEMS.registerSimpleBlockItem("wet_sand", ModBlocks.WET_SAND);
    public static final DeferredItem<BlockItem> HARDENED_SAND = ITEMS.registerSimpleBlockItem("hardened_sand", ModBlocks.HARDENED_SAND);
    public static final DeferredItem<BlockItem> PETRIFIED_SAND = ITEMS.registerSimpleBlockItem("petrified_sand", ModBlocks.PETRIFIED_SAND);
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
    /** 沙团：可食用，恢复随机饱食度，并有 50% 概率施加 3 秒的“沙化”状态。 */
    public static final DeferredItem<SandBallItem> SAND_BALL = ITEMS.registerItem(
            "sand_ball",
            properties -> new SandBallItem(properties.food(new FoodProperties.Builder()
                    .nutrition(0)
                    .saturationModifier(0.0F)
                    // 满饱食度时也能食用（沙团还会施加“沙化”状态）。
                    .alwaysEdible()
                    .build())));

    /** 沙噬剑（完整形态）：攻击力 3 点，命中施加沙化，耐久耗尽后变为破损形态。 */
    public static final DeferredItem<SandMasterSwordItem> SAND_MASTER_SWORD = ITEMS.registerItem(
            "sand_master_sword",
            properties -> new SandMasterSwordItem(ModTiers.SAND_MASTER_SWORD,
                    SandMasterSwordItem.applyProperties(properties)));

    /** 沙噬剑（破损形态）：不能作为武器，放在背包里会随时间自动修复（不需要材料）。 */
    public static final DeferredItem<BrokenSandMasterSwordItem> SAND_MASTER_SWORD_BROKEN = ITEMS.registerItem(
            "sand_master_sword_broken",
            properties -> new BrokenSandMasterSwordItem(BrokenSandMasterSwordItem.applyProperties(properties)));
    /** 沙之魂：暂无用途的独立物品。 */
    public static final DeferredItem<Item> SAND_SOUL = ITEMS.registerSimpleItem("sand_soul");
    public static final DeferredItem<Item> RESIDUE = ITEMS.registerSimpleItem("residue");
    public static final DeferredItem<MysteriousLumpItem> MYSTERIOUS_LUMP = ITEMS.registerItem("mysterious_lump", MysteriousLumpItem::new);
    public static final DeferredItem<BucketItem> FLORINOL_BUCKET = ITEMS.registerItem(
            "florinol_bucket",
            properties -> new BucketItem(ModFluids.FLORINOL.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<Item> BURNT_RESIDUE = ITEMS.registerSimpleItem("burnt_residue");
    public static final DeferredItem<FlorinolBatteryItem> FLORINOL_BATTERY = ITEMS.registerItem(
            "florinol_battery",
            properties -> new FlorinolBatteryItem(ModBlocks.FLORINOL_BATTERY.get(), properties));
    public static final DeferredItem<BlockItem> FLORINOL_SOIL = ITEMS.registerSimpleBlockItem(
            "florinol_soil",
            ModBlocks.FLORINOL_SOIL);
    public static final DeferredItem<ItemNameBlockItem> AMETHYST_FLOWER_SEEDS = ITEMS.registerItem(
            "amethyst_flower_seeds",
            properties -> new ItemNameBlockItem(ModBlocks.AMETHYST_FLOWER.get(), properties));
    public static final DeferredItem<Item> DORMANT_SEEDS = ITEMS.registerSimpleItem("dormant_seeds");
    public static final DeferredItem<Item> AMETHYST_PETAL = ITEMS.registerSimpleItem("amethyst_petal");
    public static final DeferredItem<BlockItem> AMETHYST_FLOWER = ITEMS.registerSimpleBlockItem(
            "amethyst_flower",
            ModBlocks.AMETHYST_FLOWER);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private ModItems() {
    }
}





