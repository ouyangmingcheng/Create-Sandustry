package com.minechai.create_candustry.recipe;

import com.minechai.create_candustry.CreateSandustry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CreateSandustry.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateSandustry.MODID);

    public static final ResourceLocation CAULDRON_CONVERSION_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "cauldron_conversion");

    public static final DeferredHolder<RecipeType<?>, RecipeType<CauldronConversionRecipe>> CAULDRON_CONVERSION =
            RECIPE_TYPES.register("cauldron_conversion",
                    () -> RecipeType.<CauldronConversionRecipe>simple(CAULDRON_CONVERSION_ID));

    public static final DeferredHolder<RecipeSerializer<?>, CauldronConversionRecipeSerializer> CAULDRON_CONVERSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("cauldron_conversion", CauldronConversionRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    private ModRecipeTypes() {
    }
}