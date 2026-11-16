package com.minechai.create_sandustry.content.gravity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.compat.sable.SableMoonhopBounce;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.mojang.authlib.GameProfile;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/** Invoked only when Sable is installed. Uses real remote plots, SAT and native rigid bodies. */
public final class SableMoonhopGameTests {
    private SableMoonhopGameTests() {}

    public static void impacts(GameTestHelper helper) {
        int cases = 0;
        for (double angle : new double[]{0, 25, 65}) {
            for (Vec3 linear : new Vec3[]{Vec3.ZERO, new Vec3(0.13, 0, 0), new Vec3(0, 0.12, 0), new Vec3(0, -0.09, 0)}) {
                try (Fixture f = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), angle)) {
                    f.velocity(linear, angle == 0 ? Vec3.ZERO : new Vec3(0, 0, 0.035));
                    for (int kind = 0; kind < 4; kind++) {
                        Entity entity = entity(helper, kind, new ItemStack(Items.COBBLESTONE));
                        Vec3 normal = f.normal();
                        Vec3 incoming = f.pointVelocity().add(normal.scale(-0.3)).add(0, 0, 0.07);
                        f.placeAbove(entity, 0.02);
                        entity.setDeltaMovement(incoming);
                        entity.move(MoverType.SELF, incoming);
                        var contact = ((SableMoonhopBounce.ContactAccess) entity).createsandustry$moonhopContact();
                        Vec3 at = contact.lastLocalPoint();
                        Vector3d measured = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getVelocity(
                                entity.level(), f.surface, new Vector3d(at.x, at.y, at.z), new Vector3d()).mul(0.05);
                        Vec3 surfaceVelocity = new Vec3(measured.x, measured.y, measured.z);
                        double relative = incoming.subtract(surfaceVelocity).dot(normal);
                        double rebound = Math.max(-relative * (entity instanceof LivingEntity ? 1 : 0.8), 0.42);
                        Vec3 expected = incoming.add(normal.scale(rebound - relative));
                        helper.assertTrue(entity.getDeltaMovement().distanceTo(expected) < 1e-6,
                                "SAT reflection must use relative normal and preserve tangent: angle=" + angle
                                        + " linear=" + linear + " kind=" + kind + " actual=" + entity.getDeltaMovement()
                                        + " expected=" + expected);
                        helper.assertTrue(!entity.onGround()
                                        && ((EntityMovementExtension) entity).sable$getTrackingSubLevel() == null,
                                "Launch detaches from Sable motion inheritance");
                        if (entity instanceof ServerPlayer) helper.assertTrue(entity.hurtMarked,
                                "Server player receives authoritative velocity synchronization");
                        cases++;
                    }
                }
            }
        }
        CreateSandustry.LOGGER.info("MOONHOP_SABLE_IMPACTS,cases={}", cases);
        helper.succeed();
    }

    public static void flights(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), 0)) {
            for (int kind = 0; kind < 3; kind++) {
                Entity entity = entity(helper, kind, new ItemStack(Items.COBBLESTONE));
                f.placeAbove(entity, 5);
                entity.setDeltaMovement(Vec3.ZERO);
                int bounces = 0;
                for (int tick = 0; tick < 1200; tick++) {
                    double before = entity.getDeltaMovement().y;
                    entity.tickCount++;
                    entity.tick();
                    if (before < 0 && entity.getDeltaMovement().y > 0.3) bounces++;
                }
                helper.assertTrue(bounces > 45, "Repeated real SubLevel flights for kind=" + kind + ": " + bounces
                        + " pos=" + entity.position() + " motion=" + entity.getDeltaMovement() + " top=" + f.surface.logicalPose());
                CreateSandustry.LOGGER.info("MOONHOP_SABLE_FLIGHTS,kind={},bounces={}", kind, bounces);
            }
            ItemEntity mechanism = (ItemEntity) entity(helper, 0, GravityMechanismAssembly.createIncomplete(helper.getLevel()));
            f.placeAbove(mechanism, 0.02);
            mechanism.setDeltaMovement(Vec3.ZERO);
            AtomicInteger apices = new AtomicInteger();
            int collisions = 0;
            for (int tick = 0; tick < 1400 && apices.get() < 40; tick++) {
                mechanism.tickCount++;
                GravityBouncePhysics.beforeTick(mechanism, (item, count) -> {
                    helper.assertTrue(count == Math.min(15, apices.incrementAndGet()), "One processing callback per apex");
                });
                mechanism.tick();
                if (mechanism.getData(ModAttachments.GRAVITY_BOUNCE).physicalContactTick == mechanism.tickCount) collisions++;
                Vec3 motion = mechanism.getDeltaMovement();
                GravityBouncePhysics.afterTick(mechanism, GravityBounceSurfaces::getBounceCoefficient,
                        GravityBouncePhysics.MINIMUM_IMPACT_SPEED);
                helper.assertTrue(mechanism.getDeltaMovement().equals(motion), "Observer cannot apply another impulse");
            }
            helper.assertTrue(apices.get() == 40 && collisions == 40
                            && GravityMechanismData.totalBounces(mechanism.getItem()) == 40
                            && GravityMechanismData.interruptions(mechanism.getItem()) == 0,
                    "Forty physical mechanism flights, no double counting/disconnection: collisions=" + collisions
                            + " apices=" + apices + " total=" + GravityMechanismData.totalBounces(mechanism.getItem())
                            + " interruptions=" + GravityMechanismData.interruptions(mechanism.getItem()));
            CreateSandustry.LOGGER.info("MOONHOP_SABLE_GRAVITY,collisions={},apices={}", collisions, apices);
        }
        helper.succeed();
    }

    public static void exclusions(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), 0)) {
            Entity server = entity(helper, 3, ItemStack.EMPTY);
            f.placeAbove(server, 0.3);
            server.setDeltaMovement(0, -0.3, 0);
            server.move(MoverType.SELF, server.getDeltaMovement());
            helper.assertTrue(Math.abs(server.getDeltaMovement().y - 0.42) < 1e-9,
                    "Server SAT also accepts a client-clipped position exactly touching the top");

            LivingEntity falling = (LivingEntity) entity(helper, 2, ItemStack.EMPTY);
            f.placeAbove(falling, 0.02);
            falling.fallDistance = 10;
            falling.setDeltaMovement(0, -0.3, 0);
            float health = falling.getHealth();
            falling.move(MoverType.SELF, falling.getDeltaMovement());
            helper.assertTrue(falling.getHealth() == health, "Physical moonhop retains slime's fall damage immunity");

            Entity crouching = entity(helper, 2, ItemStack.EMPTY);
            f.placeAbove(crouching, 0.02);
            crouching.setShiftKeyDown(true);
            crouching.fallDistance = 10;
            crouching.setDeltaMovement(0, -0.3, 0);
            crouching.move(MoverType.SELF, crouching.getDeltaMovement());
            helper.assertTrue(crouching.getDeltaMovement().y <= 0, "Sneaking suppresses physical bounce");
            helper.assertTrue(((LivingEntity) crouching).getHealth() < health,
                    "Physical sneaking preserves the vanilla slime fall damage rule");

            ItemEntity item = (ItemEntity) entity(helper, 0, new ItemStack(Items.COBBLESTONE));
            Vec3 center = f.surface.logicalPose().transformPosition(Vec3.atCenterOf(f.block));
            item.setPos(center.x + 2.5 + item.getBbWidth() / 2 + 0.02, center.y - 0.1, center.z);
            item.setDeltaMovement(-0.5, 0, 0);
            item.move(MoverType.SELF, item.getDeltaMovement());
            helper.assertTrue(item.getDeltaMovement().y <= 0, "Physical side cannot launch item");
            item.setPos(center.x, center.y - 0.8, center.z);
            item.setDeltaMovement(0, 0.4, 0);
            item.move(MoverType.SELF, item.getDeltaMovement());
            helper.assertTrue(item.getDeltaMovement().y <= 0.4, "Physical underside cannot add bounce");

            // Old inherited velocity must be transferred once, then removed from Sable's side channel.
            LivingEntity living = (LivingEntity) entity(helper, 1, ItemStack.EMPTY);
            f.placeAbove(living, 0.02);
            ((LivingEntityMovementExtension) living).sable$getInheritedVelocity().set(0.11, 0, 0.08);
            living.setDeltaMovement(0, -0.3, 0);
            living.move(MoverType.SELF, living.getDeltaMovement());
            helper.assertTrue(living.getDeltaMovement().distanceTo(new Vec3(0.11, 0.42, 0.08)) < 1e-6
                            && ((LivingEntityMovementExtension) living).sable$getInheritedVelocity().lengthSquared() == 0,
                    "Inherited motion is not lost or added twice");
        }
        for (Block obstruction : new Block[]{Blocks.STONE, Blocks.WATER}) {
            try (Fixture f = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), 0)) {
                helper.getLevel().setBlock(f.block.above(obstruction == Blocks.STONE ? 2 : 1), obstruction.defaultBlockState(), 3);
                f.surface.getPlot().updateBoundingBox();
                f.surface.updateBoundingBox();
                f.surface.updateLastPose();
                Entity entity = entity(helper, 1, ItemStack.EMPTY);
                f.placeAbove(entity, 0.02);
                entity.setDeltaMovement(0, -0.3, 0);
                entity.move(MoverType.SELF, entity.getDeltaMovement());
                helper.assertTrue(((SableMoonhopBounce.ContactAccess) entity).createsandustry$moonhopContact().bounceCount() == 0,
                        "A physical ceiling/fluid prevents takeoff: " + obstruction);
            }
        }
        try (Fixture f = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), 25)) {
            Entity side = entity(helper, 1, ItemStack.EMPTY);
            var pose = f.surface.logicalPose();
            Vec3 n = f.normal();
            double halfWidth = side.getBbWidth() / 2;
            double halfHeight = side.getBbHeight() / 2;
            double radiusY = Math.abs(n.x) * halfWidth + Math.abs(n.y) * halfHeight;
            Vec3 right = pose.transformNormal(new Vec3(1, 0, 0));
            double radiusX = Math.abs(right.x) * halfWidth + Math.abs(right.y) * halfHeight;
            Vec3 center = pose.transformPosition(new Vec3(f.block.getX() - 2 - radiusX - 0.02,
                    f.block.getY() + 1 + radiusY - 0.08, f.block.getZ() + 0.5));
            side.setPos(center.add(0, -halfHeight, 0));
            side.setDeltaMovement(0.4, 0, 0);
            side.move(MoverType.SELF, side.getDeltaMovement());
            helper.assertTrue(((SableMoonhopBounce.ContactAccess) side).createsandustry$moonhopContact().bounceCount() == 0,
                    "Entering the side edge below a tilted top cannot trigger its minimum impulse");
        }
        for (Block block : new Block[]{Blocks.STONE, Blocks.SLIME_BLOCK}) {
            try (Fixture f = new Fixture(helper, block, 0)) {
                Entity item = entity(helper, 0, new ItemStack(Items.COBBLESTONE));
                f.placeAbove(item, 0.02);
                item.setDeltaMovement(0, -0.2, 0);
                item.move(MoverType.SELF, item.getDeltaMovement());
                helper.assertTrue(item.getDeltaMovement().y < 0.42, "No moonhop minimum on " + block);
            }
        }
        try (Fixture first = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), 0);
             Fixture second = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), 0)) {
            second.advance(new Vec3(12, 0, 0), Vec3.ZERO);
            second.surface.updateLastPose();
            second.velocity(new Vec3(-12, 0, 0), Vec3.ZERO);
            Entity item = entity(helper, 0, new ItemStack(Items.COBBLESTONE));
            for (Fixture f : new Fixture[]{first, second}) {
                f.placeAbove(item, 0.02);
                item.setDeltaMovement(0, -0.3, 0);
                item.move(MoverType.SELF, item.getDeltaMovement());
            }
            var state = ((SableMoonhopBounce.ContactAccess) item).createsandustry$moonhopContact();
            helper.assertTrue(state.bounceCount() == 2, "Changing structures cannot retain the old contact latch");
            item.setPos(item.position().add(20, 3, 0));
            item.setDeltaMovement(0.1, 0.1, 0);
            item.move(MoverType.SELF, item.getDeltaMovement());
            helper.assertTrue(state.bounceCount() == 2 && item.getDeltaMovement().equals(new Vec3(0.1, 0.1, 0)),
                    "Leaving both structures removes all moonhop impulse");
        }
        helper.succeed();
    }

    public static void movingFlights(GameTestHelper helper) {
        for (int scenario = 0; scenario < 4; scenario++) {
            Vec3 translation = switch (scenario) {
                case 0 -> new Vec3(0.006, 0, 0);
                case 1 -> new Vec3(0, 0.025, 0);
                case 2 -> new Vec3(0, -0.025, 0);
                default -> Vec3.ZERO;
            };
            Vec3 angular = scenario == 3 ? new Vec3(0, 0, 0.002) : Vec3.ZERO;
            for (int kind = 0; kind < 4; kind++) {
                try (Fixture f = new Fixture(helper, ModBlocks.MOONHOP_BLOCK.get(), 0, 6)) {
                    Entity entity = kind == 3
                            ? entity(helper, 0, GravityMechanismAssembly.createIncomplete(helper.getLevel()))
                            : entity(helper, kind, new ItemStack(Items.COBBLESTONE));
                    f.placeAbove(entity, 0.05);
                    entity.setDeltaMovement(translation);
                    var state = ((SableMoonhopBounce.ContactAccess) entity).createsandustry$moonhopContact();
                    for (int tick = 0; tick < 180 && state.bounceCount() < 5; tick++) {
                        f.advance(translation, angular);
                        entity.tickCount++;
                        if (kind == 3) GravityBouncePhysics.beforeTick((ItemEntity) entity, (item, count) -> {});
                        entity.tick();
                        if (kind == 3) GravityBouncePhysics.afterTick((ItemEntity) entity,
                                GravityBounceSurfaces::getBounceCoefficient, GravityBouncePhysics.MINIMUM_IMPACT_SPEED);
                        helper.assertTrue(entity.getDeltaMovement().length() < 2,
                                "No growing energy/duplicated structure velocity");
                    }
                    helper.assertTrue(state.bounceCount() >= 5, "Five real moving/rotating SubLevel contacts, scenario="
                            + scenario + " kind=" + kind + " count=" + state.bounceCount() + " pos=" + entity.position()
                            + " motion=" + entity.getDeltaMovement() + " alive=" + entity.isAlive() + " pose=" + f.surface.logicalPose());
                    if (kind == 3) helper.assertTrue(GravityMechanismData.interruptions(((ItemEntity) entity).getItem()) == 0,
                            "Moving top contact must not disconnect mechanism, scenario=" + scenario);
                    CreateSandustry.LOGGER.info("MOONHOP_SABLE_MOVING,scenario={},kind={},bounces={}", scenario, kind, state.bounceCount());
                }
            }
        }
        helper.succeed();
    }

    private static Entity entity(GameTestHelper helper, int kind, ItemStack stack) {
        return switch (kind) {
            case 0 -> new ItemEntity(helper.getLevel(), 0, 0, 0, stack);
            case 1 -> new Cow(EntityType.COW, helper.getLevel()) {{
                goalSelector.removeAllGoals(goal -> true);
                targetSelector.removeAllGoals(goal -> true);
            }};
            case 2 -> helper.makeMockPlayer(GameType.SURVIVAL);
            default -> new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "MoonhopTest"), ClientInformation.createDefault());
        };
    }

    private static final class Fixture implements AutoCloseable {
        private static int nextPlot;
        final ServerSubLevel surface;
        final BlockPos block;

        Fixture(GameTestHelper helper, Block material, double angle) {
            this(helper, material, angle, 2);
        }

        Fixture(GameTestHelper helper, Block material, double angle, int radius) {
            var container = SubLevelContainer.getContainer(helper.getLevel());
            int side = 1 << container.getLogSideLength();
            int index;
            do { index = nextPlot++; } while (container.getOccupancy().get(index));
            // Native rigid bodies need loaded world chunks. Keep these manual-tick fixtures
            // independent of GameTest's random, sometimes multi-million-block template origin.
            BlockPos at = new BlockPos(1024 + index % 64 * 48, 80, 1024 + index / 64 * 48);
            for (int x = (at.getX() - 8) >> 4; x <= (at.getX() + 8) >> 4; x++) {
                for (int z = (at.getZ() - 8) >> 4; z <= (at.getZ() + 8) >> 4; z++) helper.getLevel().getChunk(x, z);
            }
            Pose3d pose = new Pose3d();
            pose.position().set(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            pose.orientation().rotateZ(Math.toRadians(angle));
            surface = (ServerSubLevel) container.allocateSubLevel(UUID.randomUUID(), index % side, index / side, pose);
            block = surface.getPlot().getCenterBlock();
            surface.logicalPose().rotationPoint().set(block.getX() + 0.5, block.getY() + 1.0, block.getZ() + 0.5);
            surface.updateLastPose();
            surface.getPlot().newEmptyChunk(new ChunkPos(block));
            for (BlockPos pos : BlockPos.betweenClosed(block.offset(-radius, 0, -radius), block.offset(radius, 0, radius))) {
                helper.getLevel().setBlock(pos, material.defaultBlockState(), 3);
            }
            surface.getPlot().updateBoundingBox();
            surface.updateBoundingBox();
            surface.forceUpdateGlobalBounds();
            RigidBodyHandle.of(surface).teleport(surface.logicalPose().position(), surface.logicalPose().orientation());
            surface.updateLastPose(); // Block placement changes the rigid body's centre of mass/pose.
        }

        Vec3 normal() { return surface.logicalPose().transformNormal(new Vec3(0, 1, 0)); }

        Vec3 pointVelocity() {
            var velocity = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getVelocity(surface.getLevel(), surface,
                    new Vector3d(block.getX() + 0.5, block.getY() + 1.0, block.getZ() + 0.5), new Vector3d());
            return new Vec3(velocity.x, velocity.y, velocity.z).scale(0.05);
        }

        void velocity(Vec3 linear, Vec3 angular) {
            RigidBodyHandle.of(surface).addLinearAndAngularVelocity(new Vector3d(linear.x, linear.y, linear.z).mul(20),
                    new Vector3d(angular.x, angular.y, angular.z).mul(20));
        }

        void placeAbove(Entity entity, double gap) {
            Vec3 n = normal();
            double radius = Math.abs(n.x) * entity.getBbWidth() / 2 + Math.abs(n.y) * entity.getBbHeight() / 2
                    + Math.abs(n.z) * entity.getBbWidth() / 2;
            Vec3 top = surface.logicalPose().transformPosition(new Vec3(block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5));
            entity.setPos(top.add(n.scale(radius + gap)).add(0, -entity.getBbHeight() / 2, 0));
        }

        void advance(Vec3 linear, Vec3 angular) {
            surface.updateLastPose();
            surface.logicalPose().position().add(linear.x, linear.y, linear.z);
            surface.logicalPose().orientation().rotateLocalZ(angular.z);
            var handle = RigidBodyHandle.of(surface);
            handle.teleport(surface.logicalPose().position(), surface.logicalPose().orientation());
            var pipeline = SubLevelContainer.getContainer(surface.getLevel()).physicsSystem().getPipeline();
            pipeline.resetVelocity(surface);
            velocity(linear, angular);
            surface.updateBoundingBox();
        }

        @Override public void close() {
            SubLevelContainer.getContainer(surface.getLevel()).removeSubLevel(surface, SubLevelRemovalReason.REMOVED);
        }
    }
}
