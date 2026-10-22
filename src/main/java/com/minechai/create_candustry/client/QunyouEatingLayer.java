package com.minechai.create_candustry.client;

import com.minechai.create_candustry.entity.Qunyou;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** 在群友嘴部附近渲染并来回送食的胡萝卜。 */
@OnlyIn(Dist.CLIENT)
public final class QunyouEatingLayer<T extends Qunyou> extends RenderLayer<T, QunyouModel<T>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public QunyouEatingLayer(RenderLayerParent<T, QunyouModel<T>> parent,
            ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (!entity.isEating()) {
            return;
        }
        ItemStack carrot = entity.getMainHandItem();
        if (carrot.isEmpty()) {
            return;
        }

        float feeding = 0.5F + 0.5F * Mth.sin(ageInTicks * 1.7F);
        poseStack.pushPose();
        if (entity.isAirEating()) {
            // 空中进食跟随猪头局部姿态，金胡萝卜在嘴部前方反复送食。
            this.getParentModel().translateToHead(poseStack);
            poseStack.translate(0.0F, 0.12F + feeding * 0.03F, -0.60F + feeding * 0.10F);
            poseStack.scale(0.58F, 0.58F, 0.58F);
        } else {
            // 地面进食沿用实体局部坐标；实体局部 -Z 是猪的正前方。
            float sideOffset = entity.isDefending() ? 0.42F : 0.0F;
            poseStack.translate(sideOffset, 0.68F + feeding * 0.05F, -1.12F + feeding * 0.22F);
            poseStack.scale(0.72F, 0.72F, 0.72F);
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        this.itemInHandRenderer.renderItem(entity, carrot, ItemDisplayContext.FIXED,
                false, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}