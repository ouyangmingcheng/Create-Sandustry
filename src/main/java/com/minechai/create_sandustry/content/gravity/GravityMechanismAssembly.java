package com.minechai.create_sandustry.content.gravity;

import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Validation and advancement of the actual Create assembly recipe and data component. */
public final class GravityMechanismAssembly {
    public static final ResourceLocation RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sequenced_assembly/gravity_mechanism");

    private GravityMechanismAssembly() {
    }

    public static boolean isAwaitingActivation(Level level, ItemStack stack) {
        if (!stack.is(ModItems.INCOMPLETE_GRAVITY_MECHANISM.get()) || stack.getCount() != 1) {
            return false;
        }
        SequencedAssembly state = stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        return state != null && RECIPE_ID.equals(state.id()) && state.step() == 1
                && Float.compare(state.progress(), 0.5F) == 0 && recipe(level) != null;
    }

    /** Produces the intermediate using Create's own first-step result, also useful to GameTests. */
    public static ItemStack createIncomplete(Level level) {
        if (recipe(level) == null) {
            return ItemStack.EMPTY;
        }
        return SequencedAssemblyRecipe.getRecipe(level, ModItems.MOONHOP_BOARD.get().getDefaultInstance(),
                        AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class)
                .filter(holder -> holder.id().equals(RECIPE_ID))
                .map(holder -> singleResult(holder.value().rollResults(level.random)))
                .filter(stack -> isAwaitingActivation(level, stack))
                .orElse(ItemStack.EMPTY);
    }

    /** Called only after the server's success roll; Create advances the final step itself. */
    public static ItemStack complete(Level level, ItemStack stack) {
        if (level.isClientSide || !isAwaitingActivation(level, stack)) {
            return ItemStack.EMPTY;
        }
        return SequencedAssemblyRecipe.getRecipe(level, stack,
                        ModRecipeTypes.BOUNCE_ACTIVATION.get(), BounceActivationRecipe.class)
                .filter(holder -> holder.id().equals(RECIPE_ID))
                .map(holder -> singleResult(holder.value().rollResults(level.random)))
                .filter(result -> result.is(ModItems.GRAVITY_MECHANISM.get()) && result.getCount() == 1)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack singleResult(List<ItemStack> results) {
        return results.size() == 1 ? results.getFirst() : ItemStack.EMPTY;
    }

    private static SequencedAssemblyRecipe recipe(Level level) {
        var holder = level.getRecipeManager().byKey(RECIPE_ID);
        if (holder.isEmpty() || !(holder.get().value() instanceof SequencedAssemblyRecipe recipe)) {
            return null;
        }
        // Reject stale components and incompatible data-pack overrides rather than guessing a step.
        if (recipe.getLoops() != 1 || recipe.getSequence().size() != 2
                || !recipe.getIngredient().test(ModItems.MOONHOP_BOARD.get().getDefaultInstance())
                || !recipe.getTransitionalItem().is(ModItems.INCOMPLETE_GRAVITY_MECHANISM.get())
                || !(recipe.getSequence().getFirst().getRecipe() instanceof DeployerApplicationRecipe deploy)
                || deploy.getType() != AllRecipeTypes.DEPLOYING.getType()
                || deploy.getIngredients().size() != 2
                || !deploy.getIngredients().get(1).test(AllItems.PRECISION_MECHANISM.asStack())
                || !(recipe.getSequence().get(1).getRecipe() instanceof BounceActivationRecipe)
                || recipe.resultPool.size() != 1) {
            return null;
        }
        ProcessingOutput output = recipe.resultPool.getFirst();
        ItemStack result = output.getStack();
        if (!result.is(ModItems.GRAVITY_MECHANISM.get()) || result.getCount() != 1
                || Float.compare(output.getChance(), 1.0F) != 0
                || result.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
            return null;
        }
        return recipe;
    }
}
