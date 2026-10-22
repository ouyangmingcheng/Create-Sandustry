package com.minechai.create_candustry.recipe;

import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import java.util.Collections;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Create 搅拌配方的一种结果选择模式：从 {@code results} 中均匀随机选择一个物品结果。
 *
 * <p>使用独立配方类型 {@code createsandustry:random_sapling_mixing}，由机械搅拌器兼容层识别；
 * 结果抽取被覆盖，确保一次加工恰好产出一种树苗，而不是多个独立概率结果。</p>
 */
public class RandomSaplingMixingRecipe extends BasinRecipe {
    public RandomSaplingMixingRecipe(ProcessingRecipeParams params) {
        super(ModRecipeTypes.RANDOM_SAPLING_MIXING_INFO, params);
    }

    @Override
    public List<ItemStack> rollResults(RandomSource randomSource) {
        List<ProcessingOutput> outputs = this.getRollableResults();
        if (outputs.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(outputs.get(randomSource.nextInt(outputs.size())).getStack());
    }

    @Override
    protected int getMaxOutputCount() {
        // Enough room for the full vanilla sapling set, with golden oak intentionally excluded in JSON.
        return 16;
    }
}