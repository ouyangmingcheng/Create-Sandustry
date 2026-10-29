package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 拔剑期间禁止客户端/服务端反复触发右键挥臂动作。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void createsandustry$cancelSwingWhilePulling(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        if ((Object) this instanceof Player player
                && player.getData(ModAttachments.SWORD_PULL_ACTIVE)) {
            ci.cancel();
        }
    }
}
