package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Temporary recipe replacement and every manual tick are confined to one synchronous callback. */
@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressSchedulingGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private KineticPressSchedulingGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void affordableLaterInputBypassesExpensiveHeadAndHeadRecovers(GameTestHelper h) {
        withRecipes(h, List.of(
                recipe("expensive", Items.CARROT, Items.GOLD_NUGGET, 1, 1, 40, 2000),
                recipe("affordable", Items.POTATO, ModItems.DORMANT_SEEDS.get(), 1, 1, 40, 200)), press -> {
            press.receiveEnergy(200, false);
            press.getInputHandler().insertItem(0, new ItemStack(Items.CARROT), false);
            press.getInputHandler().insertItem(1, new ItemStack(Items.POTATO), false);
            ticks(press, 10);
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 1 && press.getEnergyStored() == 0,
                    "Later affordable input must start despite an unaffordable first slot");
            h.assertTrue(press.getInputHandler().getStackInSlot(0).is(Items.CARROT)
                            && press.getInputHandler().getStackInSlot(1).isEmpty(),
                    "Only the affordable input may leave the queue");
            press.receiveEnergy(2000, false);
            press.getProcessor().tick();
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 2 && press.getProcessor().getInputCount() == 0
                            && press.getEnergyStored() == 0,
                    "Previously skipped input must resume after its required resources arrive");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void laterStackableProductBypassesOutputBlockedHead(GameTestHelper h) {
        withRecipes(h, List.of(
                recipe("blocked", Items.CARROT, ModItems.DORMANT_SEEDS.get(), 1, 1, 40, 200),
                recipe("stackable", Items.POTATO, Items.GOLD_NUGGET, 1, 1, 40, 200)), press -> {
            for (int slot = 0; slot < 9; slot++) {
                press.getOutputHandler().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            press.getOutputHandler().setStackInSlot(8, new ItemStack(Items.GOLD_NUGGET, 63));
            press.receiveEnergy(3000, false);
            press.getInputHandler().insertItem(0, new ItemStack(Items.CARROT), false);
            press.getInputHandler().insertItem(1, new ItemStack(Items.POTATO), false);
            ticks(press, 10);
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 1 && press.getEnergyStored() == 2800
                            && press.getInputHandler().getStackInSlot(0).is(Items.CARROT)
                            && press.getInputHandler().getStackInSlot(1).isEmpty(),
                    "An output-blocked first recipe must not block later stackable output");
            press.getOutputHandler().extractItem(0, 64, false);
            press.getProcessor().tick();
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 2 && press.getEnergyStored() == 2600
                            && press.getProcessor().getInputCount() == 0,
                    "Freeing the first recipe's output capacity must let it join existing work");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void sixteenMixedInputsRunInParallelWithExactEnergyAndResults(GameTestHelper h) {
        withRecipes(h, List.of(
                recipe("short", Items.CARROT, ModItems.DORMANT_SEEDS.get(), 1, 1, 4, 100),
                recipe("long", Items.POTATO, Items.GOLD_NUGGET, 2, 1, 8, 200)), press -> {
            press.receiveEnergy(3000, false);
            for (int slot = 0; slot < 16; slot++) {
                h.assertTrue(press.getInputHandler().insertItem(slot,
                        new ItemStack(slot % 2 == 0 ? Items.CARROT : Items.POTATO), false).isEmpty(),
                        "Mixed batch input was rejected");
            }
            press.getProcessor().tick();
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 16 && press.getProcessor().getInputCount() == 0,
                    "All sixteen affordable mixed tasks must start together");
            h.assertTrue(press.getEnergyStored() == 600, "Eight 100 FE and eight 200 FE tasks must charge 2400 FE");
            ticks(press, 4);
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 8
                            && outputCount(press, ModItems.DORMANT_SEEDS.get()) == 8,
                    "The shorter half must complete independently");
            ticks(press, 4);
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 0 && !press.getProcessor().isBatchLocked()
                            && outputCount(press, ModItems.DORMANT_SEEDS.get()) == 8
                            && outputCount(press, Items.GOLD_NUGGET) == 16 && press.getEnergyStored() == 600,
                    "Mixed batch must settle exact results and release its lock without extra FE");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void parallelRandomResultsAndSchedulingCursorSurviveReloadWithoutReroll(GameTestHelper h) {
        withRecipes(h, List.of(recipe("random", Items.CARROT, Items.GOLD_NUGGET, 4, 0.25F, 40, 100)), press -> {
            press.receiveEnergy(3000, false);
            for (int slot = 0; slot < 16; slot++) {
                press.getInputHandler().insertItem(slot, new ItemStack(Items.CARROT), false);
            }
            ticks(press, 6);
            CompoundTag active = press.saveWithoutMetadata(h.getLevel().registryAccess());
            ListTag tasks = active.getList("ActiveTasks", Tag.TAG_COMPOUND).copy();
            int expected = 0;
            for (int i = 0; i < tasks.size(); i++) {
                ListTag products = tasks.getCompound(i).getList("RolledResults", Tag.TAG_COMPOUND);
                for (int j = 0; j < products.size(); j++) {
                    expected += ItemStack.parseOptional(h.getLevel().registryAccess(), products.getCompound(j)).getCount();
                }
            }
            press.loadWithComponents(active, h.getLevel().registryAccess());
            CompoundTag restored = press.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(tasks.equals(restored.getList("ActiveTasks", Tag.TAG_COMPOUND))
                            && active.getInt("NextInputSlot") == restored.getInt("NextInputSlot"),
                    "Reload must preserve independent progress, random outcomes and scheduler cursor");
            h.assertTrue(press.getEnergyStored() == 1400 && press.getProcessor().getActiveTaskCount() == 16,
                    "Reload must retain sixteen paid tasks");
            ticks(press, 40);
            h.assertTrue(outputCount(press, Items.GOLD_NUGGET) == expected && press.getEnergyStored() == 1400,
                    "Resumed parallel tasks must settle their exact prerecorded outcomes without recharge");
            CompoundTag completed = press.saveWithoutMetadata(h.getLevel().registryAccess());
            press.loadWithComponents(completed, h.getLevel().registryAccess());
            ticks(press, 40);
            h.assertTrue(outputCount(press, Items.GOLD_NUGGET) == expected && press.getProcessor().getActiveTaskCount() == 0,
                    "Completed batch reload must not reproduce products");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void downstreamRejectingFirstProductStillAcceptsLaterProductPartially(GameTestHelper h) {
        var press = machine(h);
        h.setBlock(BASE.below(), Blocks.CHEST);
        ChestBlockEntity chest = h.getBlockEntity(BASE.below());
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        chest.setItem(0, new ItemStack(Items.CARROT, 63));
        press.getOutputHandler().setStackInSlot(0, new ItemStack(Items.GOLD_NUGGET, 1));
        press.getOutputHandler().setStackInSlot(1, new ItemStack(Items.CARROT, 2));
        press.getProcessor().tick();
        h.assertTrue(chest.getItem(0).is(Items.CARROT) && chest.getItem(0).getCount() == 64,
                "Later acceptable product must bypass rejected first output");
        h.assertTrue(outputCount(press, Items.GOLD_NUGGET) == 1 && outputCount(press, Items.CARROT) == 1,
                "Partial downstream insertion must preserve both rejected and excess items");
        CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
        press.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(saved.getInt("NextOutputSlot")
                        == press.saveWithoutMetadata(h.getLevel().registryAccess()).getInt("NextOutputSlot"),
                "Automatic output cursor must survive save/reload");
        ticks(press, 25);
        h.assertTrue(chest.getItem(0).getCount() == 64 && outputCount(press, Items.GOLD_NUGGET) == 1
                        && outputCount(press, Items.CARROT) == 1,
                "Retries against full downstream must neither lose nor duplicate retained output");
        h.succeed();
    }

    private static void withRecipes(GameTestHelper h, List<? extends RecipeHolder<?>> recipes,
                                    Consumer<KineticPressBlockEntity> scenario) {
        var manager = h.getLevel().getRecipeManager();
        List<RecipeHolder<?>> original = new ArrayList<>(manager.getRecipes());
        try {
            manager.replaceRecipes(new ArrayList<RecipeHolder<?>>(recipes));
            scenario.accept(machine(h));
        } finally {
            manager.replaceRecipes(original);
        }
    }

    private static RecipeHolder<KineticPressRecipe> recipe(String name, Item input, Item output,
                                                            int count, float chance, int time, int energy) {
        return new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "scheduling_" + name),
                new KineticPressRecipe(Ingredient.of(input), List.of(new KineticPressOutput(output, count, chance)),
                        time, 3, energy));
    }

    private static KineticPressBlockEntity machine(GameTestHelper h) {
        h.setBlock(BASE.below(), Blocks.STONE);
        h.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = h.getBlockEntity(BASE);
        CompoundTag tag = press.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putInt("workingHeight", 3);
        press.loadWithComponents(tag, h.getLevel().registryAccess());
        h.setBlock(BASE.above(3), AllBlocks.CHUTE.getDefaultState());
        press.requestStructureCheck();
        return press;
    }

    private static void ticks(KineticPressBlockEntity press, int count) {
        for (int i = 0; i < count; i++) press.getProcessor().tick();
    }

    private static int outputCount(KineticPressBlockEntity press, Item item) {
        int count = 0;
        for (int slot = 0; slot < press.getOutputHandler().getSlots(); slot++) {
            ItemStack stack = press.getOutputHandler().getStackInSlot(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
