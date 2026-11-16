package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.kineticpress.KineticPressBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bridges only chutes explicitly bound to a Kinetic Press. The vanilla/Create
 * downward output path remains unchanged for every unbound chute.
 */
@Mixin(ChuteBlockEntity.class)
public abstract class ChuteKineticPressInputMixin {
    @Inject(method = "handleDownwardOutput", at = @At("HEAD"), cancellable = true)
    private void createsandustry$insertIntoBoundPress(boolean simulate,
                                                      CallbackInfoReturnable<Boolean> cir) {
        ChuteBlockEntity chute = (ChuteBlockEntity) (Object) this;
        Level level = chute.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        ItemStack held = chute.getItem();
        if (held.isEmpty()) {
            return;
        }

        KineticPressBlockEntity press =
                KineticPressBlockEntity.findForBoundChute(level, chute.getBlockPos());
        if (press == null) {
            return;
        }

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                press.getInputHandler(), held, simulate);
        if (!simulate) {
            chute.setItem(remainder);
        }
        // Bound chutes hold their item when the press is busy/full instead of
        // falling back to a real ItemEntity drop.
        cir.setReturnValue(true);
    }
}