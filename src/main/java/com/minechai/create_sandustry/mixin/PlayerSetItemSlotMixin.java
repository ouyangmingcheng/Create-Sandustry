package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.advancement.GoldenFriedRiceAdvancements;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects a Golden Selection Egg Fried Rice being put directly into the
 * player's main hand or offhand. This covers cooking devices that hand the
 * result straight into an empty hand (bypassing {@code Inventory.add}).
 */
@Mixin(Player.class)
public abstract class PlayerSetItemSlotMixin {
    @Inject(method = "setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void createsandustry$awardGoldenFriedRiceFromHand(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (GoldenFriedRiceAdvancements.isGoldenEggFriedRice(stack)) {
            GoldenFriedRiceAdvancements.award((Player) (Object) this);
        }
    }
}
