package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.advancement.GoldenFriedRiceAdvancements;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects a Golden Selection Egg Fried Rice actually entering the player's
 * inventory. Covers both {@link Inventory#add(int, ItemStack)} (item pickup,
 * {@code /give}, cooking-device give-back) and
 * {@link Inventory#setItem(int, ItemStack)} (container GUIs such as taking the
 * item out of a chest).
 */
@Mixin(Inventory.class)
public abstract class InventoryGoldenFriedRiceMixin {
    @Shadow
    @Final
    public Player player;

    @Unique
    private boolean createsandustry$capturedGoldenEggFriedRice;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void createsandustry$captureGoldenAdd(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        this.createsandustry$capturedGoldenEggFriedRice =
                GoldenFriedRiceAdvancements.isGoldenEggFriedRice(stack);
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private void createsandustry$awardGoldenFriedRiceFromAdd(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && this.createsandustry$capturedGoldenEggFriedRice) {
            GoldenFriedRiceAdvancements.award(this.player);
        }
    }

    @Inject(method = "setItem(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void createsandustry$awardGoldenFriedRiceFromSlot(int index, ItemStack stack, CallbackInfo ci) {
        if (GoldenFriedRiceAdvancements.isGoldenEggFriedRice(stack)) {
            GoldenFriedRiceAdvancements.award(this.player);
        }
    }
}
