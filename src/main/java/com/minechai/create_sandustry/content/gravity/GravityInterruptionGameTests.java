package com.minechai.create_sandustry.content.gravity;

import java.util.concurrent.atomic.AtomicInteger;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.depot.SharedDepotBlockMethods;
import com.simibubi.create.content.logistics.chute.AbstractChuteBlock;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class GravityInterruptionGameTests {
    private GravityInterruptionGameTests() {}

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void interruptionProbabilityChangesOnlyBetweenChains(GameTestHelper helper) {
        for (int round = 0; round <= 5; round++) {
            double expected = Math.min(1.0, 0.05 * Math.pow(2, round));
            for (int bounce = 1; bounce <= 10; bounce++) {
                helper.assertTrue(GravityActivationLogic.damageChance(round) == expected,
                        "Every one of ten consecutive apices uses the same damage chance");
                if (round < 5) helper.assertTrue(GravityActivationLogic.evaluate(bounce, round, () -> 0.999999)
                        == GravityActivationLogic.Outcome.CONTINUE, "High survival rolls must preserve the chain");
            }
        }
        AtomicInteger rolls = new AtomicInteger();
        helper.assertTrue(GravityActivationLogic.evaluate(1, 5, () -> {
            rolls.incrementAndGet(); return Math.nextDown(0.05);
        }) == GravityActivationLogic.Outcome.SUCCESS && rolls.get() == 1,
                "At 100% damage the first bounce still has its 5% success opportunity before damage");
        helper.assertTrue(GravityActivationLogic.evaluate(15, Integer.MAX_VALUE, () -> {
            throw new AssertionError("Guaranteed success must not roll damage");
        }) == GravityActivationLogic.Outcome.SUCCESS, "The fifteenth consecutive bounce always succeeds");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void naturalStopsCommitExactlyOneInterruptionPerRound(GameTestHelper helper) {
        BlockPos slime = surface(helper);
        ItemStack carried = GravityMechanismAssembly.createIncomplete(helper.getLevel());
        for (int round = 0; round < 5; round++) {
            int interruptionsBefore = round;
            ItemEntity entity = falling(helper, slime, carried, 8);
            AtomicInteger apices = new AtomicInteger();
            for (int tick = 0; tick < 400; tick++) {
                tick(entity, (item, count) -> {
                    helper.assertTrue(GravityMechanismData.interruptions(item.getItem()) == interruptionsBefore,
                            "Consecutive bounces must not increase damage");
                    apices.incrementAndGet();
                });
            }
            helper.assertTrue(apices.get() == 3 && GravityMechanismData.interruptions(entity.getItem()) == round + 1
                            && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                    "One natural stop, including hundreds of resting ticks, adds exactly one interruption");
            for (int repeat = 0; repeat < 5; repeat++) GravityChainLifecycle.interrupt(entity);
            helper.assertTrue(GravityMechanismData.interruptions(entity.getItem()) == round + 1,
                    "Repeated cleanup events do not add interruptions");
            carried = entity.getItem().copy();
        }
        helper.assertTrue(GravityActivationLogic.damageChance(GravityMechanismData.interruptions(carried)) == 1,
                "Five real terminated chains reach 100% conditional damage");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void playerPickupCommitsBeforeInventoryCopyAndRejectsRollBack(GameTestHelper helper) {
        ItemEntity entity = active(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        entity.setNoPickUpDelay();
        entity.playerTouch(player);
        helper.assertTrue(!entity.isRemoved() && GravityMechanismData.interruptions(entity.getItem()) == 0
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "A full inventory must leave the active chain and interruption component unchanged");
        player.getInventory().clearContent();
        entity.playerTouch(player);
        helper.assertTrue(entity.isRemoved() && GravityMechanismData.interruptions(player.getInventory().getItem(0)) == 1,
                "The actual inventory copy already contains the interruption before entity disposal");
        GravityChainLifecycle.interrupt(entity);
        for (int i = 0; i < 3; i++) {
            ItemStack stack = player.getInventory().removeItemNoUpdate(0);
            ItemEntity rethrown = falling(helper, surface(helper), stack, 2);
            rethrown.setNoPickUpDelay();
            rethrown.playerTouch(player);
            helper.assertTrue(GravityMechanismData.interruptions(player.getInventory().getItem(0)) == 1,
                    "Repeated pickup/rethrow without another effective bounce cannot count again");
        }
        ItemEntity fresh = falling(helper, surface(helper), GravityMechanismAssembly.createIncomplete(helper.getLevel()), 2);
        player.getInventory().clearContent();
        fresh.setNoPickUpDelay();
        fresh.playerTouch(player);
        helper.assertTrue(GravityMechanismData.interruptions(player.getInventory().getItem(0)) == 0,
                "Picking up a never-bounced intermediate has no interruption");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void hopperPickupStoresTheCommittedInterruption(GameTestHelper helper) {
        ItemEntity entity = active(helper);
        SimpleContainer full = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 64));
        helper.assertTrue(!HopperBlockEntity.addItem(full, entity)
                        && GravityMechanismData.interruptions(entity.getItem()) == 0
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "A rejected hopper insertion rolls back its prepared component");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(pos);
        entity.setPos(pos.getX() + .5, pos.getY() + 1.1, pos.getZ() + .5);
        helper.getLevel().addFreshEntity(entity);
        helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), hopper)
                        && entity.isRemoved() && GravityMechanismData.interruptions(hopper.getItem(0)) == 1,
                "Real hopper collection persists one interruption before copying/removing the entity");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void createDepotAndBeltCollectionPreserveOneInterruption(GameTestHelper helper) {
        ItemEntity entity = active(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.DEPOT.getDefaultState());
        entity.setPos(pos.getX() + .5, pos.getY() + .85, pos.getZ() + .5);
        SharedDepotBlockMethods.onLanded(helper.getLevel(), entity);
        DepotBlockEntity depot = (DepotBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity.isRemoved() && GravityMechanismData.interruptions(depot.getHeldItem()) == 1,
                "Depot stores the interruption in its actual held stack");
        ItemStack stored = depot.getHeldItem().copy();
        TransportedItemStack transported = TransportedItemStack.read(new TransportedItemStack(stored)
                .serializeNBT(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(GravityMechanismData.interruptions(transported.stack) == 1,
                "Create transport serialization retains the count");

        ItemEntity beltSource = active(helper);
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BELT.getDefaultState());
        BeltBlockEntity belt = (BeltBlockEntity) helper.getLevel().getBlockEntity(pos);
        belt.beltLength = 1;
        belt.index = 0;
        belt.setController(pos);
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        helper.assertTrue(handler != null, "The real belt segment capability is available");
        beltSource.setPos(pos.getX() + .5, pos.getY() + .85, pos.getZ() + .5);
        beltSource.setDeltaMovement(0, -.1, 0);
        ((BeltBlock) belt.getBlockState().getBlock()).entityInside(belt.getBlockState(), helper.getLevel(), pos, beltSource);
        belt.getInventory().tick();
        helper.assertTrue(beltSource.isRemoved() && GravityMechanismData.interruptions(handler.getStackInSlot(0)) == 1,
                "Real belt capture commits exactly one interruption");
        ItemStack extracted = handler.extractItem(0, 1, false);
        belt.getInventory().tick();
        ItemEntity noNewFlight = falling(helper, pos, extracted, 0);
        noNewFlight.setPos(pos.getX() + .5, pos.getY() + .85, pos.getZ() + .5);
        noNewFlight.setDeltaMovement(0, -.1, 0);
        ((BeltBlock) belt.getBlockState().getBlock()).entityInside(belt.getBlockState(), helper.getLevel(), pos, noNewFlight);
        belt.getInventory().tick();
        helper.assertTrue(GravityMechanismData.interruptions(handler.getStackInSlot(0)) == 1,
                "Extracting and re-inserting a belt stack without a new chain never doubles wear");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void saveUnloadAndNetworkRetainStateWithoutInterrupting(GameTestHelper helper) {
        ItemEntity entity = active(helper);
        CompoundTag saved = entity.saveWithoutId(new CompoundTag());
        entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        ItemEntity restored = new ItemEntity(EntityType.ITEM, helper.getLevel());
        restored.load(saved);
        helper.assertTrue(GravityMechanismData.interruptions(restored.getItem()) == 0
                        && restored.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "Unload and save/load preserve the live chain without adding wear");
        AtomicInteger apices = new AtomicInteger();
        for (int i = 0; i < 20; i++) tick(restored, (item, count) -> apices.incrementAndGet());
        helper.assertTrue(apices.get() == 1 && GravityMechanismData.interruptions(restored.getItem()) == 0,
                "A restored flight resumes exactly one apex with the same damage probability");
        ItemStack stack = restored.getItem().copy();
        stack.set(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get(), 3);
        stack.set(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get(), Integer.MAX_VALUE);
        ItemStack parsed = ItemStack.parse(helper.getLevel().registryAccess(), stack.save(helper.getLevel().registryAccess())).orElseThrow();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, parsed);
            ItemStack decoded = ItemStack.STREAM_CODEC.decode(buffer);
            helper.assertTrue(GravityMechanismData.interruptions(decoded) == 3
                            && GravityActivationLogic.damageChance(GravityMechanismData.interruptions(decoded)) == .4,
                    "Persistent and multiplayer ItemStack codecs share interruption-based probability, not legacy totals");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void chuteFallAndSuctionCommitBeforeCopy(GameTestHelper helper) throws ReflectiveOperationException {
        ItemEntity falling = active(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.CHUTE.getDefaultState());
        ChuteBlockEntity chute = (ChuteBlockEntity) helper.getLevel().getBlockEntity(pos);
        chute.tick();
        falling.setPos(pos.getX() + .5, pos.getY() + 1.05, pos.getZ() + .5);
        ((AbstractChuteBlock) chute.getBlockState().getBlock()).updateEntityAfterFallOn(helper.getLevel(), falling);
        helper.assertTrue(falling.isRemoved() && GravityMechanismData.interruptions(chute.getItem()) == 1,
                "A real chute landing copies the committed interruption into its held item");
        chute.setItem(ItemStack.EMPTY);
        ItemEntity suction = active(helper);
        suction.setPos(pos.getX() + .5, pos.getY() - .2, pos.getZ() + .5);
        helper.getLevel().addFreshEntity(suction);
        var search = ChuteBlockEntity.class.getDeclaredMethod("findEntities", float.class);
        search.setAccessible(true);
        search.invoke(chute, .25f); // Invoke the native suction search; airflow configuration is outside this test.
        helper.assertTrue(suction.isRemoved() && GravityMechanismData.interruptions(chute.getItem()) == 1,
                "Native suction commits before its already-copied stack is stored");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void completedEntityTransferDoesNotCopyAnActiveChain(GameTestHelper helper) {
        ItemEntity entity = active(helper);
        Entity moved = entity.changeDimension(new DimensionTransition(helper.getLevel(), entity.position().add(.1, .1, 0),
                entity.getDeltaMovement(), 0, 0, DimensionTransition.DO_NOTHING));
        helper.assertTrue(moved == entity && GravityMechanismData.interruptions(entity.getItem()) == 1
                        && !entity.hasData(ModAttachments.GRAVITY_BOUNCE),
                "A completed transfer ends one chain and cannot leave a copied active tracker behind");
        tick(entity, (item, count) -> {});
        helper.assertTrue(GravityMechanismData.interruptions(entity.getItem()) == 1
                        && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                "The destination's first tick cannot charge the transfer interruption a second time");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void finishedStacksNeverAcquireAnInterruption(GameTestHelper helper) {
        for (boolean success : new boolean[]{true, false}) {
            ItemEntity entity = active(helper);
            if (!success) {
                ItemStack stack = entity.getItem().copy();
                stack.set(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get(), 5);
                entity.setItem(stack);
                entity.getRandom().setSeed(0);
            }
            GravityActivationLogic.activate(entity, success ? 15 : 1, GravityActivationSource.SLIME_BLOCK);
            GravityChainLifecycle.interrupt(entity);
            GravityChainLifecycle.interrupt(entity);
            helper.assertTrue(!entity.getItem().has(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get())
                            && !GravityMechanismAssembly.isAwaitingActivation(helper.getLevel(), entity.getItem()),
                    "Success/scrap is final and cannot accumulate a late interruption");
        }
        helper.succeed();
    }

    static BlockPos surface(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SLIME_BLOCK.defaultBlockState());
        for (int y = 1; y <= 20; y++) helper.getLevel().setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
        return pos;
    }

    static ItemEntity falling(GameTestHelper helper, BlockPos surface, ItemStack stack, double height) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), surface.getX() + .5, surface.getY() + 1 + height,
                surface.getZ() + .5, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        return entity;
    }

    static ItemEntity active(GameTestHelper helper) {
        ItemEntity entity = falling(helper, surface(helper), GravityMechanismAssembly.createIncomplete(helper.getLevel()), 8);
        for (int i = 0; i < 100 && entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0; i++)
            tick(entity, (item, count) -> {});
        helper.assertTrue(entity.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 1,
                "Fixture reaches a real half-block effective rebound");
        return entity;
    }

    static void tick(ItemEntity entity, java.util.function.BiConsumer<ItemEntity, Integer> apex) {
        entity.tickCount++;
        GravityBouncePhysics.beforeTick(entity, apex);
        entity.tick();
        GravityBouncePhysics.afterTick(entity, GravityBounceSurfaces::getBounceCoefficient, GravityBouncePhysics.MINIMUM_IMPACT_SPEED);
    }
}
