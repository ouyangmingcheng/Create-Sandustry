package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.content.item.ItemStackGoldAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackGoldAccess {
    @Override
    public boolean hasGold() {
        ItemStack stack = (ItemStack) (Object) this;
        return !stack.isEmpty() && Boolean.TRUE.equals(stack.get(ModDataComponents.GOLDEN_SELECTION.get()));
    }

    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void createsandustry$hasGoldenFoil(CallbackInfoReturnable<Boolean> cir) {
        if (this.hasGold()) {
            cir.setReturnValue(true);
        }
    }
}