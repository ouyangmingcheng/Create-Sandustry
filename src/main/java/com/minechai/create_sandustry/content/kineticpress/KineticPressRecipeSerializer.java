package com.minechai.create_sandustry.content.kineticpress;

import java.util.List;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class KineticPressRecipeSerializer implements RecipeSerializer<KineticPressRecipe> {
    private record RecipeData(Ingredient ingredient, List<KineticPressOutput> results,
                              int processingTime, int requiredHeight, int baseEnergy) {}

    private static final MapCodec<RecipeData> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(RecipeData::ingredient),
            KineticPressOutput.CODEC.listOf().fieldOf("results").forGetter(RecipeData::results),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("processing_time")
                    .forGetter(RecipeData::processingTime),
            Codec.intRange(1, 256).fieldOf("required_height").forGetter(RecipeData::requiredHeight),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("base_energy").forGetter(RecipeData::baseEnergy)
    ).apply(instance, RecipeData::new));

    public static final MapCodec<KineticPressRecipe> CODEC = DATA_CODEC.flatXmap(data -> {
        try {
            KineticPressRecipe recipe = new KineticPressRecipe(data.ingredient(), data.results(),
                    data.processingTime(), data.requiredHeight(), data.baseEnergy());
            // JSON recipes load on the server. COMMON config is not synchronized,
            // so this policy must not run against a joining client's local config.
            int extra = ModConfigs.SPEC.isLoaded() ? ModConfigs.KINETIC_PRESS_EXTRA_ENERGY_PER_HEIGHT.get()
                    : ModConfigs.KINETIC_PRESS_EXTRA_ENERGY_PER_HEIGHT.getDefault();
            int minimumCost = recipe.calculateEnergyCost(KineticPressStructure.MAX_WORKING_HEIGHT, extra);
            if (minimumCost > KineticPressBlockEntity.MAX_ENERGY) {
                return DataResult.error(() -> "Kinetic Press recipe cannot run at any working height 3..23: "
                        + "minimum cost " + minimumCost + " FE exceeds capacity " + KineticPressBlockEntity.MAX_ENERGY);
            }
            return DataResult.success(recipe);
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }, recipe -> DataResult.success(new RecipeData(recipe.ingredient(), recipe.results(),
            recipe.processingTime(), recipe.requiredHeight(), recipe.baseEnergy())));

    public static final StreamCodec<RegistryFriendlyByteBuf, KineticPressRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, KineticPressRecipe::ingredient,
                    KineticPressOutput.STREAM_CODEC.apply(ByteBufCodecs.list(KineticPressRecipe.MAX_RESULTS)), KineticPressRecipe::results,
                    ByteBufCodecs.VAR_INT, KineticPressRecipe::processingTime,
                    ByteBufCodecs.VAR_INT, KineticPressRecipe::requiredHeight,
                    ByteBufCodecs.VAR_INT, KineticPressRecipe::baseEnergy,
                    KineticPressRecipe::new);

    @Override
    public MapCodec<KineticPressRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, KineticPressRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
