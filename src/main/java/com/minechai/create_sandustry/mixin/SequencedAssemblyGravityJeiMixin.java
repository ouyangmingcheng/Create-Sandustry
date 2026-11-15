package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.gravity.GravityAssemblyJeiChance;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep Create's existing page, chance-slot rendering, hit box and tooltip styling. */
@Pseudo
@Mixin(targets = "com.simibubi.create.compat.jei.category.SequencedAssemblyCategory", remap = false)
public abstract class SequencedAssemblyGravityJeiMixin {
    // Includes the main output's deferred tooltip callback, so it agrees with draw and hover.
    @Redirect(method = {"setRecipe", "draw", "getTooltipStrings", "lambda$setRecipe$0"},
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe;getOutputChance()F"),
            require = 6)
    private float createsandustry$displayGravityTotals(SequencedAssemblyRecipe recipe) {
        return GravityAssemblyJeiChance.outputChance(Minecraft.getInstance().level, recipe);
    }
}
