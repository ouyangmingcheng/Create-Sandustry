package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressGameTests {
    private static final BlockPos BASE = new BlockPos(1, 0, 1);
    private static final BlockPos INPUT = BASE.above(3);
    private static final BlockPos OUTPUT_BASE = new BlockPos(1, 1, 1);
    private static final BlockPos OUTPUT_INPUT = OUTPUT_BASE.above(3);

    private KineticPressGameTests() {
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void energyCapabilityOnlyAcceptsHorizontalFacesAndCapsAtThreeThousand(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper);
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BASE);
        helper.assertTrue(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.UP) == null
                        && level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.DOWN) == null
                        && level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, (Direction) null) == null,
                "Top, bottom and unsided queries must not expose an energy input");
        for (Direction side : Direction.Plane.HORIZONTAL) {
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            helper.assertTrue(energy != null && energy.canReceive() && !energy.canExtract()
                            && energy.getMaxEnergyStored() == 3000,
                    "Every horizontal face must expose the reduced input-only buffer: " + side);
            helper.assertTrue(energy.receiveEnergy(30000, true) == 3000 && energy.getEnergyStored() == 0,
                    "Simulated charging must respect capacity without changing stored FE");
        }
        var north = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.NORTH);
        helper.assertTrue(north.receiveEnergy(30000, false) == 3000
                        && north.receiveEnergy(1, false) == 0 && north.extractEnergy(100, false) == 0
                        && press.getEnergyStored() == 3000,
                "Actual charging must stop at 3000 FE and never allow energy extraction");
        CompoundTag oldSave = press.saveWithoutMetadata(level.registryAccess());
        oldSave.putInt("energy", 30000);
        press.loadWithComponents(oldSave, level.registryAccess());
        helper.assertTrue(press.getEnergyStored() == 3000,
                "Pre-rebalance stored FE must be bounded by the new capacity on load");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 100)
    public static void processesRecipeAndConsumesEnergy(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper);
        helper.assertTrue(press.receiveEnergy(1500, false) == 1500, "FE input failed");
        ItemStack remainder = press.getInputHandler().insertItem(
                0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        helper.assertTrue(remainder.isEmpty(), "Input queue rejected the recipe item");

        helper.runAfterDelay(12, () -> {
            helper.assertTrue(press.getProcessor().isProcessing(), "Processing did not start after 10 idle ticks");
            helper.assertTrue(press.getProcessor().getWorkingHeightSnapshot() == 3,
                    "Processing must snapshot the working height at task start");
            helper.assertTrue(press.getEnergyStored() == 300,
                    "Expected 1200 FE consumed, stored=" + press.getEnergyStored());
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1,
                        "Dormant seeds were not inserted into output cache");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void insufficientEnergyKeepsInput(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper);
        press.receiveEnergy(100, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);

        helper.runAfterDelay(15, () -> {
            helper.assertTrue(press.getInputHandler().countItems() == 1,
                    "Insufficient FE must not consume the input item");
            helper.assertTrue(!press.getProcessor().isProcessing(),
                    "Processing must not start without full FE");
            helper.assertTrue(press.getEnergyStored() == 100, "FE must not be partially consumed");
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void inputBatchStartsAffordableTasksInParallel(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper);
        press.receiveEnergy(3000, false);
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                press.getInputHandler(), new ItemStack(ModItems.BURNT_RESIDUE.get(), 32), false);

        helper.assertTrue(press.getInputHandler().countItems() == 16,
                "Input cache accepted more than 16 items");
        helper.assertTrue(remainder.getCount() == 16,
                "Overflow remainder should be exactly 16, got " + remainder.getCount());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(press.getProcessor().getActiveTaskCount() == 2,
                    "Both FE-affordable items should start concurrently");
            helper.assertTrue(press.getInputHandler().countItems() == 14,
                    "Two items should leave the queue for two active tasks");
            helper.assertTrue(press.getProcessor().getEnergySpent() == 2400,
                    "Both concurrent tasks should pay their FE upfront");
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 80)
    public static void parallelTasksAdvanceIndependently(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper, OUTPUT_BASE, OUTPUT_INPUT);
        var registries = press.getLevel().registryAccess();
        CompoundTag tag = press.saveWithoutMetadata(registries);
        ListTag tasks = new ListTag();
        tasks.add(activeTaskTag(1, 20, registries));
        tasks.add(activeTaskTag(2, 40, registries));
        tag.put("InputItems", new CompoundTag());
        tag.put("OutputItems", new CompoundTag());
        tag.put("ActiveTasks", tasks);
        tag.put("PendingOutputs", new ListTag());
        tag.putBoolean("BatchLocked", true);
        tag.putInt("NextTaskId", 3);
        press.loadWithComponents(tag, registries);

        CompoundTag persisted = press.saveWithoutMetadata(registries);
        helper.assertTrue(persisted.getList("ActiveTasks", Tag.TAG_COMPOUND).size() == 2,
                "All parallel tasks must persist independently");

        CompoundTag clientState = new CompoundTag();
        press.getProcessor().write(clientState, registries, true);
        ListTag animations = clientState.getList("Animations", Tag.TAG_COMPOUND);
        helper.assertTrue(animations.size() == 2,
                "Every active task must expose its own client animation snapshot");
        helper.assertTrue(animations.getCompound(0).getInt("Total") == 20
                        && animations.getCompound(1).getInt("Total") == 40,
                "Animation snapshots must retain each task's processing time");

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(press.getProcessor().getActiveTaskCount() == 2,
                    "Both parallel tasks should be active");
            helper.runAfterDelay(24, () -> {
                helper.assertTrue(press.getProcessor().getActiveTaskCount() == 1,
                        "The 20-tick task must complete without cancelling the 40-tick task");
                helper.assertTrue(press.getProcessor().isBatchLocked(),
                        "The batch must stay open until its last task completes");
                helper.runAfterDelay(24, () -> {
                    helper.assertTrue(press.getProcessor().getActiveTaskCount() == 0,
                            "All parallel tasks should complete");
                    helper.assertTrue(!press.getProcessor().isBatchLocked(),
                            "The batch should end only after every task completes");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void legacySingleTaskSaveMigrates(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper);
        var registries = press.getLevel().registryAccess();
        CompoundTag tag = press.saveWithoutMetadata(registries);
        tag.remove("ActiveTasks");
        tag.putBoolean("Processing", true);
        tag.putInt("ProcessingProgress", 12);
        tag.putInt("ProcessingTotalTime", 40);
        tag.put("CurrentItem", new ItemStack(ModItems.BURNT_RESIDUE.get()).saveOptional(registries));
        tag.putString("CurrentRecipe", "createsandustry:burnt_residue_to_dormant_seeds");
        tag.putInt("RequiredHeightSnapshot", 8);
        tag.putInt("BaseEnergySnapshot", 2000);
        tag.putInt("WorkingHeightSnapshot", 3);
        tag.putInt("EnergyCostSnapshot", 12000);
        tag.put("RolledResults", new ListTag());
        press.loadWithComponents(tag, registries);

        helper.assertTrue(press.getProcessor().getActiveTaskCount() == 1,
                "Legacy active task should migrate into the parallel task list");
        helper.assertTrue(press.getProcessor().getProcessingProgress() == 12,
                "Legacy processing progress must be preserved");
        helper.assertTrue(press.getProcessor().isBatchLocked(),
                "Migrated legacy task must keep its batch locked");
        helper.assertTrue(press.getProcessor().getEnergySpent() == 12000,
                "A pre-rebalance paid task must retain its original energy snapshot without repricing or charging");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void validatesBlockedGap(GameTestHelper helper) {
        helper.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(BASE);
        setWorkingHeight(press, 3);
        helper.setBlock(INPUT, AllBlocks.CHUTE.getDefaultState());
        BlockPos blocker = BASE.above(1);
        helper.setBlock(blocker, Blocks.STONE);

        press.requestStructureCheck();
        helper.assertTrue(!press.isStructureValid(), "Blocked gap should invalidate the structure");
        helper.assertTrue(helper.absolutePos(blocker).equals(press.getBlockingPosition()),
                "Blocking position should point at the stone block");

        helper.setBlock(blocker, Blocks.AIR);
        press.requestStructureCheck();
        helper.assertTrue(press.isStructureValid(), "Structure should become valid after clearing the gap");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 120)
    public static void airBelowDropsSingleOutput(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper, OUTPUT_BASE, OUTPUT_INPUT);
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);

        helper.runAfterDelay(75, () -> {
            helper.assertItemEntityCountIs(ModItems.DORMANT_SEEDS.get(),
                    OUTPUT_BASE.below(), 3.0D, 1);
            helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 0,
                    "Dropped output must be removed from the output cache exactly once");
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 120)
    public static void chestBelowReceivesOutput(GameTestHelper helper) {
        helper.setBlock(OUTPUT_BASE.below(), Blocks.CHEST);
        KineticPressBlockEntity press = setupMachine(helper, OUTPUT_BASE, OUTPUT_INPUT);
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);

        helper.runAfterDelay(75, () -> {
            net.minecraft.world.level.block.entity.ChestBlockEntity chest =
                    helper.getBlockEntity(OUTPUT_BASE.below());
            int seeds = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (stack.is(ModItems.DORMANT_SEEDS.get())) {
                    seeds += stack.getCount();
                }
            }
            helper.assertTrue(seeds == 1, "Chest should receive one dormant seed, got " + seeds);
            helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 0,
                    "Successfully delivered output must leave the cache");
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 120)
    public static void solidBelowRetainsOutput(GameTestHelper helper) {
        helper.setBlock(OUTPUT_BASE.below(), Blocks.STONE);
        KineticPressBlockEntity press = setupMachine(helper, OUTPUT_BASE, OUTPUT_INPUT);
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);

        helper.runAfterDelay(75, () -> {
            helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1,
                    "Unsupported solid below must retain output in the cache");
            helper.assertItemEntityCountIs(ModItems.DORMANT_SEEDS.get(),
                    OUTPUT_BASE.below(), 3.0D, 0);
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 100)
    public static void redstonePausesAndResumesProcessing(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper, OUTPUT_BASE, OUTPUT_INPUT);
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        BlockPos redstone = OUTPUT_BASE.east();
        helper.setBlock(redstone, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(15, () -> {
            helper.assertTrue(!press.getProcessor().isProcessing(),
                    "Redstone must prevent collection from starting processing");
            helper.assertTrue(press.getInputHandler().countItems() == 1,
                    "Paused machine must retain its queued input");
            helper.assertTrue(press.getEnergyStored() == 3000,
                    "Paused machine must not deduct FE");

            helper.setBlock(redstone, Blocks.AIR);
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(press.getProcessor().isProcessing()
                                || press.getInputHandler().countItems() == 0,
                        "Processing should resume after redstone is removed");
                helper.succeed();
            });
        });
    }
    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void fullChestRetainsOutput(GameTestHelper helper) {
        helper.setBlock(OUTPUT_BASE.below(), Blocks.CHEST);
        helper.setBlock(OUTPUT_BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(OUTPUT_BASE);
        press.getOutputHandler().insertInternal(new ItemStack(ModItems.DORMANT_SEEDS.get()));

        net.minecraft.world.level.block.entity.ChestBlockEntity chest =
                helper.getBlockEntity(OUTPUT_BASE.below());
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Blocks.STONE, 64));
        }

        helper.runAfterDelay(15, () -> {
            helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1,
                    "Full chest must leave the output in the machine cache");
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void partialChestInsertKeepsRemainder(GameTestHelper helper) {
        helper.setBlock(OUTPUT_BASE.below(), Blocks.CHEST);
        helper.setBlock(OUTPUT_BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(OUTPUT_BASE);
        press.getOutputHandler().insertInternal(new ItemStack(ModItems.DORMANT_SEEDS.get(), 2));

        net.minecraft.world.level.block.entity.ChestBlockEntity chest =
                helper.getBlockEntity(OUTPUT_BASE.below());
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Blocks.STONE, 64));
        }
        chest.setItem(0, new ItemStack(ModItems.DORMANT_SEEDS.get(), 63));

        helper.runAfterDelay(15, () -> {
            helper.assertTrue(chest.getItem(0).getCount() == 64,
                    "Target chest should accept exactly one seed");
            helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1,
                    "Partial insertion must retain the unaccepted seed");
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void depotReceivesOutput(GameTestHelper helper) {
        helper.setBlock(OUTPUT_BASE.below(), AllBlocks.DEPOT.get());
        helper.setBlock(OUTPUT_BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(OUTPUT_BASE);
        press.getOutputHandler().insertInternal(new ItemStack(ModItems.DORMANT_SEEDS.get()));

        helper.runAfterDelay(15, () -> {
            DepotBlockEntity depot = helper.getBlockEntity(OUTPUT_BASE.below());
            helper.assertTrue(depot.getHeldItem().is(ModItems.DORMANT_SEEDS.get()),
                    "Depot should receive the output through DirectBeltInputBehaviour");
            helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 0,
                    "Delivered depot output must leave the cache");
            helper.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 50)
    public static void redstoneBlocksAutomaticOutput(GameTestHelper helper) {
        helper.setBlock(OUTPUT_BASE.below(), Blocks.CHEST);
        helper.setBlock(OUTPUT_BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(OUTPUT_BASE);
        press.getOutputHandler().insertInternal(new ItemStack(ModItems.DORMANT_SEEDS.get()));
        BlockPos redstone = OUTPUT_BASE.east();
        helper.setBlock(redstone, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(15, () -> {
            net.minecraft.world.level.block.entity.ChestBlockEntity chest =
                    helper.getBlockEntity(OUTPUT_BASE.below());
            helper.assertTrue(chest.isEmpty(), "Powered press must not deliver to the chest");
            helper.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1,
                    "Powered press must retain cached output");

            helper.setBlock(redstone, Blocks.AIR);
            helper.runAfterDelay(15, () -> {
                int seeds = 0;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    ItemStack stack = chest.getItem(slot);
                    if (stack.is(ModItems.DORMANT_SEEDS.get())) {
                        seeds += stack.getCount();
                    }
                }
                helper.assertTrue(seeds == 1, "Output should resume after redstone is removed");
                helper.succeed();
            });
        });
    }
    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 100)
    public static void redstonePausesActiveProcessingProgress(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper, OUTPUT_BASE, OUTPUT_INPUT);
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);

        helper.runAfterDelay(14, () -> {
            helper.assertTrue(press.getProcessor().isProcessing(), "Processing should have started");
            int progressBefore = press.getProcessor().getProcessingProgress();
            int energyBefore = press.getEnergyStored();
            BlockPos redstone = OUTPUT_BASE.east();
            helper.setBlock(redstone, Blocks.REDSTONE_BLOCK);

            helper.runAfterDelay(15, () -> {
                helper.assertTrue(press.getProcessor().isProcessing(),
                        "Redstone must pause, not cancel, the active task");
                helper.assertTrue(press.getProcessor().getProcessingProgress() == progressBefore,
                        "Redstone pause must freeze processing progress");
                helper.assertTrue(press.getEnergyStored() == energyBefore,
                        "Paused active task must not deduct additional FE");

                helper.setBlock(redstone, Blocks.AIR);
                helper.runAfterDelay(10, () -> {
                    helper.assertTrue(press.getProcessor().getProcessingProgress() > progressBefore,
                            "Progress must continue after redstone is removed");
                    helper.succeed();
                });
            });
        });
    }
    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void recipeLoadsAndEnergyFormulaMatches(GameTestHelper helper) {
        var level = helper.getLevel();
        RecipeHolder<KineticPressRecipe> holder = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.KINETIC_PRESSING.get(),
                        new SingleRecipeInput(new ItemStack(ModItems.BURNT_RESIDUE.get())), level)
                .orElse(null);
        helper.assertTrue(holder != null, "createsandustry:pressing test recipe was not loaded");

        KineticPressRecipe recipe = holder.value();
        helper.assertTrue(recipe.processingTime() == 20, "processing_time should be 20");
        helper.assertTrue(recipe.requiredHeight() == 8, "required_height should be 8");
        helper.assertTrue(recipe.baseEnergy() == 200, "base_energy should be 200");
        helper.assertTrue(recipe.calculateEnergyCost(8, 200) == 200, "Height 8 should cost 200 FE");
        helper.assertTrue(recipe.calculateEnergyCost(3, 200) == 1200, "Height 3 should cost 1200 FE");
        helper.assertTrue(recipe.calculateEnergyCost(10, 200) == 200,
                "Heights above the requirement must not add FE cost");
        helper.succeed();
    }

    private static KineticPressBlockEntity setupMachine(GameTestHelper helper) {
        return setupMachine(helper, BASE, INPUT);
    }

    private static KineticPressBlockEntity setupMachine(GameTestHelper helper, BlockPos base, BlockPos input) {
        helper.setBlock(base, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(base);
        setWorkingHeight(press, 3);
        helper.setBlock(input, AllBlocks.CHUTE.getDefaultState());
        press.requestStructureCheck();
        helper.assertTrue(press.isStructureValid(), "Test structure should be valid: "
                + press.getStructureFailure());
        return press;
    }

    private static CompoundTag activeTaskTag(int id, int totalTime,
                                              net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag task = new CompoundTag();
        task.putInt("Id", id);
        task.put("Item", new ItemStack(ModItems.BURNT_RESIDUE.get()).saveOptional(registries));
        task.putInt("Progress", 0);
        task.putInt("Total", totalTime);
        task.putInt("WorkingHeight", 3);
        task.putInt("EnergyCost", 1200);
        task.putInt("RequiredHeight", 8);
        task.putInt("BaseEnergy", 200);
        task.put("RolledResults", new ListTag());
        task.put("MaximumOutputs", new ListTag());
        return task;
    }

    private static void setWorkingHeight(KineticPressBlockEntity press, int height) {
        CompoundTag tag = press.saveWithoutMetadata(press.getLevel().registryAccess());
        tag.putInt("workingHeight", height);
        press.loadWithComponents(tag, press.getLevel().registryAccess());
    }

    private static int outputCount(KineticPressBlockEntity press, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < press.getOutputHandler().getSlots(); slot++) {
            ItemStack stack = press.getOutputHandler().getStackInSlot(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
