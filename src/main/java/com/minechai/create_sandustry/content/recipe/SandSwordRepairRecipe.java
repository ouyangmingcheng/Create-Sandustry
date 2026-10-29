package com.minechai.create_sandustry.content.recipe;

import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.content.item.SandSwordForms;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * 沙金剑 + 沙团的无序修复配方。
 *
 * <p>配方继承 {@link ShapelessRecipe} 并暴露真实材料与展示结果，因此 JEI 与配方查看器
 * 能将其作为普通无序合成展示。实际合成时仍需完整或破损的沙金剑受损，每次消耗一个沙团并恢复
 * 2 点耐久；破损剑的耐久回满时，输出完整沙金剑。</p>
 */
public class SandSwordRepairRecipe extends ShapelessRecipe {
    public SandSwordRepairRecipe(String group, CraftingBookCategory category, ItemStack result,
                                 NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && !findRepairSword(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack sword = findRepairSword(input);
        return sword.isEmpty() ? ItemStack.EMPTY : SandSwordForms.repairWithSandBall(sword);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SAND_SWORD_REPAIR_SERIALIZER.get();
    }

    /** 网格中必须恰好有一把受损的完整/破损沙金剑和一个沙团物品槽。 */
    private static ItemStack findRepairSword(CraftingInput input) {
        ItemStack sword = ItemStack.EMPTY;
        boolean hasSandBall = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (sword.isEmpty() && isSandSword(stack) && stack.getCount() == 1) {
                sword = stack;
            } else if (!hasSandBall && stack.is(ModItems.SAND_BALL.get())) {
                hasSandBall = true;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return hasSandBall && !sword.isEmpty() && sword.getDamageValue() > 0 ? sword : ItemStack.EMPTY;
    }

    private static boolean isSandSword(ItemStack stack) {
        return stack.is(ModItems.SAND_SWORD.get()) || stack.is(ModItems.SAND_SWORD_BROKEN.get());
    }
}
