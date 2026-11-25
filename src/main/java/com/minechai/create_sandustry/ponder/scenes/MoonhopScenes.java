package com.minechai.create_sandustry.ponder.scenes;

import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModMobEffects;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Teaches how Moon Crystals affect rabbits and eventually produce a Moonhop Fragment.
 */
public final class MoonhopScenes {
    private static final ResourceLocation MOON_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath("createsandustry", "mooncrystal");
    private static final ResourceLocation STRUCTURE =
            ResourceLocation.fromNamespaceAndPath("createsandustry", "lunar_conversion/basic");
    private static final int MOON_WORSHIP_DURATION = 20 * 180;

    private MoonhopScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(MOON_CRYSTAL)
                .addStoryBoard(STRUCTURE, MoonhopScenes::moonCrystalRabbitRitual);
    }

    private static void moonCrystalRabbitRitual(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("moonhop_conversion", "Moonhop Ritual");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);

        BlockPos rabbitPos = util.grid().at(3, 1, 2);
        BlockPos wolfPos = util.grid().at(1, 1, 2);
        BlockPos middleFence = util.grid().at(2, 1, 2);
        ItemStack moonCrystal = new ItemStack(ModItems.MOONCRYSTAL.get());
        Set<BlockPos> rabbitFences = rabbitRing(util);
        Set<BlockPos> wolfFences = wolfOnlyRing(util);
        Set<BlockPos> allFences = new LinkedHashSet<>(rabbitFences);
        allFences.addAll(wolfFences);

        // 1. Rabbit and its enclosure.
        ElementLink<EntityElement> rabbitLink = scene.world().createEntity(level -> {
            Rabbit rabbit = Objects.requireNonNull(EntityType.RABBIT.create(level));
            rabbit.setVariant(Rabbit.Variant.BROWN);
            rabbit.setNoAi(true);
            rabbit.setNoGravity(true);
            rabbit.moveTo(rabbitPos.getX() + 0.5D, rabbitPos.getY(), rabbitPos.getZ() + 0.5D, 180.0F, 0.0F);
            face(rabbit, 180.0F);
            return rabbit;
        });
        scene.idle(20);
        placeFences(scene, util, rabbitFences, rabbitFences);
        scene.addKeyframe();
        scene.idle(35);

        // 2. Explain the jump requirement.
        Vec3 rabbitPoint = rabbitPoint(rabbitPos);
        showText(scene, "兔子在食用月晶石碎片后跳跃指定次数，\n概率产出月跃石", rabbitPoint, 65);
        scene.idle(75);

        // 3. Feed the first Moon Crystal and show the Moon Worship effect.
        scene.overlay().showControls(rabbitPoint, Pointing.DOWN, 55).rightClick().withItem(moonCrystal.copy());
        applyMoonWorship(scene, rabbitLink);
        emitMoonWorshipParticles(scene, rabbitPoint, 225);
        scene.addKeyframe();
        scene.idle(75);

        // 4. Add a tamed, sitting wolf and only the non-overlapping wolf fences.
        ElementLink<EntityElement> wolfLink = scene.world().createEntity(level -> {
            Wolf wolf = Objects.requireNonNull(EntityType.WOLF.create(level));
            wolf.setTame(true, true);
            wolf.setOrderedToSit(true);
            wolf.setInSittingPose(true);
            wolf.setNoAi(true);
            wolf.setNoGravity(true);
            wolf.moveTo(wolfPos.getX() + 0.5D, wolfPos.getY(), wolfPos.getZ() + 0.5D, 270.0F, 0.0F);
            face(wolf, 270.0F);
            return wolf;
        });
        placeFences(scene, util, wolfFences, allFences);
        refreshFenceConnections(scene, allFences);
        scene.idle(5);
        scene.addKeyframe();
        scene.idle(55);

        // 5. Open the shared fence between them.
        scene.world().destroyBlock(middleFence);
        refreshFenceConnections(scene, withoutFence(allFences, middleFence));
        scene.idle(25);

        // 6. The rabbit faces away from the wolf and starts hopping.
        scene.world().modifyEntity(rabbitLink, entity -> face(entity, 270.0F));
        scene.idle(10);
        animateHops(scene, rabbitLink, new Vec3(rabbitPos.getX() + 0.5D, rabbitPos.getY(), rabbitPos.getZ() + 0.5D),
                0.0D, 5, 8, 2);

        // 7. Complete the ritual and drop the Moonhop Fragment.
        scene.world().modifyEntity(rabbitLink, entity -> {
            if (entity instanceof LivingEntity living) {
                living.removeEffect(ModMobEffects.MOON_WORSHIP);
            }
            if (entity instanceof Rabbit rabbit) {
                rabbit.setJumping(false);
            }
        });
        ElementLink<EntityElement> moonhopLink = scene.world().createItemEntity(new Vec3(rabbitPos.getX() + 0.5D, rabbitPos.getY() + 0.5D,
                rabbitPos.getZ() + 0.5D), new Vec3(0.0D, 0.12D, 0.0D),
                new ItemStack(ModItems.MOONHOP.get()));
        scene.effects().emitParticles(rabbitPoint, scene.effects()
                .particleEmitterWithinBlockSpace(ParticleTypes.END_ROD, Vec3.ZERO), 1.0F, 12);
        scene.addKeyframe();
        scene.idle(40);

        // 8. Put the middle fence back.
        placeBlock(scene, middleFence, fenceState(middleFence, allFences));
        refreshFenceConnections(scene, allFences);
        scene.idle(35);

        // 9. Rabbit stops jumping.
        scene.world().modifyEntity(rabbitLink, entity -> {
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity instanceof Rabbit rabbit) {
                rabbit.setJumping(false);
            }
        });
        scene.world().modifyEntity(moonhopLink, Entity::discard);
        scene.idle(15);


        // 10. Set up the repeated-feeding warning.
        scene.idle(15);
        showText(scene, "如果连续喂食……", rabbitPoint, 40);
        scene.idle(50);

        // 11. Feed a second Moon Crystal while the effect is active.
        scene.overlay().showControls(rabbitPoint, Pointing.DOWN, 50).rightClick().withItem(moonCrystal.copy());
        applyMoonWorship(scene, rabbitLink);
        emitMoonWorshipParticles(scene, rabbitPoint, 200);
        scene.addKeyframe();
        scene.idle(60);

        // 12. Open the path again and show the second right-click.
        scene.world().destroyBlock(middleFence);
        refreshFenceConnections(scene, withoutFence(allFences, middleFence));
        scene.idle(20);
        scene.overlay().showControls(rabbitPoint, Pointing.DOWN, 55).rightClick().withItem(moonCrystal.copy());
        scene.idle(30);

        // 13. The rabbit becomes hostile, attacks the wolf, and is killed by the wolf.
        scene.world().modifyEntity(rabbitLink, entity -> {
            if (entity instanceof Rabbit rabbit) {
                rabbit.setVariant(Rabbit.Variant.EVIL);
                rabbit.setJumping(true);
            }
            face(entity, 90.0F);
        });
        scene.effects().emitParticles(rabbitPoint, scene.effects()
                .simpleParticleEmitter(ParticleTypes.ANGRY_VILLAGER, Vec3.ZERO), 1.0F, 8);
        scene.idle(15);
        animateHops(scene, rabbitLink, new Vec3(rabbitPos.getX() + 0.5D, rabbitPos.getY(), rabbitPos.getZ() + 0.5D),
                -0.11D, 5, 6, 1);

        scene.world().modifyEntity(wolfLink, entity -> {
            if (entity instanceof Wolf wolf) {
                wolf.setOrderedToSit(false);
                wolf.setInSittingPose(false);
                wolf.setNoAi(true);
                wolf.setPos(wolfPos.getX() + 0.9D, wolfPos.getY(), wolfPos.getZ() + 0.5D);
                face(entity, 270.0F);
            }
        });
        scene.idle(12);
        scene.effects().emitParticles(rabbitPoint.add(0.5D, 0.3D, 0.0D), scene.effects()
                .particleEmitterWithinBlockSpace(ParticleTypes.CRIT, Vec3.ZERO), 1.0F, 6);
        scene.world().modifyEntity(rabbitLink, Entity::kill);
        scene.idle(18);
        showText(scene, "兔子会发疯！", centerOf(middleFence), 60);
        scene.idle(70);
    }

    private static Set<BlockPos> rabbitRing(SceneBuildingUtil util) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(util.grid().at(4, 1, 1));
        positions.add(util.grid().at(3, 1, 1));
        positions.add(util.grid().at(2, 1, 1));
        positions.add(util.grid().at(4, 1, 2));
        positions.add(util.grid().at(2, 1, 2));
        positions.add(util.grid().at(4, 1, 3));
        positions.add(util.grid().at(3, 1, 3));
        positions.add(util.grid().at(2, 1, 3));
        return positions;
    }

    private static Set<BlockPos> wolfOnlyRing(SceneBuildingUtil util) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(util.grid().at(1, 1, 1));
        positions.add(util.grid().at(0, 1, 1));
        positions.add(util.grid().at(0, 1, 2));
        positions.add(util.grid().at(1, 1, 3));
        positions.add(util.grid().at(0, 1, 3));
        return positions;
    }

    private static void placeFences(CreateSceneBuilder scene, SceneBuildingUtil util,
                                    Set<BlockPos> positionsToPlace, Set<BlockPos> allPositions) {
        Selection selection = null;
        for (BlockPos pos : positionsToPlace) {
            scene.world().setBlock(pos, fenceState(pos, allPositions), false);
            Selection cell = util.select().position(pos);
            selection = selection == null ? cell : selection.add(cell);
        }
        if (selection != null) {
            Selection finalSelection = selection;
            scene.addInstruction(ponderScene -> ponderScene.forEach(WorldSectionElement.class, section -> {
                if (!section.isEmpty()) {
                    section.erase(finalSelection);
                }
            }));
            scene.world().showSection(finalSelection, Direction.DOWN);
        }
    }

    private static void refreshFenceConnections(CreateSceneBuilder scene, Set<BlockPos> positions) {
        for (BlockPos pos : positions) {
            scene.world().setBlock(pos, fenceState(pos, positions), false);
        }
        scene.addInstruction(ponderScene ->
                ponderScene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw));
    }

    private static BlockState fenceState(BlockPos pos, Set<BlockPos> positions) {
        return Blocks.OAK_FENCE.defaultBlockState()
                .setValue(FenceBlock.NORTH, positions.contains(pos.north()))
                .setValue(FenceBlock.EAST, positions.contains(pos.east()))
                .setValue(FenceBlock.SOUTH, positions.contains(pos.south()))
                .setValue(FenceBlock.WEST, positions.contains(pos.west()));
    }

    private static Set<BlockPos> withoutFence(Set<BlockPos> positions, BlockPos removed) {
        Set<BlockPos> result = new LinkedHashSet<>(positions);
        result.remove(removed);
        return result;
    }

    private static void placeBlock(CreateSceneBuilder scene, BlockPos pos, BlockState state) {
        placeBlocks(scene, scene.getScene().getSceneBuildingUtil().select().position(pos), state);
    }

    private static void placeBlocks(CreateSceneBuilder scene, Selection selection, BlockState state) {
        scene.world().setBlocks(selection, state, false);
        scene.addInstruction(ponderScene -> ponderScene.forEach(WorldSectionElement.class, section -> {
            if (!section.isEmpty()) {
                section.erase(selection);
            }
        }));
        scene.world().showSection(selection, Direction.DOWN);
    }

    private static void applyMoonWorship(CreateSceneBuilder scene, ElementLink<EntityElement> rabbitLink) {
        scene.world().modifyEntity(rabbitLink, entity -> {
            if (entity instanceof LivingEntity living) {
                living.forceAddEffect(new MobEffectInstance(
                        ModMobEffects.MOON_WORSHIP,
                        MOON_WORSHIP_DURATION,
                        0,
                        false,
                        true,
                        true), living);
            }
        });
    }

    private static void emitMoonWorshipParticles(CreateSceneBuilder scene, Vec3 point, int cycles) {
        scene.effects().emitParticles(point, scene.effects()
                .simpleParticleEmitter(ParticleTypes.EFFECT, new Vec3(0.0D, 0.035D, 0.0D)), 0.8F, cycles);
    }

    private static void animateHops(CreateSceneBuilder scene, ElementLink<EntityElement> rabbitLink,
                                    Vec3 start, double xStep, int hops, int hopTicks, int pauseTicks) {
        for (int hop = 1; hop <= hops; hop++) {
            double segmentStartX = start.x + xStep * (hop - 1);
            for (int tick = 1; tick <= hopTicks; tick++) {
                double progress = tick / (double) hopTicks;
                double x = segmentStartX + xStep * progress;
                double y = start.y + Math.sin(progress * Math.PI) * 0.55D;
                boolean jumping = tick < hopTicks;
                scene.world().modifyEntity(rabbitLink, entity -> {
                    entity.setPos(x, y, start.z);
                    if (entity instanceof Rabbit rabbit) {
                        rabbit.setJumping(jumping);
                    }
                });
                scene.idle(1);
            }
            if (pauseTicks > 0) {
                scene.idle(pauseTicks);
            }
        }
    }

    private static void face(Entity entity, float yaw) {
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }

    private static Vec3 centerOf(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }
    private static Vec3 rabbitPoint(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D);
    }

    private static void showText(CreateSceneBuilder scene, String text, Vec3 point, int duration) {
        scene.overlay().showText(duration)
                .text(text)
                .pointAt(point)
                .placeNearTarget();
    }
}
