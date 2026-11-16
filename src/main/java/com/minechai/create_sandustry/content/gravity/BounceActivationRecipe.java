package com.minechai.create_sandustry.content.gravity;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.minechai.create_sandustry.compat.jei.GravityBounceAssemblySubCategory;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** A real sequenced-assembly step, requested only by the server's apex activation logic. */
public final class BounceActivationRecipe extends StandardProcessingRecipe<SingleRecipeInput>
        implements IAssemblyRecipe {
    public BounceActivationRecipe(ProcessingRecipeParams params) {
        super(ModRecipeTypes.BOUNCE_ACTIVATION_INFO, params);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        // Deliberately unavailable to generic crafting/processing recipe searches.
        return false;
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        if (ingredients.size() != 1 || results.size() != 1) {
            errors.add("Bounce activation requires exactly one item input and one output.");
        }
        return errors;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        return Component.translatable("recipe.createsandustry.assembly.bounce_activation");
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(Blocks.SLIME_BLOCK);
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
        // Slime is a reusable surface, not a consumed ingredient.
    }

    @Override
    public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
        return () -> GravityBounceAssemblySubCategory::new;
    }
}
