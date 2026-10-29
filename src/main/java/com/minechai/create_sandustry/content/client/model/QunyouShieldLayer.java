package com.minechai.create_sandustry.content.client.model;

import com.minechai.create_sandustry.content.entity.Qunyou;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** 在群友防御时把副手盾牌稳定地显示在身体前方。 */
@OnlyIn(Dist.CLIENT)
public final class QunyouShieldLayer<T extends Qunyou> extends RenderLayer<T, QunyouModel<T>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public QunyouShieldLayer(RenderLayerParent<T, QunyouModel<T>> parent,
            ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (!entity.isDefending()) {
            return;
        }
        ItemStack shield = entity.getOffhandItem();
        if (shield.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.70F, -0.95F);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        // FIXED 是盾牌自身的展示变换；不会再叠加手部骨骼的左/右手旋转。
        this.itemInHandRenderer.renderItem(entity, shield, ItemDisplayContext.FIXED,
                false, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}