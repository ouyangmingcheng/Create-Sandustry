package com.minechai.create_sandustry.compat.jei;

import java.util.List;
import java.util.Locale;

import com.minechai.create_sandustry.content.kineticpress.KineticPressOutput;
import com.minechai.create_sandustry.content.kineticpress.KineticPressRecipe;
import com.minechai.create_sandustry.compat.jei.animation.AnimatedKineticPress;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Kinetic Press JEI page using Create's mechanical saw Cutting layout. */
public final class KineticPressCategory extends CreateRecipeCategory<KineticPressRecipe> {
    private final AnimatedKineticPress press = new AnimatedKineticPress();

    public KineticPressCategory(Info<KineticPressRecipe> info) {
        super(info);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, KineticPressRecipe recipe, IFocusGroup focuses) {
        // Same input slot as Create's SawingCategory.
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 5)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.ingredient());

        List<KineticPressOutput> results = recipe.results();
        int bottomY = Math.max(48, ((results.size() + 1) / 2 - 1) * 19 + 5);
        for (int i = 0; i < results.size(); i++) {
            KineticPressOutput output = results.get(i);
            int xOffset = i % 2 == 0 ? 0 : 19;
            int yOffset = (i / 2) * -19;
            builder.addSlot(RecipeIngredientRole.OUTPUT, 118 + xOffset, bottomY + yOffset)
                    .setBackground(getRenderedSlot(output.chance()), -1, -1)
                    .addItemStack(output.getStack())
                    .addRichTooltipCallback((view, tooltip) -> {
                        if (output.chance() < 1.0F) {
                            int percent = Math.round(output.chance() * 100.0F);
                            tooltip.add(CreateLang.translateDirect(
                                    "recipe.processing.chance", percent)
                                    .withStyle(ChatFormatting.GOLD));
                            if (output.count() > 1) {
                                tooltip.add(Component.translatable(
                                        "jei.createsandustry.kinetic_press.independent_chance", output.count())
                                        .withStyle(ChatFormatting.GRAY));
                            }
                        }
                    });
        }
    }

    @Override
    protected void draw(KineticPressRecipe recipe, IRecipeSlotsView recipeSlotsView,
                        GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 70, 6);
        AllGuiTextures.JEI_SHADOW.render(graphics, 55, 55);
        press.draw(graphics, 72, 42);
    }

    @Override
    protected List<Component> getTooltipStrings(KineticPressRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                                double mouseX, double mouseY) {
        // The projected model is centered at (74, 64), at scale 25. Keep the
        // information hitbox on the model, away from ingredients and blank space.
        if (mouseX < 53 || mouseX >= 96 || mouseY < 34 || mouseY >= 77) return List.of();
        return List.of(
                Component.translatable("jei.createsandustry.kinetic_press.height", recipe.requiredHeight()),
                Component.translatable("jei.createsandustry.kinetic_press.energy", recipe.baseEnergy()),
                Component.translatable("jei.createsandustry.kinetic_press.time", recipe.processingTime(), seconds(recipe)),
                Component.translatable("jei.createsandustry.kinetic_press.height_penalty").withStyle(ChatFormatting.GRAY));
    }

    private static String seconds(KineticPressRecipe recipe) {
        return String.format(Locale.ROOT, "%.2f", recipe.processingTime() / 20.0D);
    }

}
