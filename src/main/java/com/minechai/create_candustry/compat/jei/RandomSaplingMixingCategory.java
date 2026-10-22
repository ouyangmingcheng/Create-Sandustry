package com.minechai.create_candustry.compat.jei;

import com.minechai.create_candustry.recipe.RandomSaplingMixingRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedMixer;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * JEI 展示：将随机树苗配方的全部结果压缩为一个输出格，JEI 会在该格内轮换显示所有可能树苗。
 */
public class RandomSaplingMixingCategory extends CreateRecipeCategory<RandomSaplingMixingRecipe> {
    private static final Component RANDOM_OUTPUT =
            Component.translatable("jei.createsandustry.random_sapling_mixing.random_output")
                    .withStyle(ChatFormatting.GOLD);
    private final AnimatedMixer mixer = new AnimatedMixer();

    public RandomSaplingMixingCategory(Info<RandomSaplingMixingRecipe> info) {
        super(info);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, RandomSaplingMixingRecipe recipe, IFocusGroup focuses) {
        List<Pair<Ingredient, MutableInt>> condensedIngredients =
                ItemHelper.condenseIngredients(recipe.getIngredients());
        int size = condensedIngredients.size();
        int xOffset = size < 3 ? (3 - size) * 19 / 2 : 0;
        int index = 0;

        for (Pair<Ingredient, MutableInt> pair : condensedIngredients) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack itemStack : pair.getFirst().getItems()) {
                ItemStack copy = itemStack.copy();
                copy.setCount(pair.getSecond().getValue());
                stacks.add(copy);
            }
            builder.addSlot(RecipeIngredientRole.INPUT, 17 + xOffset + (index % 3) * 19,
                            51 - (index / 3) * 19)
                    .setBackground(getRenderedSlot(), -1, -1)
                    .addItemStacks(stacks);
            index++;
        }

        List<ItemStack> possibleSaplings = recipe.getRollableResults().stream()
                .map(ProcessingOutput::getStack)
                .toList();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 51)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStacks(possibleSaplings)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(RANDOM_OUTPUT));
    }

    @Override
    protected void draw(RandomSaplingMixingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 32);
        AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);
        this.mixer.draw(graphics, this.getBackground().getWidth() / 2 + 3, 34);
    }
}