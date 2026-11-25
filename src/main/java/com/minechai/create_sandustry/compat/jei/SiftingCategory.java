package com.minechai.create_sandustry.compat.jei;

import java.util.List;

import com.minechai.create_sandustry.compat.jei.animation.AnimatedBeltProcessing;
import com.minechai.create_sandustry.content.sifting.SiftingRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI presentation for {@link SiftingRecipe}. Recipe execution remains entirely
 * in the existing belt processing code.
 */
public final class SiftingCategory extends CreateRecipeCategory<SiftingRecipe> {
    private final AnimatedBeltProcessing belt = new AnimatedBeltProcessing();

    public SiftingCategory(Info<SiftingRecipe> info) {
        super(info);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, SiftingRecipe recipe, IFocusGroup focuses) {
        List<ProcessingOutput> results = recipe.getRollableResults();

        builder.addSlot(RecipeIngredientRole.INPUT, 21, 56)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getIngredients().getFirst());

        // Keep recipe result order: residue is result 0 and stays at the top.
        for (int i = 0; i < results.size(); i++) {
            ProcessingOutput output = results.get(i);
            int yOffset = 37 + i * 19;

            builder.addSlot(RecipeIngredientRole.OUTPUT, 141, yOffset)
                    .setBackground(getRenderedSlot(output), -1, -1)
                    .addItemStack(output.getStack())
                    .addRichTooltipCallback(addStochasticTooltip(output));
        }
    }

    @Override
    protected void draw(SiftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 61, 48);
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 54, 59);

        Ingredient input = recipe.getIngredients().getFirst();
        ItemStack[] inputStacks = input.getItems();
        ItemStack preview = inputStacks.length == 0 ? ItemStack.EMPTY : inputStacks[0];
        belt.draw(graphics, getBackground().getWidth() / 2, 51, preview);
    }
}
