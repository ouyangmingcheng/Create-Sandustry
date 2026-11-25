package com.minechai.create_sandustry.content.sandsword;

import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipeParams;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * 机械手（Deployer）使用的沙金剑装配配方。
 *
 * <p>配方类型仍是 Create 的 {@code create:deploying}，因此会出现在机械手 JEI 分类中。
 * 只有受损的完整/破损沙金剑才会匹配，避免耐久全满时白白消耗沙团。</p>
 */
public class SandSwordDeployingRecipe extends DeployerApplicationRecipe {
    public SandSwordDeployingRecipe(ItemApplicationRecipeParams params) {
        super(params);
    }

    @Override
    public boolean matches(RecipeWrapper inventory, Level level) {
        ItemStack processed = inventory.getItem(0);
        return super.matches(inventory, level) && isSandSword(processed) && processed.getDamageValue() > 0;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SAND_SWORD_DEPLOYING_SERIALIZER.get();
    }

    private static boolean isSandSword(ItemStack stack) {
        return stack.is(ModItems.SAND_SWORD.get()) || stack.is(ModItems.SAND_SWORD_BROKEN.get());
    }
}
