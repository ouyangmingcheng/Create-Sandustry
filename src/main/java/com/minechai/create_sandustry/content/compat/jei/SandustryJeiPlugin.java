package com.minechai.create_sandustry.content.compat.jei;

import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.registry.ModItems;
import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.content.recipe.RandomSaplingMixingRecipe;
import com.minechai.create_sandustry.content.recipe.SiftingRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public final class SandustryJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "jei_plugin");

    private static final ResourceLocation CATEGORY_UID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "random_sapling_mixing");
    private static final RecipeType<RecipeHolder<ConversionRecipe>> MYSTERY_CONVERSION =
            RecipeType.createRecipeHolderType(Create.asResource("mystery_conversion"));

    private CreateRecipeCategory<RandomSaplingMixingRecipe> category;
    private CreateRecipeCategory<SiftingRecipe> siftingCategory;

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

        this.siftingCategory = new CreateRecipeCategory.Builder<>(SiftingRecipe.class)
                .addTypedRecipes(ModRecipeTypes.SIFTING_INFO)
                .catalyst(AllBlocks.BELT::get)
                .itemIcon(AllBlocks.BELT.get())
                .emptyBackground(177, 103)
                .build(ModRecipeTypes.SIFTING_ID, SiftingCategory::new);

        registration.addRecipeCategories(this.category, this.siftingCategory);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (this.category != null) {
            this.category.registerRecipes(registration);
        }
        if (this.siftingCategory != null) {
            this.siftingCategory.registerRecipes(registration);
        }
        registration.addRecipes(MYSTERY_CONVERSION, List.of(
                createConversion(
                        "florinol_soil",
                        Ingredient.of(ModItems.FLORINOL_SOIL.get()),
                        ModItems.FLORINOL_BUCKET.get().getDefaultInstance()),
                createConversion(
                        "mooncrystal",
                        Ingredient.of(Items.PRISMARINE_SHARD),
                        ModItems.MOONCRYSTAL.get().getDefaultInstance()),
                createConversion(
                        "moonhop",
                        Ingredient.of(ModItems.MOONCRYSTAL.get()),
                        ModItems.MOONHOP.get().getDefaultInstance())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (this.category != null) {
            this.category.registerCatalysts(registration);
        }
        if (this.siftingCategory != null) {
            this.siftingCategory.registerCatalysts(registration);
        }
    }

    private static RecipeHolder<ConversionRecipe> createConversion(String name, Ingredient input, ItemStack output) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                CreateSandustry.MODID, "conversion_" + name);
        ConversionRecipe recipe = new Builder<>(ConversionRecipe::new, recipeId)
                .withItemIngredients(input)
                .withSingleItemOutput(output)
                .build();
        return new RecipeHolder<>(recipeId, recipe);
    }
}
