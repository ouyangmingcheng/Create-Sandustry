package com.minechai.create_sandustry.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.minechai.create_sandustry.content.block.VolatileSandBlock;
import com.minechai.create_sandustry.content.block.ThunderSandBlock;
import com.minechai.create_sandustry.content.block.WetSandBlock;
import com.minechai.create_sandustry.content.block.SandMasterSwordPedestalBlock;
import com.minechai.create_sandustry.content.block.SandifiedOreDefinitions;
import com.minechai.create_sandustry.content.block.LivingSandBlock;
import com.minechai.create_sandustry.content.block.GoldenOakSaplingBlock;
import com.minechai.create_sandustry.content.block.FlorinolSoilBlock;
import com.minechai.create_sandustry.content.block.CatLitterBlock;
import com.minechai.create_sandustry.content.block.GoldenDirtBlock;
import com.minechai.create_sandustry.content.block.GoldenFarmlandBlock;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.block.crop.AmethystFlowerBlock;
import com.minechai.create_sandustry.content.block.crop.GoldenCarrotCropBlock;
import com.minechai.create_sandustry.content.block.machine.FlorinolBatteryBlock;
import com.minechai.create_sandustry.content.block.machine.ThermalBufferBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.foundation.data.SharedProperties;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateSandustry.MODID);

    /** 沙金机壳：正式 Create 机壳，连接纹理与套壳行为由 Create 原生系统驱动。 */
    public static final DeferredBlock<CasingBlock> SAND_CASING = BLOCKS.register(
            "sand_casing",
            () -> new CasingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE)
                    .mapColor(MapColor.SAND)
                    // 与 create:railway_casing 使用同一 SoundType，复用其放置和破坏音效。
                    .sound(SoundType.NETHERITE_BLOCK)));

    /** 沙金套壳传动轴：注册到 Create 的 EncasingRegistry，可由沙金机壳右键套壳。 */
    public static final DeferredBlock<EncasedShaftBlock> SAND_ENCASED_SHAFT = BLOCKS.register(
            "sand_encased_shaft",
            () -> new EncasedShaftBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE)
                    .noOcclusion()
                    .mapColor(MapColor.SAND)
                    .sound(SoundType.NETHERITE_BLOCK), SAND_CASING::get));

    /** 沙金套壳流体管道：用于验证沙金机壳在另一类 Create 可套壳方块上的兼容性。 */
    public static final DeferredBlock<EncasedPipeBlock> SAND_ENCASED_FLUID_PIPE = BLOCKS.register(
            "sand_encased_fluid_pipe",
            () -> new EncasedPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                    .noOcclusion()
                    .mapColor(MapColor.SAND)
                    .sound(SoundType.NETHERITE_BLOCK), SAND_CASING::get));

    public static final DeferredBlock<WetSandBlock> WET_SAND = BLOCKS.register(
            "wet_sand",
            () -> new WetSandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<Block> HARDENED_SAND = BLOCKS.registerSimpleBlock(
            "hardened_sand",
            hardenedSandProperties());

    public static final DeferredBlock<Block> PETRIFIED_SAND = BLOCKS.registerSimpleBlock(
            "petrified_sand",
            petrifiedSandProperties());

    /** Sandified ore variants, keyed by the namespace-free registry path. */
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
            () -> new FlorinolBatteryBlock(BlockBehaviour.Properties.ofFullCopy(SharedProperties.copperMetal())
                    .noOcclusion()));

    /** 沙噬剑座：古沙漠祭坛风格，可插入完整形态的沙噬剑。 */
    public static final DeferredBlock<SandMasterSwordPedestalBlock> SAND_MASTER_SWORD_PEDESTAL = BLOCKS.register(
            "sand_master_sword_pedestal",
            () -> new SandMasterSwordPedestalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CUT_SANDSTONE)
                    .strength(1.8F, 6.0F)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)));

    public static final DeferredBlock<FlorinolSoilBlock> FLORINOL_SOIL = BLOCKS.register(
            "florinol_soil",
            () -> new FlorinolSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));

    /** 热能缓冲器：连续热值由 BlockEntity 保存，方块状态只投影 Create 热等级。 */
    public static final DeferredBlock<ThermalBufferBlock> THERMAL_RELAY = BLOCKS.register(
            "thermal_relay",
            () -> new ThermalBufferBlock(BlockBehaviour.Properties.ofFullCopy(SharedProperties.copperMetal())));
    public static final DeferredBlock<AmethystFlowerBlock> AMETHYST_FLOWER = BLOCKS.register(
            "amethyst_flower",
            () -> new AmethystFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<GoldenDirtBlock> GOLDEN_DIRT = BLOCKS.register(
            "golden_dirt",
            () -> new GoldenDirtBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.GOLD)));

    public static final DeferredBlock<GoldenFarmlandBlock> GOLDEN_FARMLAND = BLOCKS.register(
            "golden_farmland",
            () -> new GoldenFarmlandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND).mapColor(MapColor.GOLD).randomTicks()));

    public static final DeferredBlock<GoldenCarrotCropBlock> GOLDEN_CARROT_CROP = BLOCKS.register(
            "golden_carrot_crop",
            () -> new GoldenCarrotCropBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    /** EncasingRegistry 必须在方块注册完成后写入。 */
    public static void registerEncasingVariants(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EncasingRegistry.addVariant(AllBlocks.SHAFT.get(), SAND_ENCASED_SHAFT.get());
            EncasingRegistry.addVariant(AllBlocks.FLUID_PIPE.get(), SAND_ENCASED_FLUID_PIPE.get());
        });
    }

    /** 让 Create 的套壳管道/传动轴方块实体类型识别本模组的沙金套壳方块，避免放置时报 Invalid block entity。 */
    public static void onBlockEntityTypeAddBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(AllBlockEntityTypes.ENCASED_FLUID_PIPE.get(), SAND_ENCASED_FLUID_PIPE.get());
        event.modify(AllBlockEntityTypes.ENCASED_SHAFT.get(), SAND_ENCASED_SHAFT.get());
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

    private static BlockBehaviour.Properties hardenedSandProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.SAND);
    }

    private static BlockBehaviour.Properties petrifiedSandProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).strength(3.0F, 6.0F);
    }

    private ModBlocks() {
    }
}
