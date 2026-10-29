package com.minechai.create_sandustry.content.compat.create;

import java.util.Optional;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.content.item.SandSwordForms;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * 为机械手补充带动态耐久的沙金剑修复结果。
 *
 * <p>Create 的 Deployer 配方结果通常由数据包静态描述，无法表达“当前耐久 +2”。
 * 在配方搜索阶段用输入剑生成一份当前帧的临时机械手配方，机械手执行时即可得到正确结果；
 * 数据包中的同名配方仍用于 JEI 展示与配方发现。</p>
 */
public final class SandSwordDeployerEvents {
    private static final ResourceLocation DYNAMIC_RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sand_sword_deploying_dynamic");
    private static final int DYNAMIC_RECIPE_PRIORITY = 1000;

    private SandSwordDeployerEvents() {
    }

    public static void onRecipeSearch(DeployerRecipeSearchEvent event) {
        if (!event.shouldAddRecipeWithPriority(DYNAMIC_RECIPE_PRIORITY)) {
            return;
        }
        RecipeWrapper inventory = event.getInventory();
        ItemStack processed = inventory.getItem(0);
        ItemStack held = inventory.getItem(1);
        if (!held.is(ModItems.SAND_BALL.get()) || !isDamagedSandSword(processed)) {
            return;
        }

        ItemStack repaired = SandSwordForms.repairWithSandBall(processed);
        DeployerApplicationRecipe recipe = new ItemApplicationRecipe.Builder<>(
                DeployerApplicationRecipe::new, DYNAMIC_RECIPE_ID)
                .require(Ingredient.of(ModItems.SAND_SWORD.get(), ModItems.SAND_SWORD_BROKEN.get()))
                .require(ModItems.SAND_BALL.get())
                .output(repaired)
                .build();
        event.addRecipe(() -> Optional.of(new RecipeHolder<>(DYNAMIC_RECIPE_ID, recipe)), DYNAMIC_RECIPE_PRIORITY);
    }

    private static boolean isDamagedSandSword(ItemStack stack) {
        return (stack.is(ModItems.SAND_SWORD.get()) || stack.is(ModItems.SAND_SWORD_BROKEN.get()))
                && stack.getDamageValue() > 0;
    }
}
