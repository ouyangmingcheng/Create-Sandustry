package com.minechai.create_sandustry.content.recipe;

import com.minechai.create_sandustry.registry.ModRecipeTypes;
import java.util.List;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 传送带筛振配方。
 *
 * <p>约定第一个结果保留在传送带上继续运输，后续结果作为副产物从传送带下方输出。</p>
 */
public final class SiftingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public SiftingRecipe(ProcessingRecipeParams params) {
        super(ModRecipeTypes.SIFTING_INFO, params);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && !ingredients.isEmpty() && ingredients.getFirst().test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 8;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        if (getProcessingDuration() <= 0) {
            errors.add("Sifting recipes require a positive processing_time.");
        }
        if (getRollableResults().isEmpty() || getRollableResults().getFirst().getChance() < 1.0F) {
            errors.add("Sifting recipes require a guaranteed first result to remain on the belt.");
        }
        return errors;
    }
}
