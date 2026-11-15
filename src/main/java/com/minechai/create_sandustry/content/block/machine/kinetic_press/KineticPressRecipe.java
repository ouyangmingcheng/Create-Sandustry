package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.ArrayList;
import java.util.List;

import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModRecipeTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Server-authoritative recipe for the Gravity Kinetic Press. */
public final class KineticPressRecipe implements Recipe<SingleRecipeInput> {
    public static final int MAX_RESULTS = 8;

    private final Ingredient ingredient;
    private final List<KineticPressOutput> results;
    private final int processingTime;
    private final int requiredHeight;
    private final int baseEnergy;

    public KineticPressRecipe(Ingredient ingredient, List<KineticPressOutput> results,
                              int processingTime, int requiredHeight, int baseEnergy) {
        if (ingredient == null || ingredient.isEmpty()) {
            throw new IllegalArgumentException("Kinetic Press ingredient must not be empty");
        }
        if (results == null || results.isEmpty() || results.size() > MAX_RESULTS) {
            throw new IllegalArgumentException("Kinetic Press results must contain 1 to " + MAX_RESULTS + " entries");
        }
        if (processingTime < 1) {
            throw new IllegalArgumentException("Kinetic Press processing_time must be at least 1 tick");
        }
        if (requiredHeight < 1 || requiredHeight > 256) {
            throw new IllegalArgumentException("Kinetic Press required_height must be between 1 and 256");
        }
        if (baseEnergy < 0 || baseEnergy > KineticPressBlockEntity.MAX_ENERGY) {
            throw new IllegalArgumentException("Kinetic Press base_energy must be between 0 and "
                    + KineticPressBlockEntity.MAX_ENERGY + " FE");
        }
        ItemStackHandler capacity = new ItemStackHandler(KineticPressOutputHandler.SLOT_COUNT);
        for (KineticPressOutput result : results) {
            if (result == null) throw new IllegalArgumentException("Kinetic Press result must not be null");
            ItemStack maximum = result.getMaximumStack();
            if (!maximum.isEmpty() && !ItemHandlerHelper.insertItemStacked(capacity, maximum, false).isEmpty()) {
                throw new IllegalArgumentException("Kinetic Press maximum outputs do not fit in the empty 9-slot output cache");
            }
        }
        this.ingredient = ingredient;
        this.results = List.copyOf(results);
        this.processingTime = processingTime;
        this.requiredHeight = requiredHeight;
        this.baseEnergy = baseEnergy;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public List<KineticPressOutput> results() {
        return results;
    }

    public int processingTime() {
        return processingTime;
    }

    public int requiredHeight() {
        return requiredHeight;
    }

    public int baseEnergy() {
        return baseEnergy;
    }

    public int calculateEnergyCost(int actualWorkingHeight, int extraEnergyPerMissingHeight) {
        long missing = Math.max(0, requiredHeight - actualWorkingHeight);
        return (int) Math.min(Integer.MAX_VALUE,
                (long) baseEnergy + missing * Math.max(0, extraEnergyPerMissingHeight));
    }

    public List<ItemStack> getMaximumOutputStacks() {
        List<ItemStack> maximum = new ArrayList<>();
        for (KineticPressOutput output : results) {
            ItemStack stack = output.getMaximumStack();
            if (!stack.isEmpty()) {
                maximum.add(stack);
            }
        }
        return maximum;
    }

    /** Must only be invoked by the server-side processor after FE and capacity checks. */
    public List<ItemStack> rollResults(RandomSource random) {
        List<ItemStack> rolled = new ArrayList<>();
        for (KineticPressOutput output : results) {
            ItemStack stack = output.roll(random);
            if (!stack.isEmpty()) {
                rolled.add(stack);
            }
        }
        return rolled;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String getGroup() {
        return "kinetic_pressing";
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.KINETIC_PRESS.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.KINETIC_PRESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.KINETIC_PRESSING.get();
    }
}
