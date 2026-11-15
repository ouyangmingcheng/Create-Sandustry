package com.minechai.create_sandustry.content.gravity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real vanilla movement/collisions with outcome suppression local to each test invocation. */
@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class GravityBouncePhysicsGameTests {
    private GravityBouncePhysicsGameTests() {}

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void gravityDecayTraceAndImpactThresholdSweep(GameTestHelper helper) {
        BlockPos surface = surface(helper);
        for (double height : new double[]{0.02, 2.0, 8.0}) {
            int previousCount = Integer.MAX_VALUE;
            for (double threshold : new double[]{0.05, 0.08, 0.10, 0.12}) {
                Trace trace = trace(helper, surface, height, 0.8, threshold, 0);
                helper.assertTrue(trace.bounces.size() <= previousCount,
                        "Raising the impact filter cannot add bounces");
                previousCount = trace.bounces.size();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void gravityAlternativeElasticitiesUseTheSameCollisionPath(GameTestHelper helper) {
        BlockPos surface = surface(helper);
        helper.assertTrue(GravityBounceSurfaces.getBounceCoefficient(helper.getLevel(), surface,
                        Blocks.SLIME_BLOCK.defaultBlockState()) == 0.8
                        && GravityBounceSurfaces.getBounceCoefficient(helper.getLevel(), surface,
                        Blocks.STONE.defaultBlockState()) == 0.0,
                "Vanilla slime remains supported and stone remains unsupported");
        for (double coefficient : new double[]{0.95, 1.0, 1.1}) {
            Trace result = trace(helper, surface, 8.0, coefficient,
                    GravityBouncePhysics.MINIMUM_IMPACT_SPEED, coefficient > 1 ? 5 : 0);
            helper.assertTrue(result.bounces.size() > 1, "Alternative elasticity produces real flights");
            if (coefficient > 1) {
                Bounce first = result.bounces.getFirst();
                helper.assertTrue(first.rebound > -first.impact,
                        "A coefficient above one adds collision energy and must not be clamped to one");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityPendingRiseSaveAndDuplicatePostDoNotInventBounces(GameTestHelper helper) {
        BlockPos surface = surface(helper);
        ItemEntity entity = falling(helper, surface, 2.0);
        AtomicInteger apices = new AtomicInteger();
        for (int i = 0; i < 100 && !entity.getData(ModAttachments.GRAVITY_BOUNCE).pendingRise(); i++)
            tick(entity, 0.8, GravityBouncePhysics.MINIMUM_IMPACT_SPEED, (e, n) -> apices.incrementAndGet());
        helper.assertTrue(entity.getData(ModAttachments.GRAVITY_BOUNCE).pendingRise()
                        && GravityMechanismData.totalBounces(entity.getItem()) == 0,
                "Collision alone does not increment either counter before a half-block ascent");
        Vec3 velocity = entity.getDeltaMovement();
        GravityBouncePhysics.onPostTick(new EntityTickEvent.Post(entity));
        helper.assertTrue(entity.getDeltaMovement().equals(velocity)
                        && GravityMechanismData.totalBounces(entity.getItem()) == 0,
                "Duplicate Post cannot apply rebound or increment counters again");
        ItemEntity restored = new ItemEntity(EntityType.ITEM, helper.getLevel());
        restored.load(entity.saveWithoutId(new CompoundTag()));
        helper.assertTrue(restored.getData(ModAttachments.GRAVITY_BOUNCE).pendingRise(),
                "Pending ascent is persisted across entity save/load");
        tick(restored, 0.8, GravityBouncePhysics.MINIMUM_IMPACT_SPEED, (e, n) -> apices.incrementAndGet());
        helper.assertTrue(GravityMechanismData.totalBounces(restored.getItem()) == 0
                        && restored.getData(ModAttachments.GRAVITY_BOUNCE).pendingRise(),
                "The first actual ascent is still too low and must not count");
        ItemEntity partial = new ItemEntity(EntityType.ITEM, helper.getLevel());
        partial.load(restored.saveWithoutId(new CompoundTag()));
        restored = partial;
        helper.assertTrue(Math.abs(restored.getData(ModAttachments.GRAVITY_BOUNCE).bounceOriginY()
                        - surface.getY() - 1) < 1e-9,
                "Saving mid-ascent retains the original collision height instead of shifting the gate");
        for (int i = 0; i < 10 && GravityMechanismData.totalBounces(restored.getItem()) == 0; i++)
            tick(restored, 0.8, GravityBouncePhysics.MINIMUM_IMPACT_SPEED, (e, n) -> apices.incrementAndGet());
        helper.assertTrue(GravityMechanismData.totalBounces(restored.getItem()) == 1
                        && restored.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "Reaching a half-block confirms exactly one permanent and consecutive bounce after save/load");

        // A ceiling interrupts the very first ascent: no complete bounce and no outcome roll.
        ItemEntity blocked = falling(helper, surface, 0.1);
        blocked.setDeltaMovement(0, -1.2, 0);
        tick(blocked, 0.8, GravityBouncePhysics.MINIMUM_IMPACT_SPEED, (e, n) -> apices.incrementAndGet());
        helper.getLevel().setBlockAndUpdate(surface.above(2), Blocks.STONE.defaultBlockState());
        tick(blocked, 0.8, GravityBouncePhysics.MINIMUM_IMPACT_SPEED, (e, n) -> apices.incrementAndGet());
        helper.assertTrue(blocked.verticalCollision && !blocked.verticalCollisionBelow
                        && GravityMechanismData.totalBounces(blocked.getItem()) == 0
                        && blocked.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0
                        && apices.get() == 0,
                "A rebound interrupted before its first unobstructed ascent cannot count or activate");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravitySafetyLimitOnlyCutsExcessSpeed(GameTestHelper helper) {
        BlockPos surface = surface(helper);
        ItemEntity entity = falling(helper, surface, 0.1);
        entity.setDeltaMovement(0, -10, 0);
        tick(entity, 1.1, GravityBouncePhysics.MINIMUM_IMPACT_SPEED, (e, n) -> {});
        helper.assertTrue(Math.abs(entity.getDeltaMovement().y
                        - ModConfigs.GRAVITY_MAXIMUM_BOUNCE_VELOCITY.get() * 0.98) < 1e-12,
                "Abnormal speed is capped independently of the coefficient");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityHalfBlockHeightControlsCounting(GameTestHelper helper) {
        BlockPos surface = surface(helper);
        double top = surface.getY() + 1.0;
        for (double impact : new double[]{0.285, 0.290}) {
            ItemEntity entity = falling(helper, surface, 0.1);
            entity.setDeltaMovement(0, -impact + entity.getGravity(), 0);
            AtomicInteger apices = new AtomicInteger();
            double apex = 0.0;
            for (int i = 0; i < 300; i++) {
                int previousTotal = GravityMechanismData.totalBounces(entity.getItem());
                tick(entity, 0.8, GravityBouncePhysics.MINIMUM_IMPACT_SPEED, (item, count) -> {
                    helper.assertTrue(item.getY() - top >= 0.5, "Only a visible half-block flight may roll an outcome");
                    apices.incrementAndGet();
                });
                apex = Math.max(apex, entity.getY() - top);
                if (GravityMechanismData.totalBounces(entity.getItem()) > previousTotal)
                    helper.assertTrue(entity.getY() - top >= 0.5, "The permanent counter cannot increase below half a block");
            }
            int expected = impact < 0.290 ? 0 : 1;
            helper.assertTrue((expected == 0 ? apex < 0.5 : apex >= 0.5)
                            && GravityMechanismData.totalBounces(entity.getItem()) == expected
                            && apices.get() == expected
                            && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                    "Sub-half-block flights must not count or roll; a visible flight counts and rolls once");
            CreateSandustry.LOGGER.info(String.format(Locale.ROOT, "GRAVITY_HEIGHT_GATE,%.3f,%.9f,%d,%d",
                    impact, apex, GravityMechanismData.totalBounces(entity.getItem()), apices.get()));
        }
        helper.succeed();
    }

    private static Trace trace(GameTestHelper helper, BlockPos surface, double height,
            double coefficient, double threshold, int stopAfterApices) {
        ItemEntity entity = falling(helper, surface, height);
        double top = surface.getY() + 1.0;
        List<Bounce> bounces = new ArrayList<>();
        AtomicInteger apices = new AtomicInteger();
        BiConsumer<ItemEntity, Integer> atApex = (item, count) -> {
            helper.assertTrue(count == Math.min(15, apices.incrementAndGet()), "One outcome callback per confirmed bounce");
            helper.assertTrue(++bounces.getLast().apexCalls == 1 && bounces.getLast().counted,
                    "Only a counted physical flight may activate, exactly once");
            GravityBounceTracker tracker = item.getData(ModAttachments.GRAVITY_BOUNCE);
            helper.assertTrue(tracker.beforeMotion.y > 0 && tracker.impactVelocity <= 0
                            && item.position().equals(tracker.beforePosition),
                    "Outcome callback occurs at the actual maximum, before the first downward move");
        };
        int settledTick = -1;
        for (int tick = 1; tick <= 1200; tick++) {
            if (!bounces.isEmpty()) bounces.getLast().apex = Math.max(bounces.getLast().apex, entity.getY() - top);
            int previousTotal = GravityMechanismData.totalBounces(entity.getItem());
            tick(entity, coefficient, threshold, atApex);
            GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
            if (tracker.pendingRise() && entity.verticalCollisionBelow) {
                double impact = tracker.impactVelocity;
                double rebound = entity.getDeltaMovement().y / GravityBouncePhysics.VERTICAL_DRAG;
                double expected = Math.min(-impact * coefficient, ModConfigs.GRAVITY_MAXIMUM_BOUNCE_VELOCITY.get());
                helper.assertTrue(Math.abs(rebound - expected) < 1e-12,
                        "Every collision uses actual pre-impact velocity times coefficient, followed by native drag");
                if (!bounces.isEmpty() && coefficient <= 1) {
                    helper.assertTrue(rebound < bounces.getLast().rebound, "Each successive rebound loses velocity");
                    helper.assertTrue(bounces.getLast().apex > 0, "Every counted flight actually rose");
                }
                bounces.add(new Bounce(tick, impact, rebound, entity.getDeltaMovement().y));
            }
            if (!bounces.isEmpty()) bounces.getLast().apex = Math.max(bounces.getLast().apex, entity.getY() - top);
            if (GravityMechanismData.totalBounces(entity.getItem()) > previousTotal) {
                helper.assertTrue(entity.getY() - top >= 0.5 && !bounces.getLast().counted,
                        "Only reaching half a block can add one effective bounce");
                bounces.getLast().counted = true;
            }
            if (stopAfterApices > 0 && apices.get() >= stopAfterApices) break;
            if (entity.onGround() && tracker.consecutiveBounces() == 0) {
                if (settledTick < 0) settledTick = tick;
                helper.assertTrue(Math.abs(entity.getY() - top) < 1e-6,
                        "Settled item stays at the exact surface height");
                long counted = bounces.stream().filter(bounce -> bounce.counted).count();
                helper.assertTrue(GravityMechanismData.totalBounces(entity.getItem()) == counted
                                && apices.get() == counted,
                        "Settled ticks never add wear, consecutive count or apex callbacks");
                if (tick - settledTick >= 200) break;
            } else {
                helper.assertTrue(settledTick < 0, "A settled item must never spontaneously relaunch");
            }
        }
        if (stopAfterApices == 0) helper.assertTrue(settledTick > 0, "Natural trajectory must come to rest");
        for (int i = 0; i < bounces.size(); i++) {
            Bounce bounce = bounces.get(i);
            helper.assertTrue(bounce.counted == (bounce.apex >= 0.5),
                    "Each recorded physical flight counts if and only if its actual apex reaches half a block");
            if (i > 0 && coefficient <= 1)
                helper.assertTrue(bounce.apex < bounces.get(i - 1).apex, "Successive physical apex heights decrease");
            CreateSandustry.LOGGER.info(String.format(Locale.ROOT,
                    "GRAVITY_DECAY,%.2f,%.2f,%.2f,%d,%d,%.9f,%.9f,%.9f,%.9f,%.3f,%.9f,%s",
                    coefficient, threshold, height, i + 1, bounce.tick, bounce.impact,
                    bounce.rebound, bounce.afterDrag, bounce.apex, top, top + bounce.apex, bounce.counted));
        }
        CreateSandustry.LOGGER.info(String.format(Locale.ROOT, "GRAVITY_SETTLED,%.2f,%.2f,%.2f,%d,%d,%d,%d",
                coefficient, threshold, height, bounces.size(), GravityMechanismData.totalBounces(entity.getItem()),
                apices.get(), settledTick));
        return new Trace(bounces);
    }

    private static void tick(ItemEntity entity, double coefficient, double threshold,
            BiConsumer<ItemEntity, Integer> atApex) {
        // Match ServerLevel.tickNonPassenger's order; direct ItemEntity.tick avoids posting
        // the production callback as well as the test callback on the same entity.
        entity.tickCount++;
        GravityBouncePhysics.beforeTick(entity, atApex);
        entity.tick();
        GravityBouncePhysics.afterTick(entity, (level, pos, state) -> state.is(Blocks.SLIME_BLOCK) ? coefficient : 0,
                threshold);
    }

    private static BlockPos surface(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SLIME_BLOCK.defaultBlockState());
        for (int y = 1; y <= 16; y++)
            helper.getLevel().setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
        return pos;
    }

    private static ItemEntity falling(GameTestHelper helper, BlockPos surface, double height) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), surface.getX() + 0.5, surface.getY() + 1 + height,
                surface.getZ() + 0.5, GravityMechanismAssembly.createIncomplete(helper.getLevel()));
        entity.setDeltaMovement(Vec3.ZERO);
        return entity;
    }

    private record Trace(List<Bounce> bounces) {}

    private static final class Bounce {
        final int tick;
        final double impact;
        final double rebound;
        final double afterDrag;
        double apex;
        boolean counted;
        int apexCalls;

        Bounce(int tick, double impact, double rebound, double afterDrag) {
            this.tick = tick;
            this.impact = impact;
            this.rebound = rebound;
            this.afterDrag = afterDrag;
        }
    }
}
