package com.minechai.create_sandustry.content.gravity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class GravityPistonGameTests {
    private GravityPistonGameTests() {}

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void nativePistonLaunchPreservesEnergyAndActiveContinuity(GameTestHelper helper) {
        ItemEntity entity = GravityInterruptionGameTests.active(helper);
        AtomicInteger apices = new AtomicInteger();
        for (int i = 0; i < 100 && !canCatch(entity); i++)
            GravityInterruptionGameTests.tick(entity, (item, count) -> apices.incrementAndGet());
        helper.assertTrue(canCatch(entity), "Fixture reaches a falling active trajectory");
        helper.getLevel().addFreshEntity(entity);
        PistonMovingBlockEntity piston = piston(helper, BlockPos.containing(entity.position()));
        push(helper, piston);
        helper.assertTrue(entity.getDeltaMovement().y == 1.0
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1
                        && GravityMechanismData.interruptions(entity.getItem()) == 0,
                "Vanilla upward slime sets Vy=1 without reducing energy, ending continuity or immediately counting");
        GravityInterruptionGameTests.tick(entity, (item, count) -> apices.incrementAndGet());
        helper.assertTrue(Math.abs(entity.getDeltaMovement().y - .9408) < 1e-12
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 2
                        && GravityMechanismData.interruptions(entity.getItem()) == 0 && apices.get() == 1,
                "Native impulse keeps gravity/drag, adds one new half-block collision flight, and is never an apex");
        entity.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void nativeSlimeBoostWithoutPushDoesNotAddABounce(GameTestHelper helper) {
        BlockPos surface = GravityInterruptionGameTests.surface(helper);
        ItemEntity entity = GravityInterruptionGameTests.falling(helper, surface,
                GravityMechanismAssembly.createIncomplete(helper.getLevel()), 0);
        BlockPos dest = surface.above();
        // Vanilla's broad query includes the old cube; its swept leading face does not.
        entity.setPos(dest.getX() + .5, dest.getY() - .6, dest.getZ() + .5);
        entity.setDeltaMovement(0, .02, 0);
        GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
        tracker.startBounce(entity.getY() - .5);
        tracker.confirmRise();
        tracker.remember(entity);
        helper.getLevel().addFreshEntity(entity);
        double beforeY = entity.getY();
        PistonMovingBlockEntity piston = piston(helper, dest);
        push(helper, piston);
        helper.assertTrue(entity.getY() == beforeY && entity.getDeltaMovement().y == 1
                        && tracker.consecutiveBounces() == 1 && !tracker.pendingRise(),
                "An actual native velocity boost without swept-face displacement cannot start another flight");
        AtomicInteger apices = new AtomicInteger();
        entity.tickCount++;
        GravityBouncePhysics.beforeTick(entity, (item, count) -> apices.incrementAndGet());
        helper.assertTrue(apices.get() == 0 && tracker.consecutiveBounces() == 1
                        && GravityMechanismData.interruptions(entity.getItem()) == 0,
                "A boost of an almost-apex rising item is not a false apex or interruption");
        entity.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void pistonMovementDeduplicatesAcrossEntityAndBlockEntitySave(GameTestHelper helper) {
        BlockPos surface = GravityInterruptionGameTests.surface(helper);
        ItemEntity entity = GravityInterruptionGameTests.falling(helper, surface,
                GravityMechanismAssembly.createIncomplete(helper.getLevel()), .1);
        helper.getLevel().addFreshEntity(entity);
        PistonMovingBlockEntity piston = piston(helper, surface.above());
        push(helper, piston);
        double origin = entity.getData(ModAttachments.GRAVITY_BOUNCE).bounceOriginY();
        CompoundTag savedEntity = entity.saveWithoutId(new CompoundTag());
        CompoundTag savedPiston = piston.saveWithoutMetadata(helper.getLevel().registryAccess());
        entity.discard();
        ItemEntity restored = new ItemEntity(EntityType.ITEM, helper.getLevel());
        restored.load(savedEntity);
        restored.setUUID(UUID.randomUUID());
        helper.getLevel().addFreshEntity(restored);
        PistonMovingBlockEntity loadedPiston = new PistonMovingBlockEntity(piston.getBlockPos(), piston.getBlockState());
        loadedPiston.loadWithComponents(savedPiston, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(loadedPiston);
        push(helper, loadedPiston);
        push(helper, loadedPiston);
        helper.assertTrue(restored.getData(ModAttachments.GRAVITY_BOUNCE).bounceOriginY() == origin,
                "Replayed/remaining ticks of a saved piston movement cannot create another takeoff origin");
        GravityInterruptionGameTests.tick(restored, (item, count) -> {});
        helper.assertTrue(restored.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1
                        && GravityMechanismData.interruptions(restored.getItem()) == 0,
                "The saved movement produces one effective launch and no save/load interruption");
        restored.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void pistonAfterNaturalStopStartsANewChain(GameTestHelper helper) {
        BlockPos surface = GravityInterruptionGameTests.surface(helper);
        ItemEntity entity = GravityInterruptionGameTests.falling(helper, surface,
                GravityMechanismAssembly.createIncomplete(helper.getLevel()), 8);
        for (int i = 0; i < 400; i++) GravityInterruptionGameTests.tick(entity, (item, count) -> {});
        helper.assertTrue(GravityMechanismData.interruptions(entity.getItem()) == 1
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                "The previous three-bounce chain has naturally ended");
        helper.getLevel().addFreshEntity(entity);
        push(helper, piston(helper, surface.above()));
        GravityInterruptionGameTests.tick(entity, (item, count) -> {});
        helper.assertTrue(entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1
                        && GravityMechanismData.interruptions(entity.getItem()) == 1
                        && GravityActivationLogic.damageChance(1) == .10,
                "Piston relaunch begins at one, retaining the interruption instead of resurrecting the old chain");
        entity.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 900)
    public static void pistonAssistanceSustainsTenRealConsecutiveBouncesAtFivePercentDamage(GameTestHelper helper) {
        BlockPos surface = GravityInterruptionGameTests.surface(helper);
        ItemEntity entity = GravityInterruptionGameTests.falling(helper, surface,
                GravityMechanismAssembly.createIncomplete(helper.getLevel()), 8);
        helper.getLevel().addFreshEntity(entity);
        AtomicInteger launches = new AtomicInteger();
        helper.onEachTick(() -> {
            // No production bypass: genuine server events and probability calls run, using survival rolls.
            entity.getRandom().setSeed(5244L);
            helper.assertTrue(!entity.isRemoved() && GravityMechanismAssembly.isAwaitingActivation(helper.getLevel(), entity.getItem()),
                    "Fixed rolls preserve the live intermediate through ten apices");
            GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
            helper.assertTrue(GravityMechanismData.interruptions(entity.getItem()) == 0
                            && GravityActivationLogic.damageChance(GravityMechanismData.interruptions(entity.getItem())) == .05,
                    "Actual piston-assisted motion must retain the original 5% damage chance throughout the chain");
            if (tracker.consecutiveBounces() >= 10 && tracker.phase() == GravityBounceTracker.Phase.AFTER_APEX) {
                CreateSandustry.LOGGER.info("GRAVITY_PISTON_CHAIN,consecutive={},piston_launches={},interruptions={},damage={}",
                        tracker.consecutiveBounces(), launches.get(), GravityMechanismData.interruptions(entity.getItem()),
                        GravityActivationLogic.damageChance(GravityMechanismData.interruptions(entity.getItem())));
                entity.discard();
                helper.succeed();
                return;
            }
            if (canCatch(entity)) {
                push(helper, piston(helper, BlockPos.containing(entity.position())));
                helper.assertTrue(entity.getDeltaMovement().y == 1, "Every assistance uses the real native slime impulse");
                launches.incrementAndGet();
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void consecutiveBouncesCanCrossDifferentSlimePositions(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 2));
        helper.getLevel().setBlockAndUpdate(base, Blocks.SLIME_BLOCK.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(base.east(), Blocks.SLIME_BLOCK.defaultBlockState());
        for (int y = 1; y <= 16; y++) {
            helper.getLevel().setBlockAndUpdate(base.above(y), Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(base.east().above(y), Blocks.AIR.defaultBlockState());
        }
        ItemEntity entity = GravityInterruptionGameTests.falling(helper, base,
                GravityMechanismAssembly.createIncomplete(helper.getLevel()), 8);
        entity.setPos(base.getX() + .2, entity.getY(), entity.getZ());
        entity.setDeltaMovement(.04, 0, 0);
        List<BlockPos> contacts = new ArrayList<>();
        AtomicInteger apices = new AtomicInteger();
        for (int i = 0; i < 100 && apices.get() < 2; i++) {
            GravityInterruptionGameTests.tick(entity, (item, count) -> apices.incrementAndGet());
            if (entity.getData(ModAttachments.GRAVITY_BOUNCE).pendingRise() && entity.verticalCollisionBelow)
                contacts.add(entity.getOnPos());
        }
        helper.assertTrue(contacts.size() >= 2 && !contacts.get(0).equals(contacts.get(1))
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 2
                        && GravityMechanismData.interruptions(entity.getItem()) == 0,
                "An uninterrupted trajectory between different slime blocks is one continuous chain");
        helper.succeed();
    }

    private static boolean canCatch(ItemEntity entity) {
        double fraction = entity.getY() - Math.floor(entity.getY());
        return entity.getData(ModAttachments.GRAVITY_BOUNCE).phase() == GravityBounceTracker.Phase.AFTER_APEX
                && entity.getDeltaMovement().y < 0 && fraction > .05 && fraction < .45;
    }

    private static PistonMovingBlockEntity piston(GameTestHelper helper, BlockPos destination) {
        var state = Blocks.MOVING_PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
        helper.getLevel().setBlockAndUpdate(destination, state);
        PistonMovingBlockEntity piston = new PistonMovingBlockEntity(destination, state,
                Blocks.SLIME_BLOCK.defaultBlockState(), Direction.UP, true, false);
        helper.getLevel().setBlockEntity(piston);
        return piston;
    }

    private static void push(GameTestHelper helper, PistonMovingBlockEntity piston) {
        PistonMovingBlockEntity.tick(helper.getLevel(), piston.getBlockPos(), piston.getBlockState(), piston);
    }
}
