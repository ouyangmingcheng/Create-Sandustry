package com.minechai.create_sandustry.content.kineticpress;

import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Item ownership and lifecycle regression tests. No random outcomes are assumed. */
@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressSafetyGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private KineticPressSafetyGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void emptyRemovalAndRepeatedReleaseDoNotDropItems(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        helper.setBlock(BASE, Blocks.AIR);
        press.releaseContentsForRemoval();
        press.releaseContentsForRemoval();
        helper.assertTrue(drops(helper).isEmpty(), "Empty/repeated removal must not generate items");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void fullInputRemovalReturnsExactlySixteenOriginalItems(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        ItemStack original = new ItemStack(ModItems.BURNT_RESIDUE.get());
        original.set(DataComponents.CUSTOM_NAME, Component.literal("queued component sentinel"));
        for (int slot = 0; slot < 16; slot++) {
            helper.assertTrue(press.getInputHandler().insertItem(slot, original.copy(), false).isEmpty(),
                    "Fixture input was rejected at slot " + slot);
        }
        helper.setBlock(BASE, Blocks.AIR);
        press.releaseContentsForRemoval();
        helper.assertTrue(dropCount(helper, ModItems.BURNT_RESIDUE.get()) == 16,
                "Removal must return sixteen queued inputs exactly once");
        helper.assertTrue(drops(helper).stream().allMatch(entity ->
                        ItemStack.isSameItemSameComponents(original, entity.getItem())),
                "Queued item components must survive removal");
        helper.assertTrue(press.getInputHandler().countItems() == 0, "Released input must no longer be owned");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void fullOutputRemovalDropsAllNineStacksExactlyOnce(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        for (int slot = 0; slot < 9; slot++) {
            press.getOutputHandler().setStackInSlot(slot, new ItemStack(Items.GOLD_NUGGET, 64));
        }
        helper.setBlock(BASE, Blocks.AIR);
        press.releaseContentsForRemoval();
        helper.assertTrue(dropCount(helper, Items.GOLD_NUGGET) == 9 * 64,
                "All nine output stacks must be returned without duplication");
        helper.assertTrue(press.isEmpty(), "Released output cache must be empty");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void parallelTaskRemovalReturnsInputsAndCancelsRolledResults(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        HolderLookup.Provider registries = press.getLevel().registryAccess();
        CompoundTag saved = press.saveWithoutMetadata(registries);
        ListTag tasks = new ListTag();
        for (int id = 1; id <= 3; id++) {
            ItemStack input = new ItemStack(ModItems.BURNT_RESIDUE.get());
            input.set(DataComponents.CUSTOM_NAME, Component.literal("active input " + id));
            tasks.add(task(id, 10 + id, input, registries));
        }
        saved.put("ActiveTasks", tasks);
        saved.putBoolean("BatchLocked", true);
        saved.putInt("energy", 500);
        press.loadWithComponents(saved, registries);
        helper.setBlock(BASE, Blocks.AIR);
        press.releaseContentsForRemoval();
        helper.assertTrue(dropCount(helper, ModItems.BURNT_RESIDUE.get()) == 3,
                "Each active task must return its original input once");
        helper.assertTrue(dropCount(helper, ModItems.AMETHYST_FLOWER_SEEDS.get()) == 0
                        && dropCount(helper, Items.GOLD_NUGGET) == 0,
                "Cancelled active tasks must not also release their rolled results");
        for (int id = 1; id <= 3; id++) {
            Component name = Component.literal("active input " + id);
            helper.assertTrue(drops(helper).stream().anyMatch(entity ->
                            name.equals(entity.getItem().get(DataComponents.CUSTOM_NAME))),
                    "Active input components missing for task " + id);
        }
        helper.assertTrue(press.getProcessor().getActiveTaskCount() == 0, "Active task ownership must be cleared");
        helper.assertTrue(press.getEnergyStored() == 500, "Removal must not refund already spent FE");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void completedPendingResultsReturnProductsWithoutInputs(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        HolderLookup.Provider registries = press.getLevel().registryAccess();
        CompoundTag saved = press.saveWithoutMetadata(registries);
        ListTag products = new ListTag();
        products.add(new ItemStack(ModItems.AMETHYST_FLOWER_SEEDS.get(), 3).saveOptional(registries));
        products.add(new ItemStack(Items.GOLD_NUGGET, 2).saveOptional(registries));
        saved.put("PendingOutputs", products);
        press.loadWithComponents(saved, registries);
        helper.setBlock(BASE, Blocks.AIR);
        press.releaseContentsForRemoval();
        helper.assertTrue(dropCount(helper, ModItems.AMETHYST_FLOWER_SEEDS.get()) == 3
                        && dropCount(helper, Items.GOLD_NUGGET) == 2,
                "Finished pending products must be returned exactly once");
        helper.assertTrue(dropCount(helper, ModItems.BURNT_RESIDUE.get()) == 0,
                "Completed work must not also return an input");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void visualPropertyChangesDoNotReleaseContents(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        press.getOutputHandler().insertInternal(new ItemStack(Items.GOLD_NUGGET, 7));
        helper.setBlock(BASE, press.getBlockState().setValue(KineticPressBlock.WORKING, true));
        helper.setBlock(BASE, press.getBlockState().setValue(KineticPressBlock.POWERED, true));
        helper.setBlock(BASE, press.getBlockState().setValue(KineticPressBlock.WORKING, false)
                .setValue(KineticPressBlock.POWERED, false));
        helper.assertTrue(helper.getBlockEntity(BASE) == press, "Property changes must retain the same block entity");
        helper.assertTrue(drops(helper).isEmpty(), "Visual property changes must not release any contents");
        helper.assertTrue(outputCount(press, Items.GOLD_NUGGET) == 7, "Property changes lost cached output");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void chunkUnloadCallbackAndSaveReloadRetainOwnership(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        press.getOutputHandler().insertInternal(new ItemStack(Items.GOLD_NUGGET, 7));
        HolderLookup.Provider registries = press.getLevel().registryAccess();
        CompoundTag saved = press.saveWithoutMetadata(registries);
        press.onChunkUnloaded();
        helper.assertTrue(drops(helper).isEmpty(), "Chunk unload lifecycle callback must not release contents");
        press.loadWithComponents(saved, registries);
        helper.assertTrue(press.getInputHandler().countItems() == 1
                        && outputCount(press, Items.GOLD_NUGGET) == 7,
                "Unload/save/reload must retain input and output ownership");
        helper.assertTrue(drops(helper).isEmpty(), "Reload must not create drops");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void restoredTaskPreservesRolledProductsAndSettlesOnlyOnce(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        HolderLookup.Provider registries = press.getLevel().registryAccess();
        CompoundTag saved = press.saveWithoutMetadata(registries);
        ListTag tasks = new ListTag();
        tasks.add(task(1, 39, new ItemStack(ModItems.BURNT_RESIDUE.get()), registries));
        saved.put("ActiveTasks", tasks);
        saved.putBoolean("BatchLocked", true);
        saved.putInt("energy", 500);
        press.loadWithComponents(saved, registries);
        CompoundTag roundTrip = press.saveWithoutMetadata(registries);
        helper.assertTrue(tasks.equals(roundTrip.getList("ActiveTasks", Tag.TAG_COMPOUND)),
                "Task reload must preserve progress, original input and predetermined results");
        press.loadWithComponents(roundTrip, registries);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(outputCount(press, ModItems.AMETHYST_FLOWER_SEEDS.get()) == 1
                            && outputCount(press, Items.GOLD_NUGGET) == 2,
                    "Restored task must settle predetermined results without rerolling");
            helper.assertTrue(press.getEnergyStored() == 500, "Resuming paid work must not charge FE again");
            CompoundTag completed = press.saveWithoutMetadata(registries);
            press.loadWithComponents(completed, registries);
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(outputCount(press, ModItems.AMETHYST_FLOWER_SEEDS.get()) == 1
                                && outputCount(press, Items.GOLD_NUGGET) == 2,
                        "Reloading completed work must not settle it again");
                helper.assertTrue(press.getProcessor().getActiveTaskCount() == 0,
                        "Completed tasks must no longer be active");
                helper.succeed();
            });
        });
    }

    private static KineticPressBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(BASE.below(), Blocks.STONE);
        helper.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(BASE);
        CompoundTag saved = press.saveWithoutMetadata(press.getLevel().registryAccess());
        saved.putInt("workingHeight", 3);
        press.loadWithComponents(saved, press.getLevel().registryAccess());
        helper.setBlock(BASE.above(3), AllBlocks.CHUTE.getDefaultState());
        press.requestStructureCheck();
        helper.assertTrue(press.isStructureValid(), "Safety test fixture must have a valid structure");
        return press;
    }

    private static CompoundTag task(int id, int progress, ItemStack input, HolderLookup.Provider registries) {
        CompoundTag task = new CompoundTag();
        task.putInt("Id", id);
        task.put("Item", input.saveOptional(registries));
        task.putString("Recipe", "createsandustry:burnt_residue_to_amethyst_flower_seeds");
        task.putInt("Progress", progress);
        task.putInt("Total", 40);
        task.putInt("WorkingHeight", 3);
        task.putInt("EnergyCost", 1200);
        task.putInt("RequiredHeight", 8);
        task.putInt("BaseEnergy", 200);
        ListTag products = new ListTag();
        products.add(new ItemStack(ModItems.AMETHYST_FLOWER_SEEDS.get()).saveOptional(registries));
        products.add(new ItemStack(Items.GOLD_NUGGET, 2).saveOptional(registries));
        task.put("RolledResults", products);
        task.put("MaximumOutputs", products.copy());
        return task;
    }

    private static List<ItemEntity> drops(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(BASE)).inflate(1.0D), entity -> entity.isAlive());
    }

    private static int dropCount(GameTestHelper helper, Item item) {
        return drops(helper).stream().mapToInt(entity -> entity.getItem().is(item)
                ? entity.getItem().getCount() : 0).sum();
    }

    private static int outputCount(KineticPressBlockEntity press, Item item) {
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
