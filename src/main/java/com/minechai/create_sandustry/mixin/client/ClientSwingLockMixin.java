package com.minechai.create_sandustry.mixin.client;

import com.minechai.create_sandustry.content.sandmastersword.client.SwordPullClient;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 成功拔剑后等待松手时，也屏蔽右键重复触发的挥臂动画。 */
@Mixin(LivingEntity.class)
public abstract class ClientSwingLockMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void createsandustry$cancelSwingWhileWaitingForRelease(InteractionHand hand, boolean updateSelf,
            CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer && SwordPullClient.isReleaseLockPending()) {
            ci.cancel();
        }
    }
}
