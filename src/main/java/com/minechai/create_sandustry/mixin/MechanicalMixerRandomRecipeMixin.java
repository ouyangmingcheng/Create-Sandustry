package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.goldenflora.RandomSaplingMixingRecipe;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 让机械搅拌器像接受 create:mixing 一样接受本模组的随机树苗搅拌配方。 */
@Mixin(MechanicalMixerBlockEntity.class)
public abstract class MechanicalMixerRandomRecipeMixin {
    @Inject(method = "matchStaticFilters", at = @At("HEAD"), cancellable = true)
    private void createsandustry$acceptRandomSaplingMixing(RecipeHolder<? extends Recipe<?>> recipe,
            CallbackInfoReturnable<Boolean> cir) {
        if (recipe.value() instanceof RandomSaplingMixingRecipe) {
            cir.setReturnValue(true);
        }
    }
}