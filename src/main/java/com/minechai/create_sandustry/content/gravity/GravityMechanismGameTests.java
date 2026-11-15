package com.minechai.create_sandustry.content.gravity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class GravityMechanismGameTests {
    private GravityMechanismGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityProbabilityBoundariesAndSuccessPriority(GameTestHelper helper) {
        double[] success = {0.05, 0.05 + .95/14, 0.05 + 2*.95/14, 0.05 + 3*.95/14, 0.05 + 4*.95/14, 0.05 + 5*.95/14};
        double[] damage = {0.05, 0.10, 0.20, 0.40, 0.80, 1.00};
        for (int i = 1; i <= 6; i++) {
            helper.assertTrue(Math.abs(GravityActivationLogic.successChance(i) - success[i - 1]) < 1e-12,
                    "Success chance at bounce " + i);
            helper.assertTrue(Math.abs(GravityActivationLogic.damageChance(i - 1) - damage[i - 1]) < 1e-12,
                    "Damage chance after interruption " + (i - 1));
        }
        helper.assertTrue(GravityActivationLogic.damageChance(Integer.MAX_VALUE) == 1,
                "Interruption damage saturates naturally at 100% without overflow");
        helper.assertTrue(GravityActivationLogic.evaluate(15, 15, () -> {
            throw new AssertionError("Guaranteed fifteenth success must not consume any random roll");
        }) == GravityActivationLogic.Outcome.SUCCESS, "Fifteenth consecutive bounce wins before damage");

        AtomicInteger rolls = new AtomicInteger();
        helper.assertTrue(GravityActivationLogic.evaluate(1, 2,
                        () -> rolls.getAndIncrement() == 0 ? 0.05 : Math.nextDown(0.20))
                        == GravityActivationLogic.Outcome.DAMAGE,
                "After two interruptions, first success is 5% and conditional damage is 20%");
        helper.assertTrue(rolls.get() == 2, "Success must be checked before the independent damage roll");
        helper.assertTrue(GravityActivationLogic.evaluate(1, 2, () -> 0.20)
                        == GravityActivationLogic.Outcome.CONTINUE,
                "Roll equal to a probability boundary must fail that roll");
        helper.assertTrue(GravityActivationLogic.evaluate(1, 1, () -> 0)
                        == GravityActivationLogic.Outcome.SUCCESS,
                "Successful conversion must never evaluate damage");
        helper.assertTrue(GravityActivationLogic.successChance(14) < 1 && GravityActivationLogic.successChance(15) == 1
                && GravityActivationLogic.successChance(Integer.MAX_VALUE) == 1, "Fifteenth success boundary");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityWearSurvivesInventoryStackAndCreateLogisticsSave(GameTestHelper helper) {
        ItemStack original = validIncomplete(helper);
        GravityMechanismData.incrementTotal(original);
        GravityMechanismData.incrementTotal(original);
        SimpleContainer inventory = new SimpleContainer(1);
        helper.assertTrue(inventory.addItem(original.copy()).isEmpty(), "Inventory must accept an intermediate");
        ItemStack stored = inventory.removeItem(0, 1);
        helper.assertTrue(GravityMechanismData.totalBounces(stored) == 2, "Pickup and storage retain wear");
        ItemStack restored = ItemStack.parse(helper.getLevel().registryAccess(),
                stored.saveOptional(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(GravityMechanismData.totalBounces(restored) == 2, "World save retains wear");
        helper.assertTrue(GravityMechanismAssembly.isAwaitingActivation(helper.getLevel(), restored), "World save retains assembly state");
        TransportedItemStack transported = new TransportedItemStack(restored);
        transported = TransportedItemStack.read(transported.serializeNBT(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(GravityMechanismData.totalBounces(transported.stack) == 2, "Create belt NBT retains wear");

        BlockPos depotPos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(depotPos, AllBlocks.DEPOT.getDefaultState());
        DepotBlockEntity depot = (DepotBlockEntity) helper.getLevel().getBlockEntity(depotPos);
        depot.setHeldItem(transported.stack.copy());
        CompoundTag depotSaved = depot.saveWithoutMetadata(helper.getLevel().registryAccess());
        depot.clearContent();
        depot.loadWithComponents(depotSaved, helper.getLevel().registryAccess());
        helper.assertTrue(GravityMechanismData.totalBounces(depot.getHeldItem()) == 2, "Create depot save retains wear");
        helper.assertTrue(GravityMechanismAssembly.isAwaitingActivation(helper.getLevel(), depot.getHeldItem()), "Create depot retains assembly state");
        ItemStack max = original.copy();
        max.set(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get(), Integer.MAX_VALUE);
        helper.assertTrue(GravityMechanismData.incrementTotal(max) == Integer.MAX_VALUE, "Permanent counter saturates safely");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravitySuccessAndDamageReplaceTheExistingEntity(GameTestHelper helper) {
        ItemStack incomplete = validIncomplete(helper);
        incomplete.set(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get(), 6);
        ItemEntity entity = new ItemEntity(helper.getLevel(), 1, 2, 3, incomplete);
        entity.setDeltaMovement(.12, -.02, .03);
        Vec3 position = entity.position();
        Vec3 velocity = entity.getDeltaMovement();
        UUID id = entity.getUUID();
        helper.assertTrue(GravityActivationLogic.activate(entity, 15, GravityActivationSource.SLIME_BLOCK)
                        == GravityActivationLogic.Outcome.SUCCESS,
                "Fifteenth bounce is guaranteed success before damage");
        helper.assertTrue(entity.isAlive() && id.equals(entity.getUUID()) && entity.position().equals(position)
                        && entity.getDeltaMovement().equals(velocity),
                "Conversion preserves the original entity, position and motion");
        assertFinished(helper, entity.getItem(), true);
        helper.assertTrue(GravityActivationLogic.activate(entity, 15, GravityActivationSource.SLIME_BLOCK)
                        == GravityActivationLogic.Outcome.CONTINUE,
                "A duplicate apex must not create a second output");

        ItemStack damaged = validIncomplete(helper);
        damaged.set(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get(), 6);
        damaged.set(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get(), 3);
        ItemEntity damageEntity = new ItemEntity(helper.getLevel(), 1, 2, 3, damaged);
        damageEntity.getRandom().setSeed(0L); // First double fails success, second double is below 40% damage.
        helper.assertTrue(GravityActivationLogic.activate(damageEntity, 1, GravityActivationSource.SLIME_BLOCK)
                        == GravityActivationLogic.Outcome.DAMAGE,
                "Three interruptions apply 40% damage independently of total bounces");
        helper.assertTrue(damageEntity.isAlive(), "Damage replaces the stack in the existing entity");
        assertFinished(helper, damageEntity.getItem(), false);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityNaturalBouncesReachUniqueApicesAndStop(GameTestHelper helper) {
        BlockPos slime = slimeSurface(helper);
        ItemEntity entity = falling(helper, validIncomplete(helper), slime, 8.0);
        int landings = 0;
        int apices = 0;
        for (int tick = 0; tick < 700; tick++) {
            int previousTotal = GravityMechanismData.totalBounces(entity.getItem());
            GravityBounceTracker.Phase previousPhase = entity.hasData(ModAttachments.GRAVITY_BOUNCE)
                    ? entity.getData(ModAttachments.GRAVITY_BOUNCE).phase() : GravityBounceTracker.Phase.FALLING;
            double previousVy = entity.getDeltaMovement().y;
            deterministicTick(helper, entity);
            helper.assertTrue(GravityMechanismAssembly.isAwaitingActivation(helper.getLevel(), entity.getItem()),
                    "Fixed survival rolls keep the intermediate intact throughout natural decay");
            GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
            int total = GravityMechanismData.totalBounces(entity.getItem());
            helper.assertTrue(total == previousTotal || total == previousTotal + 1, "One contact can add at most one bounce");
            if (total > previousTotal) {
                landings++;
                helper.assertTrue(entity.getY() >= slime.getY() + 1.5
                                && tracker.phase() == GravityBounceTracker.Phase.RISING,
                        "A rebound is counted only after a real half-block ascent, without a velocity floor");
                helper.assertTrue(tracker.consecutiveBounces() == total, "An uninterrupted trajectory retains its consecutive count");
            }
            if (previousPhase == GravityBounceTracker.Phase.RISING && tracker.phase() == GravityBounceTracker.Phase.AFTER_APEX) {
                apices++;
                helper.assertTrue(previousVy > 0 && entity.getDeltaMovement().y <= 0,
                        "Activation must follow a velocity sign crossing, never a fixed delay");
                RandomSource expectedRandom = RandomSource.create(5244L);
                expectedRandom.nextDouble();
                expectedRandom.nextDouble();
                helper.assertTrue(entity.getRandom().nextLong() == expectedRandom.nextLong(),
                        "Each surviving apex consumes exactly one success roll and one damage roll");
            }
        }
        helper.assertTrue(landings > 1 && landings < 15 && apices == landings,
                "Natural decay must stop short of fifteen, with one apex per bounce: " + landings + "/" + apices);
        helper.assertTrue(entity.onGround() && Math.abs(entity.getY() - slime.getY() - 1) < 1e-6
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                "Natural stopping ends continuity and retains accumulated wear");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityFlightSaveLoadResumesAndPickupResetsContinuity(GameTestHelper helper) {
        BlockPos slime = slimeSurface(helper);
        ItemEntity entity = falling(helper, validIncomplete(helper), slime, 8.0);
        driveUntilBounce(helper, entity, 1);
        helper.assertTrue(entity.getData(ModAttachments.GRAVITY_BOUNCE).phase() == GravityBounceTracker.Phase.RISING,
                "Save the flight immediately after its first rebound");
        ItemEntity restored = reload(helper, entity);
        helper.assertTrue(restored.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1
                        && restored.getData(ModAttachments.GRAVITY_BOUNCE).phase() == GravityBounceTracker.Phase.RISING,
                "Entity NBT restores the in-flight state and consecutive count");
        for (int tick = 0; tick < 40 && restored.getData(ModAttachments.GRAVITY_BOUNCE).phase() != GravityBounceTracker.Phase.AFTER_APEX; tick++)
            deterministicTick(helper, restored);
        helper.assertTrue(restored.getData(ModAttachments.GRAVITY_BOUNCE).phase() == GravityBounceTracker.Phase.AFTER_APEX
                        && GravityMechanismData.totalBounces(restored.getItem()) == 1,
                "Restored flight reaches one apex without creating a second contact");
        restored = reload(helper, restored);
        deterministicTick(helper, restored);
        helper.assertTrue(restored.getData(ModAttachments.GRAVITY_BOUNCE).phase() == GravityBounceTracker.Phase.AFTER_APEX,
                "Saving after the apex must not replay activation");
        driveUntilBounce(helper, restored, 2);
        ItemEntity rethrown = falling(helper, restored.getItem().copy(), slime, 2.0);
        deterministicTick(helper, rethrown);
        helper.assertTrue(rethrown.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0
                        && GravityMechanismData.totalBounces(rethrown.getItem()) == 2,
                "An inventory round trip creates a fresh trajectory but retains permanent wear");
        driveUntilBounce(helper, rethrown, 3);
        helper.assertTrue(rethrown.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "First bounce after pickup starts a new consecutive sequence");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityIdleSideAndOrdinaryItemsNeverStartProcessing(GameTestHelper helper) {
        BlockPos slime = slimeSurface(helper);
        for (boolean grounded : new boolean[]{false, true}) {
            ItemEntity idle = falling(helper, validIncomplete(helper), slime, 0);
            idle.setDeltaMovement(Vec3.ZERO);
            idle.setOnGround(grounded);
            for (int tick = 0; tick < 100; tick++)
                deterministicTick(helper, idle);
            helper.assertTrue(GravityMechanismData.totalBounces(idle.getItem()) == 0,
                    "Resting on slime must not start processing, initial onGround=" + grounded);
        }

        ItemEntity side = new ItemEntity(helper.getLevel(), slime.getX() - .2, slime.getY() + .5,
                slime.getZ() + .5, validIncomplete(helper));
        side.setDeltaMovement(.3, -.05, 0);
        deterministicTick(helper, side);
        helper.assertTrue(side.horizontalCollision && GravityMechanismData.totalBounces(side.getItem()) == 0,
                "Side contact does not count as a top-face bounce");

        ItemEntity ordinary = falling(helper, new ItemStack(Items.COBBLESTONE), slime, .25);
        ItemEntity invalid = falling(helper, new ItemStack(ModItems.INCOMPLETE_GRAVITY_MECHANISM.get()), slime, .25);
        invalid.setId(ordinary.getId()); // Align vanilla resting-tick throttling for the unregistered controls.
        for (int tick = 0; tick < 40; tick++) {
            deterministicTick(helper, ordinary);
            deterministicTick(helper, invalid);
            helper.assertTrue(ordinary.position().equals(invalid.position())
                            && ordinary.getDeltaMovement().equals(invalid.getDeltaMovement()),
                    "Ordinary and unassembled items keep identical vanilla physics");
            helper.assertTrue(!ordinary.hasData(ModAttachments.GRAVITY_BOUNCE)
                            && !invalid.hasData(ModAttachments.GRAVITY_BOUNCE),
                    "Unqualified items never acquire a processing trajectory");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityExternalMotionFluidsAndStoneInterruptContinuity(GameTestHelper helper) {
        BlockPos slime = slimeSurface(helper);
        ItemEntity entity = falling(helper, validIncomplete(helper), slime, 2.0);
        driveUntilBounce(helper, entity, 1);
        entity.setDeltaMovement(.1, .4, 0);
        deterministicTick(helper, entity);
        helper.assertTrue(entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                "An external relaunch interrupts continuity");
        helper.assertTrue(GravityMechanismData.interruptions(entity.getItem()) == 1,
                "An unrecognized relaunch commits one interruption");
        helper.assertTrue(GravityMechanismData.totalBounces(entity.getItem()) == 1, "Interruption preserves wear");

        // Start a fresh, genuinely active flight so the stone collision itself is
        // responsible for clearing continuity, without changing entity motion or position.
        ItemEntity stoneFlight = falling(helper, validIncomplete(helper), slime, 2.0);
        driveUntilBounce(helper, stoneFlight, 1);
        helper.assertTrue(stoneFlight.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "Stone interruption starts from an active sequence");
        helper.getLevel().setBlockAndUpdate(slime, Blocks.STONE.defaultBlockState());
        for (int tick = 0; tick < 60 && !stoneFlight.onGround(); tick++)
            deterministicTick(helper, stoneFlight);
        helper.assertTrue(stoneFlight.onGround()
                        && GravityMechanismData.totalBounces(stoneFlight.getItem()) == 1
                        && stoneFlight.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                "Stone landings do not activate or increment wear");
        helper.assertTrue(GravityMechanismData.interruptions(stoneFlight.getItem()) == 1,
                "A non-elastic landing ends the active chain once");

        slimeSurface(helper);
        ItemEntity fluidFlight = falling(helper, validIncomplete(helper), slime, 2.0);
        driveUntilBounce(helper, fluidFlight, 1);
        helper.assertTrue(fluidFlight.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "Fluid interruption starts from an independently active sequence");
        helper.getLevel().setBlockAndUpdate(slime.above(), Blocks.WATER.defaultBlockState());
        deterministicTick(helper, fluidFlight);
        helper.assertTrue(fluidFlight.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0
                        && GravityMechanismData.totalBounces(fluidFlight.getItem()) == 1,
                "Fluid entry interrupts the trajectory without changing wear");
        helper.assertTrue(GravityMechanismData.interruptions(fluidFlight.getItem()) == 1,
                "Fluid entry commits exactly one interruption");
        entity.discard();
        GravityActivationLogic.activate(entity, 15, GravityActivationSource.SLIME_BLOCK);
        helper.assertTrue(entity.getItem().is(ModItems.INCOMPLETE_GRAVITY_MECHANISM.get()),
                "Removed entities must not be processed at an apex");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void gravityRealHopperPickupAndChestStorageRetainWear(GameTestHelper helper) {
        BlockPos chestPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos hopperPos = chestPos.above();
        helper.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(hopperPos, Blocks.HOPPER.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(hopperPos.above(), Blocks.AIR.defaultBlockState());
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(hopperPos);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos);
        ItemStack stack = validIncomplete(helper);
        GravityMechanismData.incrementTotal(stack);
        GravityMechanismData.incrementTotal(stack);
        ItemEntity entity = new ItemEntity(helper.getLevel(), hopperPos.getX() + .5,
                hopperPos.getY() + 1.1, hopperPos.getZ() + .5, stack);
        helper.getLevel().addFreshEntity(entity);
        helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), hopper), "The real hopper must collect the world entity");
        helper.assertTrue(entity.isRemoved() && GravityMechanismData.totalBounces(hopper.getItem(0)) == 2,
                "Hopper collection removes the entity while retaining the stack component");
        for (int tick = 0; tick < 10 && chest.isEmpty(); tick++)
            HopperBlockEntity.pushItemsTick(helper.getLevel(), hopperPos, helper.getLevel().getBlockState(hopperPos), hopper);
        helper.assertTrue(!chest.isEmpty() && GravityMechanismData.totalBounces(chest.getItem(0)) == 2,
                "Hopper-to-chest transfer retains permanent wear");
        CompoundTag saved = chest.saveWithoutMetadata(helper.getLevel().registryAccess());
        chest.clearContent();
        chest.loadWithComponents(saved, helper.getLevel().registryAccess());
        ItemStack extracted = chest.removeItem(0, 1);
        helper.assertTrue(GravityMechanismData.totalBounces(extracted) == 2
                        && GravityMechanismAssembly.isAwaitingActivation(helper.getLevel(), extracted),
                "An actual chest save/load preserves wear and native assembly state");
        helper.succeed();
    }

    private static BlockPos slimeSurface(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        for (int y = 1; y <= 8; y++)
            helper.getLevel().setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SLIME_BLOCK.defaultBlockState());
        return pos;
    }

    private static ItemEntity falling(GameTestHelper helper, ItemStack stack, BlockPos surface, double height) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), surface.getX() + .5,
                surface.getY() + 1 + height, surface.getZ() + .5, stack);
        entity.setDeltaMovement(0, -.1, 0);
        return entity;
    }

    private static void deterministicTick(GameTestHelper helper, ItemEntity entity) {
        // Both first doubles exceed .99. This forces survival for the first fourteen apices,
        // while all motion, collisions, events and the guaranteed fifteenth result run normally.
        entity.getRandom().setSeed(5244L);
        helper.getLevel().tickNonPassenger(entity);
    }

    private static void driveUntilBounce(GameTestHelper helper, ItemEntity entity, int total) {
        for (int tick = 0; tick < 100 && GravityMechanismData.totalBounces(entity.getItem()) < total; tick++)
            deterministicTick(helper, entity);
        helper.assertTrue(GravityMechanismData.totalBounces(entity.getItem()) == total, "Expected real contact count " + total);
    }

    private static ItemEntity reload(GameTestHelper helper, ItemEntity original) {
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        ItemEntity restored = new ItemEntity(EntityType.ITEM, helper.getLevel());
        restored.load(saved);
        return restored;
    }

    private static ItemStack validIncomplete(GameTestHelper helper) {
        ItemStack stack = GravityMechanismAssembly.createIncomplete(helper.getLevel());
        helper.assertTrue(!stack.isEmpty(), "The assembly recipe must be loaded");
        return stack;
    }

    private static void assertFinished(GameTestHelper helper, ItemStack stack, boolean success) {
        helper.assertTrue(stack.getCount() == 1, "Each result contains exactly one item");
        helper.assertTrue(!stack.has(AllDataComponents.SEQUENCED_ASSEMBLY)
                        && !stack.has(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get())
                        && !stack.has(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get()),
                "Finished products and scrap must not retain processing components");
        if (success) {
            helper.assertTrue(stack.is(ModItems.GRAVITY_MECHANISM.get()), "Success must produce a gravity mechanism");
        } else {
            helper.assertTrue(stack.is(AllBlocks.COGWHEEL.get().asItem()) || stack.is(ModItems.MOONHOP_BOARD.get())
                            || stack.is(AllItems.GOLDEN_SHEET.get()),
                    "Damage produces one of exactly three scrap items");
        }
    }
}
