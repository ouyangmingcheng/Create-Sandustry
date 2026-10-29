package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 统一平移第一人称双手和袖子，避免只移动 ModelPart 枢轴导致手部消失。 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void createsandustry$pushPullingHandPose(float partialTicks, PoseStack poseStack,
            MultiBufferSource.BufferSource buffer, LocalPlayer player, int combinedLight, CallbackInfo ci) {
        if (player.getData(ModAttachments.SWORD_PULL_ACTIVE)) {
            poseStack.pushPose();
            // 相机局部坐标中 -Z 为前方；整体下移并前移，避免手贴在胸口。
            poseStack.translate(0.0F, -0.12F, -0.35F);
        }
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void createsandustry$popPullingHandPose(float partialTicks, PoseStack poseStack,
            MultiBufferSource.BufferSource buffer, LocalPlayer player, int combinedLight, CallbackInfo ci) {
        if (player.getData(ModAttachments.SWORD_PULL_ACTIVE)) {
            poseStack.popPose();
        }
    }
}
