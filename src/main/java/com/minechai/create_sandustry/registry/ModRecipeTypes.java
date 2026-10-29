package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.recipe.SiftingRecipe;
import com.minechai.create_sandustry.content.recipe.SandSwordRepairRecipeSerializer;
import com.minechai.create_sandustry.content.recipe.SandSwordDeployingRecipe;
import com.minechai.create_sandustry.content.recipe.RandomSaplingMixingRecipe;
import com.minechai.create_sandustry.content.recipe.CauldronConversionRecipeSerializer;
import com.minechai.create_sandustry.content.recipe.CauldronConversionRecipe;
import com.minechai.create_sandustry.CreateSandustry;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
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
    public static final ResourceLocation RANDOM_SAPLING_MIXING_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "random_sapling_mixing");
    public static final ResourceLocation SIFTING_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sifting");

    public static final DeferredHolder<RecipeType<?>, RecipeType<CauldronConversionRecipe>> CAULDRON_CONVERSION =
            RECIPE_TYPES.register("cauldron_conversion",
                    () -> RecipeType.<CauldronConversionRecipe>simple(CAULDRON_CONVERSION_ID));

    public static final DeferredHolder<RecipeSerializer<?>, CauldronConversionRecipeSerializer> CAULDRON_CONVERSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("cauldron_conversion", CauldronConversionRecipeSerializer::new);

    /** 沙金剑 + 沙团的无序修复配方。使用原版合成配方类型，仅序列化器由本模组提供。 */
    public static final DeferredHolder<RecipeSerializer<?>, SandSwordRepairRecipeSerializer>
            SAND_SWORD_REPAIR_SERIALIZER = RECIPE_SERIALIZERS.register("sand_sword_repair",
                    SandSwordRepairRecipeSerializer::new);

    /** 机械手沙金剑装配配方。类型仍为 create:deploying。 */
    public static final DeferredHolder<RecipeSerializer<?>, ItemApplicationRecipe.Serializer<SandSwordDeployingRecipe>>
            SAND_SWORD_DEPLOYING_SERIALIZER = RECIPE_SERIALIZERS.register("sand_sword_deploying",
                    () -> new ItemApplicationRecipe.Serializer<>(SandSwordDeployingRecipe::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<RandomSaplingMixingRecipe>> RANDOM_SAPLING_MIXING =
            RECIPE_TYPES.register("random_sapling_mixing",
                    () -> RecipeType.simple(RANDOM_SAPLING_MIXING_ID));

    public static final DeferredHolder<RecipeSerializer<?>, StandardProcessingRecipe.Serializer<RandomSaplingMixingRecipe>>
            RANDOM_SAPLING_MIXING_SERIALIZER = RECIPE_SERIALIZERS.register("random_sapling_mixing",
                    () -> new StandardProcessingRecipe.Serializer<>(RandomSaplingMixingRecipe::new));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SiftingRecipe>> SIFTING =
            RECIPE_TYPES.register("sifting",
                    () -> RecipeType.<SiftingRecipe>simple(SIFTING_ID));

    public static final DeferredHolder<RecipeSerializer<?>, StandardProcessingRecipe.Serializer<SiftingRecipe>>
            SIFTING_SERIALIZER = RECIPE_SERIALIZERS.register("sifting",
                    () -> new StandardProcessingRecipe.Serializer<>(SiftingRecipe::new));

    public static final IRecipeTypeInfo RANDOM_SAPLING_MIXING_INFO = new IRecipeTypeInfo() {
        @Override
        public ResourceLocation getId() {
            return RANDOM_SAPLING_MIXING_ID;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends RecipeSerializer<?>> T getSerializer() {
            return (T) RANDOM_SAPLING_MIXING_SERIALIZER.get();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
            return (RecipeType<R>) RANDOM_SAPLING_MIXING.get();
        }
    };

    public static final IRecipeTypeInfo SIFTING_INFO = new IRecipeTypeInfo() {
        @Override
        public ResourceLocation getId() {
            return SIFTING_ID;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends RecipeSerializer<?>> T getSerializer() {
            return (T) SIFTING_SERIALIZER.get();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
            return (RecipeType<R>) SIFTING.get();
        }
    };

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    private ModRecipeTypes() {
    }
}