package com.minechai.create_sandustry.content.ponder.scenes;

import com.minechai.create_sandustry.registry.ModItems;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Teaches the player-driven and Mechanical Arm versions of the Midnight Prismarine conversion.
 */
public final class MoonCrystalScenes {
    private static final ResourceLocation PRISMARINE_SHARD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "prismarine_shard");
    private static final ResourceLocation STRUCTURE =
            ResourceLocation.fromNamespaceAndPath("createsandustry", "lunar_conversion/basic");

    private static final String DEPOT_STACK_NBT =
            "{HeldItem:{Item:{id:\"minecraft:prismarine_shard\",count:1},Pos:0.5f,PrevPos:0.5f,"
                    + "Offset:0.0f,PrevOffset:0.0f,InSegment:0,Angle:180,InDirection:1}}";
    private static final String ARM_TARGETS_NBT =
            "{InteractionPoints:["
                    + "{Type:\"create:depot\",Pos:[I;1,0,0],Mode:\"TAKE\"},"
                    + "{Type:\"create:depot\",Pos:[I;-1,0,0],Mode:\"DEPOSIT\"}"
                    + "],Phase:\"SEARCH_INPUTS\",TargetPointIndex:-1,MovementProgress:0.0f,HeldItem:{}}";

    private MoonCrystalScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(PRISMARINE_SHARD)
                .addStoryBoard(STRUCTURE, MoonCrystalScenes::prismarineToMoonCrystal);
    }

    private static void prismarineToMoonCrystal(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mooncrystal_conversion", "Lunar Conversion");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(4);

        BlockPos depot = util.grid().at(3, 1, 2);
        BlockPos arm = util.grid().at(2, 1, 2);
        BlockPos outputDepot = util.grid().at(1, 1, 2);
        scene.addInstruction(ponderScene -> {
            ponderScene.getWorld().getBounds().encapsulate(depot);
            ponderScene.getWorld().getBounds().encapsulate(arm);
            ponderScene.getWorld().getBounds().encapsulate(outputDepot);
        });

        // 1. Place the input Depot.
        placeBlock(scene, depot, AllBlocks.DEPOT.getDefaultState());
        scene.addKeyframe();
        scene.idle(35);

        // 2. Show Prismarine Shards being placed onto the Depot.
        ItemStack shard = new ItemStack(Items.PRISMARINE_SHARD);
        scene.overlay().showControls(util.vector().topOf(depot), Pointing.DOWN, 50).withItem(shard.copy());
        mergeBlockEntity(scene, depot, DEPOT_STACK_NBT);
        scene.idle(70);

        // 3. Waterlog the Depot with a Water Bucket.
        scene.overlay().showControls(util.vector().topOf(depot), Pointing.DOWN, 50)
                .rightClick()
                .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.world().modifyBlock(depot, state -> state.setValue(BlockStateProperties.WATERLOGGED, true), false);
        addWaterParticles(scene, util.vector().topOf(depot));
        scene.idle(70);

        // 4. Establish the midnight condition.
        showText(scene, "当月光最盛之时，", util.vector().topOf(depot), 50);
        scene.idle(65);

        // 5. Explain manual extraction.
        scene.overlay().showControls(util.vector().topOf(depot), Pointing.DOWN, 50).rightClick();
        scene.addKeyframe();
        showText(scene, "此时从置物台中拿取海晶碎片，有概率获得月晶石碎片", util.vector().topOf(depot), 40);
        scene.idle(60);

        // 6. Add a powered Mechanical Arm and its output Depot.
        placeBlock(scene, arm, AllBlocks.MECHANICAL_ARM.getDefaultState());
        scene.idle(25);
        placeBlock(scene, outputDepot, AllBlocks.DEPOT.getDefaultState());
        scene.world().setKineticSpeed(util.select().position(arm), 128.0F);
        mergeBlockEntity(scene, arm, ARM_TARGETS_NBT);
        scene.idle(40);

        // 7. Mark input and output, then explain automatic extraction.
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), util.select().position(depot), 140);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), util.select().position(outputDepot), 140);
        showText(scene, "机械手可以代替玩家的拿取动作", util.vector().topOf(arm), 85);
        scene.idle(100);

        // 8. Animate the Arm taking a shard, replacing it with a Moon Crystal, and depositing it.
        ItemStack moonCrystal = new ItemStack(ModItems.MOONCRYSTAL.get());
        scene.world().instructArm(arm, Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 0);
        scene.idle(20);
        scene.world().removeItemsFromBelt(depot);
        scene.world().instructArm(arm, Phase.SEARCH_OUTPUTS, moonCrystal.copy(), -1);
        scene.idle(4);
        scene.world().instructArm(arm, Phase.MOVE_TO_OUTPUT, moonCrystal.copy(), 0);
        scene.idle(20);
        scene.world().createItemOnBeltLike(outputDepot, Direction.UP, moonCrystal.copy());
        scene.effects().emitParticles(util.vector().topOf(outputDepot), scene.effects()
                .particleEmitterWithinBlockSpace(ParticleTypes.END_ROD, Vec3.ZERO), 1.0F, 15);
        scene.world().instructArm(arm, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);
        scene.idle(25);
    }

    private static void placeBlock(CreateSceneBuilder scene, BlockPos pos, BlockState state) {
        scene.world().setBlock(pos, state, false);
        Selection selection = scene.getScene().getSceneBuildingUtil().select().position(pos);
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

    private static void addWaterParticles(CreateSceneBuilder scene, Vec3 point) {
        scene.effects().emitParticles(point, scene.effects()
                .simpleParticleEmitter(ParticleTypes.BUBBLE, new Vec3(0.0D, 0.04D, 0.0D)), 0.2F, 50);
    }

    private static void showText(CreateSceneBuilder scene, String text, Vec3 point, int duration) {
        scene.overlay().showText(duration)
                .text(text)
                .pointAt(point)
                .placeNearTarget();
    }
}
