package com.minechai.create_sandustry.mixin.createstuffadditions;

import com.minechai.create_sandustry.content.compat.createstuffadditions.BrassArmorFlorinolCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Enables the netherite jetpack's lava elytra mode while it is FE-bound. */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.item.NetheriteJetpackItem$Chestplate", remap = false)
public abstract class NetheriteJetpackElytraMixin {
    @Inject(method = "canElytraFly", at = @At("HEAD"), cancellable = true)
    private void createsandustry$allowFlorinolLavaElytra(ItemStack stack, LivingEntity entity,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (entity.isInLava() && BrassArmorFlorinolCompat.canOperate(entity.level(), stack)) {
            cir.setReturnValue(true);
        }
    }
}