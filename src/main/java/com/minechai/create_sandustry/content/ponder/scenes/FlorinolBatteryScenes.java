package com.minechai.create_sandustry.content.ponder.scenes;

import com.minechai.create_sandustry.registry.ModBlocks;
import java.util.List;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.machine.FlorinolBatteryBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Step-by-step construction guide for the Florinol Battery multiblock.
 */
public final class FlorinolBatteryScenes {
    private static final ResourceLocation FLORINOL_BATTERY =
            ResourceLocation.fromNamespaceAndPath("createsandustry", "florinol_battery");
    private static final ResourceLocation STRUCTURE =
            ResourceLocation.fromNamespaceAndPath("createsandustry", "florinol_battery/assembly");

    private static final BlockPos BATTERY_POS = new BlockPos(5, 1, 3);
    private static final BlockPos ALTERNATE_BATTERY_POS = new BlockPos(5, 3, 3);
    private static final BlockPos LEFT_CENTER = new BlockPos(3, 3, 5);

    private FlorinolBatteryScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(FLORINOL_BATTERY)
                .addStoryBoard(STRUCTURE, FlorinolBatteryScenes::assembleBattery);
    }

    private static void assembleBattery(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("florinol_battery_assembly", "Building a Florinol Battery");
        scene.configureBasePlate(0, 0, 11);
        scene.scaleSceneView(0.58F);

        BlockState sandCasing = ModBlocks.SAND_CASING.get().defaultBlockState();
        BlockState framedGlass = AllPaletteBlocks.FRAMED_GLASS.getDefaultState();
        BlockState copperCasing = AllBlocks.COPPER_CASING.getDefaultState();

        // Keep the 11x11 checkerboard floor, but replace its light gray concrete with white concrete.
        Selection floor = util.select().fromTo(0, 0, 0, 10, 0, 10);
        Selection whiteConcreteFloor = floor;
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                if ((x + z) % 2 == 0) {
                    whiteConcreteFloor = whiteConcreteFloor.substract(util.select().position(x, 0, z));
                }
            }
        }
        scene.world().setBlocks(whiteConcreteFloor, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
        showPositions(scene, floor, 18);

        // 1. Build the 5x5x5 shell one layer at a time. The controller starts as casing.
        setBlockQuietly(scene, BATTERY_POS, sandCasing);
        showPositions(scene, util.select().fromTo(3, 1, 3, 7, 1, 7), 16);

        for (int y = 2; y <= 4; y++) {
            Selection wall = util.select().fromTo(3, y, 3, 7, y, 7)
                    .substract(util.select().fromTo(4, y, 4, 6, y, 6));
            showPositions(scene, wall, 16);
        }

        Selection topRim = util.select().fromTo(3, 5, 3, 7, 5, 7)
                .substract(util.select().fromTo(4, 5, 4, 6, 5, 6));
        showPositions(scene, topRim, 20);
        scene.addKeyframe();
        scene.idle(20);

        // 3-6. Explain the shell.
        showText(scene, "有底", centerOf(new BlockPos(3, 1, 4)), 55);
        scene.idle(65);
        showText(scene, "不封顶", centerOf(new BlockPos(3, 5, 4)), 55);
        scene.idle(65);
        showText(scene, "沙金机壳骨架决定结构大小", centerOf(new BlockPos(3, 3, 3)), 55);
        scene.idle(65);
        showText(scene, "没有大小限制", centerOf(new BlockPos(3, 3, 3)), 55);
        scene.idle(70);

        // 7-8. Replace the casing with the controller.
        replaceBlock(scene, BATTERY_POS, ModBlocks.FLORINOL_BATTERY.get().defaultBlockState());
        scene.addKeyframe();
        scene.idle(25);
        showText(scene, "放置\u00a7e弗洛林电池\u00a7r结构成型", centerOf(BATTERY_POS), 60);
        scene.idle(75);

        // 9-12. Demonstrate another valid controller position on the shell.
        replaceBlock(scene, BATTERY_POS, sandCasing);
        scene.idle(12);
        replaceBlock(scene, ALTERNATE_BATTERY_POS, ModBlocks.FLORINOL_BATTERY.get().defaultBlockState());
        scene.idle(20);
        replaceBlock(scene, ALTERNATE_BATTERY_POS, framedGlass);
        replaceBlock(scene, BATTERY_POS, ModBlocks.FLORINOL_BATTERY.get().defaultBlockState());
        scene.addKeyframe();
        scene.idle(25);
        showText(scene, "可以放在结构外壳的任何位置", centerOf(BATTERY_POS), 40);
        scene.idle(55);

        // 13-14. Replace the entire left pane of glass with copper casing at once.
        Selection leftWall = selectionOf(util, List.of(
                new BlockPos(3, 2, 4), new BlockPos(3, 2, 5), new BlockPos(3, 2, 6),
                new BlockPos(3, 3, 4), new BlockPos(3, 3, 5), new BlockPos(3, 3, 6),
                new BlockPos(3, 4, 4), new BlockPos(3, 4, 5), new BlockPos(3, 4, 6)
        ));
        replaceBlocks(scene, leftWall, copperCasing);
        scene.addKeyframe();
        scene.idle(30);
        showText(scene, "外壁可以用任何机壳代替", centerOf(LEFT_CENTER), 60);
        scene.idle(75);

        // 15. Demonstrate alternate filler-wall blocks.
        replaceBlock(scene, LEFT_CENTER, ModBlocks.THERMAL_RELAY.get().defaultBlockState());
        scene.idle(30);
        replaceBlock(scene, LEFT_CENTER, Blocks.MAGMA_BLOCK.defaultBlockState());
        scene.addKeyframe();
        scene.idle(35);

        // 16. Show the complete 3x3x5 tank with its internal fluid and connection states.
        showPositions(scene, util.select().fromTo(8, 1, 8, 10, 5, 10), 24);
        scene.addKeyframe();
        scene.idle(20);

        // 17. Show the pipes, pump, and extended hose pulley.
        Selection pipeNetwork = selectionOf(util, List.of(
                new BlockPos(6, 6, 7), new BlockPos(6, 6, 8), new BlockPos(8, 6, 8)));
        showPositions(scene, pipeNetwork, 12);
        showPositions(scene, selectionOf(util, List.of(new BlockPos(7, 6, 8))), 12);
        showPositions(scene, selectionOf(util, List.of(new BlockPos(6, 6, 6))), 16);
        scene.idle(5);

        // 18. Show the complete 64 RPM drive train.
        Selection drive = selectionOf(util, List.of(
                new BlockPos(7, 6, 7), new BlockPos(8, 6, 7), new BlockPos(9, 6, 7),
                new BlockPos(10, 6, 7), new BlockPos(11, 6, 7), new BlockPos(11, 5, 8)));
        showPositions(scene, drive, 16);
        Selection kinetic = selectionOf(util, List.of(
                new BlockPos(7, 6, 7), new BlockPos(8, 6, 7), new BlockPos(9, 6, 7),
                new BlockPos(10, 6, 7), new BlockPos(11, 6, 7), new BlockPos(11, 5, 8),
                new BlockPos(7, 6, 8)));
        scene.world().setKineticSpeed(kinetic, 64.0F);
        scene.idle(30);

        // 19-20. Fill the 3x3x3 interior from bottom to top.
        showText(scene, "最后，通入\u00a7d弗洛林电池液\u00a7r", centerOf(new BlockPos(6, 6, 6)), 60);
        scene.idle(75);
        BlockState florinol = ModBlocks.FLORINOL.get().defaultBlockState();
        for (int y = 2; y <= 4; y++) {
            Selection layer = util.select().fromTo(4, y, 4, 6, y, 6);
            scene.world().setBlocks(layer, florinol, false);
            scene.world().showSection(layer, Direction.DOWN);
            scene.idle(14);
        }
        BlockState batteryFull = ModBlocks.FLORINOL_BATTERY.get().defaultBlockState()
                .setValue(FlorinolBatteryBlock.DISPLAY_STATE, FlorinolBatteryBlock.DisplayState.FULL);
        scene.world().setBlock(BATTERY_POS, batteryFull, false);
        scene.addKeyframe();
        scene.idle(20);
        scene.idle(45);
    }

    private static Selection selectionOf(SceneBuildingUtil util, List<BlockPos> positions) {
        Selection selection = util.select().position(positions.get(0));
        for (int i = 1; i < positions.size(); i++) {
            selection = selection.add(util.select().position(positions.get(i)));
        }
        return selection;
    }

    private static void showPositions(CreateSceneBuilder scene, Selection selection, int duration) {
        scene.world().showSection(selection, Direction.DOWN);
        scene.idle(duration);
    }

    private static void setBlockQuietly(CreateSceneBuilder scene, BlockPos pos, BlockState state) {
        scene.world().setBlock(pos, state, false);
    }

    private static void replaceBlock(CreateSceneBuilder scene, BlockPos pos, BlockState state) {
        replaceBlocks(scene, scene.getScene().getSceneBuildingUtil().select().position(pos), state);
    }

    private static void replaceBlocks(CreateSceneBuilder scene, Selection selection, BlockState state) {
        scene.addInstruction(ponderScene -> {
            var world = ponderScene.getWorld();
            var random = world.getRandom();
            selection.forEach(pos -> {
                BlockState replaced = world.getBlockState(pos);
                if (replaced.isAir()) {
                    return;
                }
                BlockParticleOption particles = new BlockParticleOption(ParticleTypes.BLOCK, replaced);
                for (int i = 0; i < 16; i++) {
                    world.addParticle(particles,
                            pos.getX() + 0.1D + random.nextDouble() * 0.8D,
                            pos.getY() + 0.1D + random.nextDouble() * 0.8D,
                            pos.getZ() + 0.1D + random.nextDouble() * 0.8D,
                            (random.nextDouble() - 0.5D) * 0.15D,
                            random.nextDouble() * 0.15D,
                            (random.nextDouble() - 0.5D) * 0.15D);
                }
            });
        });
        scene.idle(6);
        scene.world().setBlocks(selection, state, false);
        scene.addInstruction(ponderScene ->
                ponderScene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw));
        scene.idle(6);
    }

    private static Vec3 centerOf(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static void showText(CreateSceneBuilder scene, String text, Vec3 point, int duration) {
        scene.overlay().showText(duration)
                .text(text)
                .pointAt(point)
                .placeNearTarget();
    }

}
