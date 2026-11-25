package com.minechai.create_sandustry.ponder.scenes;

import com.minechai.create_sandustry.content.kineticpress.KineticPressBlock;
import com.minechai.create_sandustry.content.kineticpress.KineticPressBlockEntity;
import com.minechai.create_sandustry.content.kineticpress.client.KineticPressAnimation;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Scripted demonstration: Ponder's client level never starts real server tasks. */
public final class KineticPressScenes {
    private static final BlockPos BASE = new BlockPos(2, 2, 2);
    private static final BlockPos CHEST = BASE.below();
    // A compact example keeps the input and its controls inside the viewport.
    private static final int DEMO_HEIGHT = 3;
    private static final int PROCESSING_TICKS = 20;
    private static final ResourceLocation STRUCTURE =
            ResourceLocation.fromNamespaceAndPath("createsandustry", "kinetic_press/basic");

    private KineticPressScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("createsandustry", "kinetic_press"))
                .addStoryBoard(STRUCTURE, KineticPressScenes::basics);
    }

    private static void basics(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("kinetic_press_basics", "Building and Using a Kinetic Press");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.75F);
        scene.setSceneOffsetY(-0.5F);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);

        place(scene, util, CHEST, Blocks.CHEST.defaultBlockState());
        place(scene, util, BASE, ModBlocks.KINETIC_PRESS.get().defaultBlockState());
        showText(scene, "Place the base above a container. Keep the processing column clear.",
                Vec3.atCenterOf(BASE), 65);
        scene.idle(75);
        scene.addKeyframe();

        Vec3 settings = Vec3.atCenterOf(BASE).add(0.0D, 0.2D, 0.5D);
        scene.overlay().showControls(settings, Pointing.RIGHT, 60).rightClick();
        showText(scene, "Set the working height on the side. This example uses a short, 3-block column.",
                settings, 65);
        setHeight(scene, util, DEMO_HEIGHT);
        scene.idle(75);

        scene.overlay().showControls(settings, Pointing.RIGHT, 60).rightClick()
                .withItem(new ItemStack(AllBlocks.CHUTE.get()));
        showText(scene, "Right-click the base with a Chute to build the input at the configured height.", settings, 70);
        scene.idle(20);
        place(scene, util, BASE.above(DEMO_HEIGHT), AllBlocks.CHUTE.getDefaultState());
        scene.idle(65);
        scene.addKeyframe();

        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), util.select().position(BASE), 80);
        scene.effects().emitParticles(Vec3.atCenterOf(BASE), scene.effects()
                .simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, Vec3.ZERO), 0.7F, 12);
        setEnergy(scene, util, KineticPressBlockEntity.MAX_ENERGY);
        showText(scene, "Supply FE through one of the four horizontal faces.", settings, 60);
        scene.idle(70);

        Vec3 inputPoint = Vec3.atCenterOf(BASE.above(DEMO_HEIGHT));
        ItemStack residue = new ItemStack(ModItems.BURNT_RESIDUE.get());
        scene.overlay().showControls(inputPoint, Pointing.LEFT, 60).withItem(residue.copy());
        showText(scene, "Feed ingredients through the Chute. Up to 16 items process together after a short collection period.",
                inputPoint, 75);
        ElementLink<EntityElement> falling = createHeldItem(scene, residue, DEMO_HEIGHT);
        scene.idle(10);
        setWorking(scene, true);
        setEnergy(scene, util, 1_800);
        animateFall(scene, falling, DEMO_HEIGHT, 0, PROCESSING_TICKS);
        scene.world().modifyEntity(falling, Entity::discard);
        setWorking(scene, false);
        scene.idle(30);
        scene.addKeyframe();

        ItemStack seeds = new ItemStack(ModItems.AMETHYST_FLOWER_SEEDS.get());
        scene.overlay().showControls(Vec3.atCenterOf(CHEST), Pointing.RIGHT, 65).withItem(seeds.copy());
        scene.world().modifyBlockEntity(CHEST, ChestBlockEntity.class, chest -> chest.setItem(0, seeds.copy()));
        showText(scene, "Products leave from below, into containers or Create logistics.",
                Vec3.atCenterOf(CHEST), 65);
        scene.idle(75);

        scene.overlay().showControls(settings, Pointing.RIGHT, 60).rightClick();
        showText(scene, "When idle, changing height moves the Chute. More height reduces the extra energy cost.", settings, 75);
        moveInput(scene, util, DEMO_HEIGHT, DEMO_HEIGHT + 1);
        scene.idle(85);
        moveInput(scene, util, DEMO_HEIGHT + 1, DEMO_HEIGHT);
        scene.idle(20);
        scene.addKeyframe();

        ElementLink<EntityElement> pausedItem = createHeldItem(scene, residue, DEMO_HEIGHT);
        setWorking(scene, true);
        animateFall(scene, pausedItem, DEMO_HEIGHT, 0, PROCESSING_TICKS / 2);
        BlockPos signal = BASE.east();
        place(scene, util, signal, Blocks.REDSTONE_BLOCK.defaultBlockState());
        scene.world().modifyBlock(BASE, state -> state.setValue(KineticPressBlock.POWERED, true), false);
        showText(scene, "Redstone pauses the machine. Remove the signal to continue.",
                Vec3.atCenterOf(signal), 75);
        scene.idle(85);

        scene.world().setBlock(signal, Blocks.AIR.defaultBlockState(), false);
        scene.world().modifyBlock(BASE, state -> state.setValue(KineticPressBlock.POWERED, false), false);
        animateFall(scene, pausedItem, DEMO_HEIGHT, PROCESSING_TICKS / 2, PROCESSING_TICKS);
        scene.world().modifyEntity(pausedItem, Entity::discard);
        setWorking(scene, false);
        scene.idle(55);
    }

    private static ElementLink<EntityElement> createHeldItem(CreateSceneBuilder scene, ItemStack stack, int height) {
        Vec3 start = new Vec3(BASE.getX() + 0.5D, BASE.getY() + 1.0D + height, BASE.getZ() + 0.5D);
        ElementLink<EntityElement> link = scene.world().createItemEntity(start, Vec3.ZERO, stack.copy());
        scene.world().modifyEntity(link, entity -> {
            entity.setNoGravity(true);
            entity.setDeltaMovement(Vec3.ZERO);
        });
        return link;
    }

    private static void animateFall(CreateSceneBuilder scene, ElementLink<EntityElement> link,
                                    int height, int fromTick, int toTick) {
        for (int tick = fromTick + 1; tick <= toTick; tick++) {
            double y = KineticPressAnimation.getPosition(BASE.getY() + 1.0D + height, tick, PROCESSING_TICKS, height, 7.0D);
            scene.world().modifyEntity(link, entity -> {
                entity.setDeltaMovement(Vec3.ZERO);
                entity.setPos(BASE.getX() + 0.5D, y, BASE.getZ() + 0.5D);
            });
            scene.idle(1);
        }
    }

    private static void moveInput(CreateSceneBuilder scene, SceneBuildingUtil util, int oldHeight, int newHeight) {
        scene.world().setBlock(BASE.above(oldHeight), Blocks.AIR.defaultBlockState(), false);
        place(scene, util, BASE.above(newHeight), AllBlocks.CHUTE.getDefaultState());
        setHeight(scene, util, newHeight);
    }

    private static void setHeight(CreateSceneBuilder scene, SceneBuildingUtil util, int height) {
        scene.world().modifyBlockEntityNBT(util.select().position(BASE), KineticPressBlockEntity.class, tag -> {
            tag.putInt("workingHeight", height);
            tag.putBoolean("structureValid", true);
            tag.putString("structureFailure", "NONE");
            tag.putLong("BoundInput", BASE.above(height).asLong());
        });
    }

    private static void setEnergy(CreateSceneBuilder scene, SceneBuildingUtil util, int energy) {
        scene.world().modifyBlockEntityNBT(util.select().position(BASE), KineticPressBlockEntity.class,
                tag -> tag.putInt("energy", energy));
    }

    private static void setWorking(CreateSceneBuilder scene, boolean working) {
        scene.world().modifyBlock(BASE, state -> state.setValue(KineticPressBlock.WORKING, working), false);
    }

    private static void place(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, BlockState state) {
        scene.world().setBlock(pos, state, false);
        Selection selection = util.select().position(pos);
        scene.addInstruction(ponderScene -> ponderScene.forEach(WorldSectionElement.class, section -> {
            if (!section.isEmpty()) section.erase(selection);
        }));
        scene.world().showSection(selection, Direction.DOWN);
    }

    private static void showText(CreateSceneBuilder scene, String text, Vec3 point, int duration) {
        scene.overlay().showText(duration).text(text).pointAt(point).placeNearTarget();
    }
}
