package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** RecipeManager replacements are restored before another world tick can observe them. */
@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressRecipeCacheGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private KineticPressRecipeCacheGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void repeatedMatchHitsCacheAndRecipeReplacementChangesActualOutput(GameTestHelper h) {
        withRecipe(h, Items.GOLD_NUGGET, 0, press -> {
            long before = press.getProcessor().getRecipeLookupCount();
            for (int i = 0; i < 25; i++) {
                h.assertTrue(press.getProcessor().matchesInput(new ItemStack(Items.CARROT)), "Initial recipe must match");
            }
            h.assertTrue(press.getProcessor().getRecipeLookupCount() - before == 1,
                    "Repeated identical input must perform exactly one real RecipeManager lookup");
            h.getLevel().getRecipeManager().replaceRecipes(List.of(recipe(Items.DIAMOND, 0)));
            h.assertTrue(press.getProcessor().matchesInput(new ItemStack(Items.CARROT)), "Replacement recipe must match");
            h.assertTrue(press.getProcessor().getRecipeLookupCount() - before == 2,
                    "Recipe replacement must invalidate a previously successful cache entry");
            for (int slot = 0; slot < 16; slot++) {
                press.getInputHandler().insertItem(slot, new ItemStack(Items.CARROT), false);
            }
            for (int tick = 0; tick < 3; tick++) press.getProcessor().tick();
            h.assertTrue(outputCount(press, Items.DIAMOND) == 16 && outputCount(press, Items.GOLD_NUGGET) == 0,
                    "New tasks must use replacement recipe products rather than a cached old recipe");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void componentsHaveIndependentCacheEntriesAndExplicitInvalidationWorks(GameTestHelper h) {
        withRecipe(h, Items.GOLD_NUGGET, 200, press -> {
            long before = press.getProcessor().getRecipeLookupCount();
            ItemStack first = new ItemStack(Items.CARROT);
            first.set(DataComponents.CUSTOM_NAME, Component.literal("cache input A"));
            ItemStack second = new ItemStack(Items.CARROT);
            second.set(DataComponents.CUSTOM_NAME, Component.literal("cache input B"));
            h.assertTrue(press.getProcessor().matchesInput(first)
                            && press.getProcessor().matchesInput(first.copyWithCount(64))
                            && press.getProcessor().matchesInput(second)
                            && press.getProcessor().matchesInput(first.copy()),
                    "Both component variants must independently match the ingredient");
            h.assertTrue(press.getProcessor().getRecipeLookupCount() - before == 2,
                    "Different components need distinct entries while count changes reuse a one-item entry");
            press.getProcessor().invalidateRecipeCache();
            h.assertTrue(press.getProcessor().matchesInput(first), "Explicit invalidation must preserve matching behavior");
            h.assertTrue(press.getProcessor().getRecipeLookupCount() - before == 3,
                    "Explicit invalidation must force a fresh RecipeManager lookup");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void removedRecipeRejectsFutureInputAndRetainsReturnableQueuedComponents(GameTestHelper h) {
        withRecipe(h, Items.GOLD_NUGGET, 200, press -> {
            ItemStack input = new ItemStack(Items.CARROT);
            input.set(DataComponents.CUSTOM_NAME, Component.literal("removed recipe input"));
            h.assertTrue(press.getInputHandler().insertItem(0, input.copy(), false).isEmpty(),
                    "Original recipe input must enter the queue");
            h.getLevel().getRecipeManager().replaceRecipes(List.of());
            h.assertTrue(!press.getProcessor().matchesInput(input)
                            && press.getInputHandler().insertItem(1, input.copy(), false).getCount() == 1,
                    "Recipe removal must immediately reject future inputs despite old cached success");
            for (int tick = 0; tick < 20; tick++) press.getProcessor().tick();
            h.assertTrue(press.getProcessor().getInputCount() == 1 && press.getProcessor().getActiveTaskCount() == 0,
                    "An invalidated queued input must retain ownership without starting stale work");
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            h.assertTrue(press.returnQueuedInputs(player) == 1, "Invalidated queued input must remain returnable");
            int returned = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (ItemStack.isSameItemSameComponents(stack, input)) returned += stack.getCount();
            }
            h.assertTrue(returned == 1 && press.getProcessor().getInputCount() == 0,
                    "Returning an invalidated recipe input must preserve components and exact ownership");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void waitingOneHundredTicksPerformsOnlyOneRecipeManagerLookup(GameTestHelper h) {
        withRecipe(h, Items.GOLD_NUGGET, 200, press -> {
            long before = press.getProcessor().getRecipeLookupCount();
            press.getInputHandler().insertItem(0, new ItemStack(Items.CARROT), false);
            for (int tick = 0; tick < 100; tick++) press.getProcessor().tick();
            long queries = press.getProcessor().getRecipeLookupCount() - before;
            h.assertTrue(press.getProcessor().getInputCount() == 1 && press.getProcessor().getActiveTaskCount() == 0
                            && press.getEnergyStored() == 0 && press.getProcessor().isBatchLocked(),
                    "Measurement must preserve an actual energy-starved batch for the entire wait");
            h.assertTrue(queries == 1, "One waiting ingredient should need only one manager query, got " + queries);
            CreateSandustry.LOGGER.info("KINETIC_PRESS_RECIPE_CACHE,waiting_ticks=100,manager_queries={}", queries);
        });
        h.succeed();
    }

    private static void withRecipe(GameTestHelper h, Item output, int energy,
                                    Consumer<KineticPressBlockEntity> scenario) {
        var manager = h.getLevel().getRecipeManager();
        List<RecipeHolder<?>> original = new ArrayList<>(manager.getRecipes());
        try {
            manager.replaceRecipes(List.of(recipe(output, energy)));
            scenario.accept(machine(h));
        } finally {
            manager.replaceRecipes(original);
        }
    }

    private static RecipeHolder<KineticPressRecipe> recipe(Item output, int energy) {
        return new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "recipe_cache_fixture"),
                new KineticPressRecipe(Ingredient.of(Items.CARROT),
                        List.of(new KineticPressOutput(output, 1, 1.0F)), 2, 3, energy));
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

    private static int outputCount(KineticPressBlockEntity press, Item item) {
        int count = 0;
        for (int slot = 0; slot < press.getOutputHandler().getSlots(); slot++) {
            var stack = press.getOutputHandler().getStackInSlot(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
