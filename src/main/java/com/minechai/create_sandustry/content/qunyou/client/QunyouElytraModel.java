package com.minechai.create_sandustry.content.qunyou.client;

import com.minechai.create_sandustry.content.qunyou.Qunyou;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 群友专用鞘翅模型。
 *
 * <p>几何来自原版 {@code ModelLayers.ELYTRA}，贴图仍为原版鞘翅贴图，
 * 但左右翼旋转由第三阶段姿态控制器单独驱动，不复用玩家鞘翅的姿态状态。</p>
 */
@OnlyIn(Dist.CLIENT)
final class QunyouElytraModel extends EntityModel<Qunyou> {
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    QunyouElytraModel(ModelPart root) {
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    void setPose(float xRot, float zRot) {
        this.leftWing.xRot = xRot;
        this.rightWing.xRot = xRot;
        this.leftWing.yRot = 0.0F;
        this.rightWing.yRot = 0.0F;
        this.leftWing.zRot = -zRot;
        this.rightWing.zRot = zRot;
    }

    @Override
    public void setupAnim(Qunyou entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        // 旋转由 QunyouFlightPoseController 的统一姿态采样提供。
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
            int color) {
        this.leftWing.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.rightWing.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
