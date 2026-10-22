package com.minechai.create_candustry.compat.jei;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.recipe.ModRecipeTypes;
import com.minechai.create_candustry.recipe.RandomSaplingMixingRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class SandustryJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "jei_plugin");
    private static final ResourceLocation CATEGORY_UID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "random_sapling_mixing");

    private CreateRecipeCategory<RandomSaplingMixingRecipe> category;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        this.category = new CreateRecipeCategory.Builder<>(RandomSaplingMixingRecipe.class)
                .addTypedRecipes(ModRecipeTypes.RANDOM_SAPLING_MIXING::get)
                .catalyst(AllBlocks.MECHANICAL_MIXER::get)
                .catalyst(AllBlocks.BASIN::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_MIXER.get(), AllBlocks.BASIN.get())
                .emptyBackground(177, 103)
                .build(CATEGORY_UID, RandomSaplingMixingCategory::new);
        registration.addRecipeCategories(this.category);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (this.category != null) {
            this.category.registerRecipes(registration);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (this.category != null) {
            this.category.registerCatalysts(registration);
        }
    }
}