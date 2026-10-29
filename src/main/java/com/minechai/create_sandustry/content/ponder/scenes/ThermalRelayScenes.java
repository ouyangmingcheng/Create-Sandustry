package com.minechai.create_sandustry.content.ponder.scenes;

import java.util.Locale;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.machine.ThermalBufferBlock;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Native Ponder scenes for the Thermal Buffer.
 *
 * <p>This class is maintained by hand and does not depend on Ponderer or any
 * Ponderer-generated support classes.</p>
 */
public final class ThermalRelayScenes {
    public static final ResourceLocation THERMAL_TAG =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "thermal");

    private static final ResourceLocation THERMAL_RELAY =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "thermal_relay");
    private static final ResourceLocation STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "thermal_relay/basic");

    private static final BlockPos RELAY_CENTER = new BlockPos(2, 1, 2);
    private static final Vec3 RELAY_POINT = new Vec3(2.0D, 1.5D, 2.5D);
    private static final Vec3 ABOVE_RELAY_POINT = new Vec3(2.5D, 2.0D, 2.5D);

    private ThermalRelayScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation[] tags = {THERMAL_TAG};
        helper.forComponents(THERMAL_RELAY)
                .addStoryBoard(STRUCTURE, ThermalRelayScenes::heatSource, tags)
                .addStoryBoard(STRUCTURE, ThermalRelayScenes::heatTransfer, tags)
                .addStoryBoard(STRUCTURE, ThermalRelayScenes::heatStorage, tags)
                .addStoryBoard(STRUCTURE, ThermalRelayScenes::redstoneControl, tags);
    }

    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(THERMAL_TAG)
                .title("Thermal Equipment")
                .description("Blocks that store, transfer, or use heat")
                .item(ModItems.THERMAL_RELAY.get())
                .addToIndex()
                .register();
        helper.addTagToComponent(THERMAL_RELAY, THERMAL_TAG);
    }

    private static void heatSource(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = startScene(builder, util, "thermal_relay_thermalbuffer", "Heat Sources");
        BlockPos relay = util.grid().at(RELAY_CENTER.getX(), RELAY_CENTER.getY(), RELAY_CENTER.getZ());
        BlockPos heatSource = util.grid().at(1, 1, 2);

        placeThermalRelay(scene, relay, HeatLevel.NONE, 0.0D, 0);
        scene.idle(20);
        showText(scene, "热能缓冲器能从临近的热源方块获得热值", RELAY_POINT, 30);
        scene.idle(40);

        placeBlock(scene, heatSource, Blocks.CAMPFIRE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true)
                .setValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE, false)
                .setValue(net.minecraft.world.level.block.CampfireBlock.WATERLOGGED, false)
                .setValue(net.minecraft.world.level.block.CampfireBlock.FACING, Direction.SOUTH));
        scene.idle(20);
        updateThermalRelay(scene, relay, HeatLevel.SMOULDERING, 300.0D, 1);
        scene.idle(20);

        scene.world().destroyBlock(heatSource);
        placeBlock(scene, heatSource, Blocks.MAGMA_BLOCK.defaultBlockState());
        showText(scene, "不同的热源方块提供的热值不同", new Vec3(1.0D, 1.5D, 2.5D), 30);
        scene.idle(40);

        scene.world().destroyBlock(heatSource);
        scene.addKeyframe();
        placeBlazeBurner(scene, heatSource, HeatLevel.KINDLED);
        emitBlazeParticles(scene, heatSource, HeatLevel.KINDLED, 60);
        scene.idle(10);
        showText(scene, "燃烧室提供的热值能提升热能等级", new Vec3(2.5D, 2.0D, 2.5D), 30);
        scene.idle(40);
        updateThermalRelay(scene, relay, HeatLevel.KINDLED, 700.0D, 2);
        scene.idle(20);

        placeBlazeBurner(scene, heatSource, HeatLevel.SEETHING);
        emitBlazeParticles(scene, heatSource, HeatLevel.SEETHING, 200);
        updateThermalRelay(scene, relay, HeatLevel.SEETHING, 1000.0D, 3);
        scene.addKeyframe();
        scene.idle(20);

        BlockPos tankBottom = util.grid().at(2, 2, 2);
        BlockPos tankTop = util.grid().at(2, 3, 2);
        scene.world().setBlock(tankBottom, tankState(false, true), false);
        scene.world().setBlock(tankTop, tankState(true, false), false);
        mergeBlockEntity(scene, tankBottom,
                "{Boiler:{ActiveHeat:2,Engines:1,PassiveHeat:0b,SimVents:0,Supply:0.0f,Update:0b,Whistles:0},Height:2,LastKnownPos:[I;87,-59,-54],Luminosity:0,Owner:[I;940439953,-167562164,-1601161573,-1389718966],Size:1,TankContent:{},Window:0b}");
        mergeBlockEntity(scene, tankTop,
                "{Boiler:{ActiveHeat:0,Engines:0,PassiveHeat:0b,SimVents:0,Supply:0.0f,Update:0b,Whistles:0},Controller:[I;87,-59,-54],LastKnownPos:[I;87,-58,-54],Luminosity:0,Owner:[I;940439953,-167562164,-1601161573,-1389718966]}");
        setBoilerGaugeImmediately(scene, tankBottom, 1.0F, 1.0F);
        showBlock(scene, tankBottom);
        showBlock(scene, tankTop);
        showText(scene, "作为锅炉的热源", ABOVE_RELAY_POINT, 30);
        scene.idle(40);

        scene.world().setBlock(tankBottom, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(tankTop, Blocks.AIR.defaultBlockState(), false);
        BlockPos basin = util.grid().at(2, 2, 2);
        BlockPos mixer = util.grid().at(2, 4, 2);
        placeBlock(scene, basin, AllBlocks.BASIN.getDefaultState());
        placeBlock(scene, mixer, AllBlocks.MECHANICAL_MIXER.getDefaultState());
        scene.world().setKineticSpeed(util.select().position(mixer), 16.0F);
        scene.idle(10);
        showText(scene, "正常处理加热配方", ABOVE_RELAY_POINT, 30);
        scene.idle(40);

        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        scene.world().createItemOnBeltLike(basin, Direction.UP, cobblestone.copy());
        scene.overlay().showControls(ABOVE_RELAY_POINT, Pointing.LEFT, 10).withItem(cobblestone);
        scene.world().setKineticSpeed(util.select().position(mixer), 128.0F);
        scene.world().modifyBlockEntity(mixer, MechanicalMixerBlockEntity.class,
                MechanicalMixerBlockEntity::startProcessingBasin);
        scene.idle(40);
    }

    private static void heatTransfer(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = startScene(builder, util, "thermal_relay_thermal_relay_conductheat", "Heat Transfer");
        BlockPos source = util.grid().at(4, 1, 2);
        BlockPos relayHigh = util.grid().at(3, 1, 2);
        BlockPos relayMiddle = util.grid().at(2, 1, 2);
        BlockPos relayLow = util.grid().at(1, 1, 2);

        placeBlazeBurner(scene, source, HeatLevel.SEETHING);
        emitBlazeParticles(scene, source, HeatLevel.SEETHING, 200);
        placeThermalRelay(scene, relayHigh, HeatLevel.NONE, 0.0D, 0);
        updateThermalRelay(scene, relayHigh, HeatLevel.SEETHING, 1000.0D, 3);
        scene.idle(20);

        placeThermalRelay(scene, relayMiddle, HeatLevel.NONE, 0.0D, 0);
        scene.idle(20);
        showText(scene, "热能缓冲器之间可以转递热值", RELAY_POINT, 30);
        scene.idle(40);
        updateThermalRelay(scene, relayMiddle, HeatLevel.KINDLED, 750.0D, 2);
        scene.idle(15);
        showText(scene, "但会损失最大热值", RELAY_POINT, 30);
        scene.idle(40);

        placeThermalRelay(scene, relayLow, HeatLevel.NONE, 0.0D, 0);
        scene.idle(15);
        updateThermalRelay(scene, relayLow, HeatLevel.SMOULDERING, 421.875D, 1);
        scene.idle(20);
    }

    private static void heatStorage(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = startScene(builder, util, "thermal_relay_thermal_relay_tes", "Storing Heat");
        BlockPos relay = util.grid().at(2, 1, 2);
        BlockPos heatSource = util.grid().at(2, 2, 2);

        placeThermalRelay(scene, relay, HeatLevel.NONE, 0.0D, 0);
        placeBlazeBurner(scene, heatSource, HeatLevel.SEETHING);
        emitBlazeParticles(scene, heatSource, HeatLevel.SEETHING, 50);
        scene.idle(15);
        updateThermalRelay(scene, relay, HeatLevel.SEETHING, 1000.0D, 3);
        showText(scene, "热能缓冲器有存储热能的功能", RELAY_POINT, 30);
        scene.idle(40);

        scene.world().destroyBlock(heatSource);
        showText(scene, "热源被破坏后会缓慢流失热值", RELAY_POINT, 30);
        scene.idle(40);

        scene.overlay().showControls(ABOVE_RELAY_POINT, Pointing.DOWN, 30)
                .withItem(new ItemStack(Items.CLOCK));
        scene.idle(40);
        updateThermalRelay(scene, relay, HeatLevel.KINDLED, 750.0D, 2);
        scene.addKeyframe();
        scene.idle(15);

        scene.world().setBlocks(util.select().layer(0), Blocks.NETHERRACK.defaultBlockState(), true);
        scene.idle(15);
        showText(scene, "在地狱中热值不会流失", RELAY_POINT, 30);
        scene.idle(40);

        scene.overlay().showControls(ABOVE_RELAY_POINT, Pointing.DOWN, 10)
                .leftClick()
                .withItem(new ItemStack(Items.DIAMOND_PICKAXE));
        scene.world().destroyBlock(relay);

        ItemStack droppedBuffer = new ItemStack(ModItems.THERMAL_RELAY.get());
        droppedBuffer.set(ModDataComponents.THERMAL_HEAT.get(), 750.0D);
        scene.world().createItemEntity(new Vec3(2.5D, 2.0D, 2.5D), Vec3.ZERO, droppedBuffer);
        scene.idle(20);
        showText(scene, "掉落物会保存热值", new Vec3(2.5D, 1.0D, 2.5D), 30);
        scene.idle(20);
    }

    private static void redstoneControl(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = startScene(builder, util, "thermal_relay_redstone_control", "Redstone Control");
        BlockPos relay = util.grid().at(2, 1, 2);
        BlockPos lever = util.grid().at(1, 1, 2);
        BlockPos tankBottom = util.grid().at(2, 2, 2);
        BlockPos tankTop = util.grid().at(2, 3, 2);

        placeThermalRelay(scene, relay, HeatLevel.SEETHING, 1000.0D, 3);
        placeBlock(scene, lever, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.WEST)
                .setValue(BlockStateProperties.POWERED, false));

        scene.world().setBlock(tankBottom, tankState(false, true), false);
        scene.world().setBlock(tankTop, tankState(true, false), false);
        mergeBlockEntity(scene, tankBottom,
                "{Boiler:{ActiveHeat:2,Engines:1,PassiveHeat:0b,SimVents:0,Supply:0.0f,Update:0b,Whistles:0},Height:2,LastKnownPos:[I;87,-59,-54],Luminosity:0,Owner:[I;940439953,-167562164,-1601161573,-1389718966],Size:1,TankContent:{},Window:0b}");
        mergeBlockEntity(scene, tankTop,
                "{Boiler:{ActiveHeat:0,Engines:0,PassiveHeat:0b,SimVents:0,Supply:0.0f,Update:0b,Whistles:0},Controller:[I;87,-59,-54],LastKnownPos:[I;87,-58,-54],Luminosity:0,Owner:[I;940439953,-167562164,-1601161573,-1389718966]}");
        setBoilerGaugeImmediately(scene, tankBottom, 1.0F, 1.0F);
        showBlock(scene, tankBottom);
        showBlock(scene, tankTop);
        scene.idle(20);

        showText(scene, "未通入红石信号时，缓冲器会向锅炉持续提供热量", ABOVE_RELAY_POINT, 40);
        scene.idle(50);

        scene.world().modifyBlock(lever, state -> state.setValue(BlockStateProperties.POWERED, true), false);
        scene.effects().indicateRedstone(lever);
        setThermalRelayPowered(scene, relay, true, HeatLevel.NONE, 1000.0D, 3);
        mergeBlockEntity(scene, tankBottom, "{Boiler:{ActiveHeat:0,Engines:1}}");
        animateBoilerGauge(scene, tankBottom, 1.0F, 0.0F, 20);
        scene.addKeyframe();
        scene.idle(20);

        showText(scene, "通入红石信号后，缓冲器封闭对外热输出", ABOVE_RELAY_POINT, 40);
        scene.idle(50);
        showText(scene, "内部热值不会消失，自然流失也会停止", ABOVE_RELAY_POINT, 40);
        scene.idle(50);

        scene.world().modifyBlock(lever, state -> state.setValue(BlockStateProperties.POWERED, false), false);
        scene.effects().indicateRedstone(lever);
        setThermalRelayPowered(scene, relay, false, HeatLevel.SEETHING, 1000.0D, 3);
        mergeBlockEntity(scene, tankBottom, "{Boiler:{ActiveHeat:2,Engines:1}}");
        animateBoilerGauge(scene, tankBottom, 0.0F, 1.0F, 20);
        scene.addKeyframe();
        scene.idle(20);

        showText(scene, "断开红石信号后，已储存的热量会重新释放", ABOVE_RELAY_POINT, 40);
        scene.idle(50);
    }
    private static CreateSceneBuilder startScene(SceneBuilder builder, SceneBuildingUtil util, String id, String title) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        return scene;
    }

    private static void placeThermalRelay(CreateSceneBuilder scene, BlockPos pos, HeatLevel level,
                                          double storedHeat, int thermalLevel) {
        BlockState state = ModBlocks.THERMAL_RELAY.get().defaultBlockState()
                .setValue(ThermalBufferBlock.POWERED, false)
                .setValue(ThermalBufferBlock.HEAT_LEVEL, level);
        scene.world().setBlock(pos, state, false);
        mergeBlockEntity(scene, pos, thermalNbt(storedHeat, thermalLevel));
        showBlock(scene, pos);
    }

    private static void setBoilerGaugeImmediately(CreateSceneBuilder scene, BlockPos controllerPos,
                                                  float value, float target) {
        scene.addInstruction(ponderScene -> setBoilerGauge(ponderScene, controllerPos, value, target));
    }
    private static void animateBoilerGauge(CreateSceneBuilder scene, BlockPos controllerPos,
                                           float from, float to, int duration) {
        for (int tick = 1; tick <= duration; tick++) {
            float value = from + (to - from) * tick / duration;
            scene.addInstruction(ponderScene -> setBoilerGauge(ponderScene, controllerPos, value, to));
            scene.idle(1);
        }
    }

    private static void setBoilerGauge(PonderScene scene, BlockPos controllerPos, float value, float target) {
        if (scene.getWorld().getBlockEntity(controllerPos) instanceof FluidTankBlockEntity tank) {
            // Reassert the target every tick: the virtual boiler may still hold the old
            // target after a data reload, which would otherwise pull the dial back.
            tank.boiler.gauge.chase(target, 0.125F, Chaser.EXP);
            tank.boiler.gauge.setValueNoUpdate(value);
        }
    }
    private static void setThermalRelayPowered(CreateSceneBuilder scene, BlockPos pos, boolean powered,
                                               HeatLevel exposedLevel, double storedHeat, int thermalLevel) {
        scene.world().modifyBlock(pos, state -> state
                .setValue(ThermalBufferBlock.POWERED, powered)
                .setValue(ThermalBufferBlock.HEAT_LEVEL, exposedLevel), false);
        mergeBlockEntity(scene, pos, thermalNbt(storedHeat, thermalLevel));
    }
    private static void updateThermalRelay(CreateSceneBuilder scene, BlockPos pos, HeatLevel level,
                                           double storedHeat, int thermalLevel) {
        scene.world().modifyBlock(pos, state -> state
                .setValue(ThermalBufferBlock.POWERED, false)
                .setValue(ThermalBufferBlock.HEAT_LEVEL, level), false);
        mergeBlockEntity(scene, pos, thermalNbt(storedHeat, thermalLevel));
    }

    private static void emitBlazeParticles(CreateSceneBuilder scene, BlockPos pos, HeatLevel level, int cycles) {
        if (level == HeatLevel.NONE) {
            return;
        }

        Vec3 anchor = new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        ParticleEmitter smoke = scene.effects()
                .simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, new Vec3(0.0D, 0.04D, 0.0D));
        scene.effects().emitParticles(anchor, smoke, 0.08F, cycles);

        var flameType = level.isAtLeast(HeatLevel.SEETHING)
                ? ParticleTypes.SOUL_FIRE_FLAME
                : ParticleTypes.FLAME;
        ParticleEmitter flames = scene.effects()
                .particleEmitterWithinBlockSpace(flameType, new Vec3(0.0D, 0.06D, 0.0D));
        scene.effects().emitParticles(anchor.add(0.0D, 0.25D, 0.0D), flames, 0.25F, cycles);
    }
    private static void placeBlazeBurner(CreateSceneBuilder scene, BlockPos pos, HeatLevel level) {
        BlockState state = AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, level);
        scene.world().setBlock(pos, state, false);
        mergeBlockEntity(scene, pos, "{isCreative:1b}");
        showBlock(scene, pos);
    }

    private static BlockState tankState(boolean top, boolean bottom) {
        return AllBlocks.FLUID_TANK.getDefaultState()
                .setValue(FluidTankBlock.TOP, top)
                .setValue(FluidTankBlock.BOTTOM, bottom)
                .setValue(FluidTankBlock.SHAPE, Shape.PLAIN);
    }

    private static void placeBlock(CreateSceneBuilder scene, BlockPos pos, BlockState state) {
        scene.world().setBlock(pos, state, false);
        showBlock(scene, pos);
    }

    private static void showBlock(CreateSceneBuilder scene, BlockPos pos) {
        Selection selection = scene.getScene().getSceneBuildingUtil().select().position(pos);
        // Remove any previous block at this position from every visible section before
        // animating the replacement. Otherwise the old section and the incoming
        // animated section render the same coordinate at once.
        scene.addInstruction(ponderScene -> ponderScene.forEach(WorldSectionElement.class, section -> {
            if (!section.isEmpty()) {
                section.erase(selection);
            }
        }));
        scene.world().showSection(selection, Direction.DOWN);
    }

    private static void mergeBlockEntity(CreateSceneBuilder scene, BlockPos pos, String snbt) {
        CompoundTag patch = parseTag(snbt);
        scene.world().modifyBlockEntityNBT(
                scene.getScene().getSceneBuildingUtil().select().position(pos),
                BlockEntity.class,
                tag -> tag.merge(patch.copy()),
                true);
    }

    private static CompoundTag parseTag(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException exception) {
            throw new IllegalArgumentException("Invalid Ponder SNBT: " + snbt, exception);
        }
    }
    private static String thermalNbt(double storedHeat, int thermalLevel) {
        return String.format(Locale.ROOT, "{StoredHeat:%sd,ThermalLevel:%d}", storedHeat, thermalLevel);
    }

    private static void showText(CreateSceneBuilder scene, String text, Vec3 point, int duration) {
        scene.overlay().showText(duration)
                .text(text)
                .pointAt(point)
                .placeNearTarget();
    }
}