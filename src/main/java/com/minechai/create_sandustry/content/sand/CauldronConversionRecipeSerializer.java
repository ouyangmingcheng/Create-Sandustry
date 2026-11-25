package com.minechai.create_sandustry.content.sand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CauldronConversionRecipeSerializer implements RecipeSerializer<CauldronConversionRecipe> {
    public static final MapCodec<CauldronConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CauldronConversionRecipe::ingredient),
                    Codec.STRING.optionalFieldOf("fluid", "minecraft:water").forGetter(CauldronConversionRecipe::fluid),
                    Codec.INT.fieldOf("water_mb").forGetter(CauldronConversionRecipe::waterMb),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CauldronConversionRecipe::result),
                    Codec.STRING.optionalFieldOf("effect", "").forGetter(CauldronConversionRecipe::effectId)
            ).apply(instance, CauldronConversionRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CauldronConversionRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CauldronConversionRecipe::ingredient,
            ByteBufCodecs.STRING_UTF8, CauldronConversionRecipe::fluid,
            ByteBufCodecs.INT, CauldronConversionRecipe::waterMb,
            ItemStack.STREAM_CODEC, CauldronConversionRecipe::result,
            ByteBufCodecs.STRING_UTF8, CauldronConversionRecipe::effectId,
            CauldronConversionRecipe::new
    );

    @Override
    public MapCodec<CauldronConversionRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CauldronConversionRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}