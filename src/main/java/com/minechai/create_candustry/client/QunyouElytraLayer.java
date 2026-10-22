package com.minechai.create_candustry.client;

import com.minechai.create_candustry.entity.Qunyou;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import org.slf4j.Logger;
import net.neoforged.api.distmarker.OnlyIn;

/** 第三阶段专用鞘翅渲染，不修改原版鞘翅资源或其他实体。 */
@OnlyIn(Dist.CLIENT)
public final class QunyouElytraLayer<T extends Qunyou> extends RenderLayer<T, QunyouModel<T>> {
    private static final ResourceLocation ELYTRA_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_ATTACHMENT =
            Boolean.getBoolean("createsandustry.debugQunyouElytra");
    // body 局部 -Y 对应猪模型的世界前向 -Z；当前锚点位于背部中央略靠前。
    private static final float ATTACH_Y = -0.2F;
    private static final float ATTACH_Z = -0.05F;
    private static final float ELYTRA_SCALE = 0.85F;
    private static int lastDebugState = -1;
    private final QunyouElytraModel model;

    public QunyouElytraLayer(RenderLayerParent<T, QunyouModel<T>> parent, EntityModelSet modelSet) {
        super(parent);
        this.model = new QunyouElytraModel(modelSet.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (!entity.isThirdPhase() || !entity.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }

        poseStack.pushPose();
        // 转换到 PigModel.body：枢轴 (0,11,2)px、X+90°，随后在身体局部坐标安装。
        this.getParentModel().translateToBody(poseStack);
        poseStack.translate(0.0F, ATTACH_Y, ATTACH_Z);
        poseStack.scale(ELYTRA_SCALE, ELYTRA_SCALE, ELYTRA_SCALE);
        if (DEBUG_ATTACHMENT && lastDebugState != entity.getState()) {
            lastDebugState = entity.getState();
            LOGGER.info("Qunyou elytra attach: bodyPivotPx=(0,11,2), bodyXRot=90deg, "
                    + "attachBodyLocal=(0,{},{}), scale={}, state={}",
                    ATTACH_Y, ATTACH_Z, ELYTRA_SCALE, entity.getState());
        }
        this.model.setPose(this.getParentModel().getElytraXRot(), this.getParentModel().getElytraZRot());
        VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffer,
                RenderType.armorCutoutNoCull(ELYTRA_TEXTURE), false);
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
