package com.minechai.create_sandustry.compat.kubejs;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.kineticpress.KineticPressOutput;
import com.minechai.create_sandustry.content.kineticpress.KineticPressRecipeType;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.mojang.serialization.Codec;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.component.SimpleRecipeComponent;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SandustryKubeJSPlugin implements KubeJSPlugin {
    // Use the same codec as Create's processing serializer, including its legacy item format.
    @SuppressWarnings("removal")
    private static final RecipeComponentType<ProcessingOutput> SIFTING_OUTPUT = RecipeComponentType.unit(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sifting_output"),
            type -> new ItemResultComponent<>(type, ProcessingOutput.CODEC, ProcessingOutput::getStack,
                    ProcessingOutput::getChance, ProcessingOutput::new));

    private static final RecipeComponentType<KineticPressOutput> PRESSING_OUTPUT = RecipeComponentType.unit(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "pressing_output"),
            type -> new ItemResultComponent<>(type, KineticPressOutput.CODEC, KineticPressOutput::getStack,
                    KineticPressOutput::chance, (stack, chance) -> {
                        if (!stack.getComponentsPatch().isEmpty()) {
                            throw new IllegalArgumentException("Gravity pressing outputs do not support item components");
                        }
                        return new KineticPressOutput(stack.getItem(), stack.getCount(), chance);
                    }));

    @Override
    public void registerRecipeComponents(RecipeComponentTypeRegistry registry) {
        registry.register(SIFTING_OUTPUT);
        registry.register(PRESSING_OUTPUT);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        registry.register(ModRecipeTypes.SIFTING_ID, new RecipeSchema(
                SIFTING_OUTPUT.instance().asList().outputKey("results"),
                IngredientComponent.INGREDIENT.instance().asList().inputKey("ingredients"),
                NumberComponent.INT.otherKey("processing_time")));
        registry.register(KineticPressRecipeType.ID, new RecipeSchema(
                PRESSING_OUTPUT.instance().asList().outputKey("results"),
                IngredientComponent.INGREDIENT.inputKey("ingredient"),
                NumberComponent.INT.otherKey("processing_time"),
                NumberComponent.INT.otherKey("required_height"),
                NumberComponent.INT.otherKey("base_energy")));
    }

    private static final class ItemResultComponent<T> extends SimpleRecipeComponent<T> {
        private final Function<T, ItemStack> stack;
        private final Function<T, Float> chance;
        private final BiFunction<ItemStack, Float, T> create;

        private ItemResultComponent(RecipeComponentType<T> type, Codec<T> codec, Function<T, ItemStack> stack,
                                    Function<T, Float> chance, BiFunction<ItemStack, Float, T> create) {
            super(type, codec, ItemWrapper.TYPE_INFO.or(TypeInfo.STRING).or(TypeInfo.RAW_MAP));
            this.stack = stack;
            this.chance = chance;
            this.create = create;
        }

        @Override
        public T wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof JsonObject || cx.cx().isMapLike(from)) {
                return codec().parse(cx.ops().json(), JsonUtils.of(cx.cx(), from)).getOrThrow();
            }
            ItemStack item = ItemWrapper.wrap(cx.cx(), from);
            if (item.isEmpty()) {
                throw new IllegalArgumentException("Recipe output must be a non-empty item stack");
            }
            return create.apply(item, 1.0F);
        }

        @Override
        public boolean matches(RecipeMatchContext cx, T value, ReplacementMatchInfo match) {
            return ItemStackComponent.ITEM_STACK.instance().matches(cx, stack.apply(value), match);
        }

        @Override
        public T replace(RecipeScriptContext cx, T original, ReplacementMatchInfo match, Object with) {
            ItemStack current = stack.apply(original);
            ItemStack replacement = ItemStackComponent.ITEM_STACK.instance().replace(cx, current, match, with);
            return replacement == current ? original : create.apply(replacement, chance.apply(original));
        }
    }
}
