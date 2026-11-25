package com.minechai.create_sandustry.content.sandsword;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** 为 JEI 可展示的无序修复配方提供数据包与网络序列化。 */
public final class SandSwordRepairRecipeSerializer implements RecipeSerializer<SandSwordRepairRecipe> {
    private static final int MAX_INGREDIENTS = 9;

    private static final MapCodec<SandSwordRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(SandSwordRepairRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category")
                    .orElse(CraftingBookCategory.MISC)
                    .forGetter(SandSwordRepairRecipe::category),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResultItem(null)),
            Ingredient.CODEC_NONEMPTY.listOf()
                    .fieldOf("ingredients")
                    .flatXmap(SandSwordRepairRecipeSerializer::validateIngredients, DataResult::success)
                    .forGetter(SandSwordRepairRecipe::getIngredients)
    ).apply(instance, SandSwordRepairRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SandSwordRepairRecipe> STREAM_CODEC =
            StreamCodec.of(SandSwordRepairRecipeSerializer::toNetwork,
                    SandSwordRepairRecipeSerializer::fromNetwork);

    private static DataResult<NonNullList<Ingredient>> validateIngredients(
            java.util.List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) {
            return DataResult.error(() -> "No ingredients for sand sword repair recipe");
        }
        if (ingredients.size() > MAX_INGREDIENTS) {
            return DataResult.error(() -> "Too many ingredients for sand sword repair recipe");
        }
        Ingredient[] array = ingredients.toArray(Ingredient[]::new);
        return DataResult.success(NonNullList.of(Ingredient.EMPTY, array));
    }

    private static SandSwordRepairRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        int ingredientCount = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
        ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        return new SandSwordRepairRecipe(group, category, result, ingredients);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, SandSwordRepairRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        buffer.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
        }
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
    }

    @Override
    public MapCodec<SandSwordRepairRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SandSwordRepairRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
