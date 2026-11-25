package com.minechai.create_sandustry.mixin.client;

import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 拔剑期间让玩家双手持续向前伸出，而不是循环播放挥臂动画。 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void createsandustry$posePullingArms(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player) || !player.getData(ModAttachments.SWORD_PULL_ACTIVE)) {
            return;
        }

        HumanoidModel<?> self = (HumanoidModel<?>) (Object) this;
        self.rightArm.xRot = -1.32F;
        self.rightArm.yRot = -0.18F;
        self.rightArm.zRot = 0.07F;
        self.leftArm.xRot = -1.32F;
        self.leftArm.yRot = 0.18F;
        self.leftArm.zRot = -0.07F;
    }
}
