package com.minechai.create_sandustry.ponder.scenes;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Reuses the Aeronautics Levitite Blend demonstration flow for Florinol Soil.
 * The conversion ends after the liquid spreads through the soil; the later
 * casting demonstration is intentionally omitted.
 */
public final class FlorinolSoilScenes {
    private static final ResourceLocation FLORINOL_SOIL =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "florinol_soil");
    private static final ResourceLocation FLORINOL_BUCKET =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "florinol_bucket");
    private static final ResourceLocation STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "florinol_soil/liquefaction");

    private static final BlockPos CENTER_SAMPLE = new BlockPos(3, 3, 3);
    private static final Vec3 CENTER_POINT = new Vec3(3.5D, 1.5D, 3.5D);

    private FlorinolSoilScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(FLORINOL_SOIL, FLORINOL_BUCKET)
                .addStoryBoard(STRUCTURE, FlorinolSoilScenes::liquefySoil);
    }

    private static void liquefySoil(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        BlockState florinol = ModBlocks.FLORINOL.get().defaultBlockState();

        scene.title("florinol_soil_liquefaction", "Liquefying Florinol Soil into Florinol");
        scene.configureBasePlate(1, 1, 5);
        scene.world().showSection(util.select().fromTo(1, 0, 1, 5, 0, 5), Direction.UP);
        scene.idle(10);

        // Lift a single soil block into view, then lower it into the demonstration area.
        ElementLink<WorldSectionElement> sample = scene.world()
                .showIndependentSection(util.select().position(CENTER_SAMPLE), Direction.DOWN);
        scene.world().moveSection(sample, new Vec3(0.0D, -2.0D, 0.0D), 0);
        scene.idle(20);

        showText(scene, "\u00a7d弗洛林电池液\u00a7r能量耗尽会凝固为\u00a7e弗洛林土壤\u00a7r", CENTER_POINT, 60);
        scene.idle(60);
        showText(scene, "\u00a7e弗洛林土壤\u00a7r可以液化为\u00a7d弗洛林电池液\u00a7r", CENTER_POINT, 60);
        scene.idle(60);
        showText(scene, "这个过程需要热量", CENTER_POINT, 50);
        scene.idle(20);

        scene.overlay().showControls(new Vec3(3.5D, 2.0D, 3.5D), Pointing.DOWN, 20)
                .withItem(new ItemStack(Items.FLINT_AND_STEEL));
        scene.idle(5);
        emitParticles(scene, ParticleTypes.SMOKE, 3.5D, 1.5D, 3.5D, 15.0F);
        scene.idle(50);

        scene.world().replaceBlocks(util.select().position(CENTER_SAMPLE), florinol, false);
        emitParticles(scene, ParticleTypes.FLAME, 3.5D, 1.5D, 3.5D, 15.0F);
        scene.idle(25);

        scene.world().hideIndependentSection(sample, Direction.UP);
        scene.idle(20);

        // Nearby heat sources can also trigger liquefaction in adjacent soil.
        ElementLink<WorldSectionElement>[] samples = new ElementLink[3];
        for (int i = 0; i < 3; i++) {
            samples[i] = scene.world()
                    .showIndependentSection(util.select().position(5 - 2 * i, 2, 3), Direction.DOWN);
            scene.world().moveSection(samples[i], new Vec3(0.0D, -1.0D, 0.0D), 0);
            scene.idle(5);
        }
        scene.idle(5);

        ElementLink<WorldSectionElement>[] heatSources = new ElementLink[3];
        Selection[] sourceSelections = {
                util.select().fromTo(1, 3, 3, 1, 4, 4),
                util.select().position(4, 3, 4),
                util.select().position(5, 3, 4)
        };
        for (int i = 0; i < 3; i++) {
            heatSources[i] = scene.world().showIndependentSection(sourceSelections[i], Direction.DOWN);
            scene.world().moveSection(heatSources[i],
                    new Vec3((i == 1 ? -1 : 0) + (1 - i) * 4, -2.0D, 0.0D), 0);
            scene.idle(5);
        }

        scene.idle(10);
        showText(scene, "附近的热源也能引发液化", new Vec3(3.5D, 1.5D, 4.5D), 70);
        for (int i = 0; i < 3; i++) {
            emitParticles(scene, ParticleTypes.SMOKE, 5.5D - i * 2.0D, 1.5D, 3.5D, 15.0F);
            scene.idle(5);
        }
        scene.idle(60);
        for (int i = 0; i < 3; i++) {
            emitParticles(scene, ParticleTypes.FLAME, 5.5D - i * 2.0D, 1.5D, 3.5D, 15.0F);
            scene.world().replaceBlocks(util.select().position(5 - i * 2, 2, 3), florinol, false);
            scene.idle(5);
        }
        scene.idle(20);

        for (ElementLink<WorldSectionElement> link : samples) {
            scene.world().hideIndependentSection(link, Direction.UP);
            scene.idle(2);
        }
        for (ElementLink<WorldSectionElement> link : heatSources) {
            scene.world().hideIndependentSection(link, Direction.UP);
            scene.idle(2);
        }
        scene.idle(20);

        // Reveal the complete 5x5 soil volume without showing any casting apparatus.
        for (int row = 0; row < 7; row++) {
            Selection rowSelection = null;
            for (int x = 0; x < 5; x++) {
                int z = 4 - row + Math.abs(x - 2);
                if (z >= 0 && z < 5) {
                    Selection position = util.select().position(x + 1, 1, z + 1);
                    rowSelection = rowSelection == null ? position : rowSelection.add(position);
                }
            }
            if (rowSelection != null) {
                scene.world().showSection(rowSelection, Direction.DOWN);
            }
            scene.idle(4);
        }

        scene.idle(15);
        scene.overlay().showControls(new Vec3(3.5D, 2.0D, 3.5D), Pointing.DOWN, 20)
                .withItem(new ItemStack(Items.FLINT_AND_STEEL));
        scene.addKeyframe();
        scene.idle(5);
        animateLiquefactionSpread(scene, util, florinol);
        scene.idle(25);

        scene.markAsFinished();
        scene.idle(70);
    }

    private static void animateLiquefactionSpread(CreateSceneBuilder scene, SceneBuildingUtil util,
                                                  BlockState florinol) {
        for (int distance = 0; distance <= 4; distance++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    if (Math.abs(x - 2) + Math.abs(z - 2) != distance) {
                        continue;
                    }
                    scene.world().replaceBlocks(util.select().position(x + 1, 1, z + 1), florinol, false);
                    emitParticles(scene, ParticleTypes.FLAME, 1.5D + x, 1.5D, 1.5D + z, 10.0F);
                }
            }
            if (distance == 2) {
                showText(scene, "反应开始后，液体会蔓延到所有相连的土壤",
                        new Vec3(3.5D, 1.5D, 3.5D), 55);
            }
            scene.idle(6);
        }
    }

    private static void emitParticles(CreateSceneBuilder scene, net.minecraft.core.particles.SimpleParticleType type,
                                      double x, double y, double z, float speed) {
        scene.effects().emitParticles(new Vec3(x, y, z),
                scene.effects().particleEmitterWithinBlockSpace(type, new Vec3(0.0D, 0.0D, 0.0D)),
                speed, 2);
    }

    private static void showText(CreateSceneBuilder scene, String text, Vec3 point, int duration) {
        scene.overlay().showText(duration)
                .text(text)
                .pointAt(point)
                .placeNearTarget();
    }
}
