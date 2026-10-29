package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版第一人称 renderHand 会分别把手和袖子 xRot 清零。
 * 在两次字段赋值之后立即修正，并让袖子复制手部的完整姿态，保证两者不会反向错位。
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "renderHand", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/model/geom/ModelPart;xRot:F",
            opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
    private void createsandustry$restorePullingArmRotation(PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, AbstractClientPlayer player, ModelPart rendererArm, ModelPart rendererArmwear,
            CallbackInfo ci) {
        if (player.getData(ModAttachments.SWORD_PULL_ACTIVE)) {
            PlayerRenderer renderer = (PlayerRenderer) (Object) this;
            boolean rightArm = rendererArm == renderer.getModel().rightArm;
            rendererArm.xRot = -0.55F;
            rendererArm.yRot = rightArm ? -0.18F : 0.18F;
            rendererArm.zRot = rightArm ? -0.08F : 0.08F;
        }
    }

    @Inject(method = "renderHand", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/model/geom/ModelPart;xRot:F",
            opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    private void createsandustry$syncPullingSleeveRotation(PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, AbstractClientPlayer player, ModelPart rendererArm, ModelPart rendererArmwear,
            CallbackInfo ci) {
        if (player.getData(ModAttachments.SWORD_PULL_ACTIVE)) {
            rendererArmwear.copyFrom(rendererArm);
        }
    }
}
