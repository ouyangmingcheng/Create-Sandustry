package com.minechai.create_candustry.block;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.block.crop.AmethystFlowerBlock;
import com.minechai.create_candustry.block.machine.FlorinolBatteryBlock;
import com.minechai.create_candustry.fluid.ModFluids;
import com.simibubi.create.foundation.data.SharedProperties;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateSandustry.MODID);

    public static final DeferredBlock<Block> WET_SAND = BLOCKS.registerSimpleBlock(
            "wet_sand",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SAND));

    public static final DeferredBlock<Block> HARDENED_SAND = BLOCKS.registerSimpleBlock(
            "hardened_sand",
            hardenedSandProperties());

    public static final DeferredBlock<Block> PETRIFIED_SAND = BLOCKS.registerSimpleBlock(
            "petrified_sand",
            petrifiedSandProperties());

    /**
     * 所有沙化矿石。键为不含命名空间的注册路径；顺序为硬化沙子矿石一组、石化沙子矿石一组。
     */
    public static final Map<String, DeferredBlock<Block>> SANDIFIED_ORES = createSandifiedOres();

    public static final DeferredBlock<LeavesBlock> GOLDEN_OAK_LEAVES = BLOCKS.register(
            "golden_oak_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));

    public static final DeferredBlock<GoldenOakSaplingBlock> GOLDEN_OAK_SAPLING = BLOCKS.register(
            "golden_oak_sapling",
            () -> new GoldenOakSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    public static final DeferredBlock<VolatileSandBlock> VOLATILE_SAND = BLOCKS.register(
            "volatile_sand",
            () -> new VolatileSandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<ThunderSandBlock> THUNDER_SAND = BLOCKS.register(
            "thunder_sand",
            () -> new ThunderSandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<CatLitterBlock> CAT_LITTER = BLOCKS.register(
            "cat_litter",
            () -> new CatLitterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<LivingSandBlock> LIVING_SAND = BLOCKS.register(
            "living_sand",
            () -> new LivingSandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<LiquidBlock> FLORINOL = BLOCKS.register(
            "florinol",
            () -> new LiquidBlock(ModFluids.FLORINOL.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

    public static final DeferredBlock<FlorinolBatteryBlock> FLORINOL_BATTERY = BLOCKS.register(
            "florinol_battery",
            () -> FlorinolBatteryBlock.regular(BlockBehaviour.Properties.ofFullCopy(SharedProperties.copperMetal())
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> true)));

    /** 沙噬剑座：古沙漠祭坛风格，可插入完整形态的沙噬剑。 */
    public static final DeferredBlock<SandMasterSwordPedestalBlock> SAND_MASTER_SWORD_PEDESTAL = BLOCKS.register(
            "sand_master_sword_pedestal",
            () -> new SandMasterSwordPedestalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CUT_SANDSTONE)
                    .strength(1.8F, 6.0F)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)));

    public static final DeferredBlock<Block> FLORINOL_SOIL = BLOCKS.registerSimpleBlock(
            "florinol_soil",
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT));
    public static final DeferredBlock<AmethystFlowerBlock> AMETHYST_FLOWER = BLOCKS.register(
            "amethyst_flower",
            () -> new AmethystFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static Map<String, DeferredBlock<Block>> createSandifiedOres() {
        Map<String, DeferredBlock<Block>> ores = new LinkedHashMap<>();
        for (SandifiedOreDefinitions.Entry entry : SandifiedOreDefinitions.ENTRIES) {
            ores.put(entry.id(), BLOCKS.register(entry.id(), () -> createSandifiedOreBlock(entry)));
        }
        return Collections.unmodifiableMap(ores);
    }

    private static Block createSandifiedOreBlock(SandifiedOreDefinitions.Entry entry) {
        BlockBehaviour.Properties properties = (entry.variant().usesDeepslateSource()
                ? petrifiedSandProperties()
                : hardenedSandProperties())
                .requiresCorrectToolForDrops();
        if (entry.ore().redstoneBehavior()) {
            properties.lightLevel(state -> state.getValue(RedStoneOreBlock.LIT) ? 9 : 0);
            return new RedStoneOreBlock(properties);
        }
        return new DropExperienceBlock(
                UniformInt.of(entry.ore().xpMin(), entry.ore().xpMax()), properties);
    }

    /** 硬化沙子及其矿石共用的基底属性。 */
    private static BlockBehaviour.Properties hardenedSandProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.SAND);
    }

    /** 石化沙子及其矿石共用的基底属性。 */
    private static BlockBehaviour.Properties petrifiedSandProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).strength(3.0F, 6.0F);
    }

    private ModBlocks() {
    }
}
