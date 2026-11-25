package com.minechai.create_sandustry.ponder.scenes;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.nbt.CompoundTag;

public final class GoldenExperienceScenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ResourceLocation.parse("createsandustry:golden_experience_block"),
                ResourceLocation.parse("create:experience_block"))
                .addStoryBoard(ResourceLocation.parse("createsandustry:golden_experience"), GoldenExperienceScenes::conversion);
    }

    private static void conversion(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("golden_experience", "Golden Experience Block Conversion");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        BlockPos input = util.grid().at(2, 1, 2);
        BlockPos blast = util.grid().at(2, 1, 1);
        // Ponder checks world bounds before applying block replacements. A floor-only
        // structure otherwise excludes the demonstration above the base plate.
        scene.addInstruction(s -> s.getWorld().getBounds().encapsulate(input.above(2)));
        scene.world().setBlock(input, AllBlocks.EXPERIENCE_BLOCK.getDefaultState(), false);
        // Keep the glowing demonstration separate from the normally shaded floor.
        scene.world().showIndependentSection(util.select().position(input), Direction.DOWN);
        scene.overlay().showText(70).text("Place a Block of Experience near a charged Creeper.")
                .pointAt(util.vector().blockSurface(input, Direction.UP)).placeNearTarget();
        scene.idle(80);
        scene.addKeyframe();
        var chargedCreeper = scene.world().createEntity(level -> {
            Creeper creeper = EntityType.CREEPER.create(level);
            if (creeper != null) {
                CompoundTag tag = new CompoundTag();
                tag.putBoolean("powered", true);
                creeper.readAdditionalSaveData(tag);
                creeper.moveTo(2.5, 1, 1.5, 0, 0);
            }
            return creeper;
        });
        scene.overlay().showText(70).text("The explosion may turn it into a Block of Golden Experience.")
                .pointAt(util.vector().centerOf(blast)).placeNearTarget();
        scene.idle(80);
        scene.world().modifyEntity(chargedCreeper, entity -> entity.discard());
        scene.addInstruction(s -> {
            s.getWorld().addParticle(ParticleTypes.EXPLOSION_EMITTER, 2.5, 1.5, 1.5, 0, 0, 0);
            s.getWorld().playLocalSound(2.5, 1.5, 1.5, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1, false);
        });
        scene.world().setBlock(input, ModBlocks.GOLDEN_EXPERIENCE_BLOCK.get().defaultBlockState(), false);
        scene.idle(25);
        scene.idle(100);
        scene.markAsFinished();
    }
}
