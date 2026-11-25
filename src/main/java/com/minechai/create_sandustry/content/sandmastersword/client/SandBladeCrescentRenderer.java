package com.minechai.create_sandustry.content.sandmastersword.client;

import com.minechai.create_sandustry.content.sandmastersword.SandBladeCrescent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

/**
 * 半月形剑气渲染：直接使用原版横扫攻击粒子纹理 {@code sweep_3.png} 的弧形轮廓。
 *
 * <p>四边形盘面法线固定为世界竖直方向，因此两道半月剑气始终平行于地面，并在水平面内自转；
 * 它不是始终朝向摄像机的 billboard。两道半月剑气分别使用相反的自转方向和略微不同的位置。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SandBladeCrescentRenderer extends EntityRenderer<SandBladeCrescent> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/particle/sweep_3.png");
    private static final float SIZE = 1.1F;

    public SandBladeCrescentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SandBladeCrescent entity) {
        return TEXTURE;
    }

    @Override
    public void render(SandBladeCrescent entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float spin = (entity.tickCount + partialTick) * (entity.getVariant() == 0 ? 30.0F : -30.0F)
                * Mth.DEG_TO_RAD;
        Quaternionf rotation = new Quaternionf()
                // 半月盘面法线固定为世界 +Y：两道剑气始终平行于地面。
                .rotateTo(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F)
                .mul(new Quaternionf().rotationZ(spin));

        int color = argb(255, 255, 226, 164);

        poseStack.pushPose();
        poseStack.mulPose(rotation);
        poseStack.scale(SIZE, SIZE, SIZE);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        addQuad(consumer, pose, color);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(SandBladeCrescent entity, Frustum frustum, double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(SIZE + 0.5F));
    }

    private static void addQuad(VertexConsumer consumer, PoseStack.Pose pose, int color) {
        addVertex(consumer, pose, -0.5F, -0.5F, 0.0F, 1.0F, color);
        addVertex(consumer, pose, -0.5F, 0.5F, 0.0F, 0.0F, color);
        addVertex(consumer, pose, 0.5F, 0.5F, 1.0F, 0.0F, color);
        addVertex(consumer, pose, 0.5F, -0.5F, 1.0F, 1.0F, color);

        // 双面渲染，确保剑气绕过障碍物或改变朝向时不会突然消失。
        addVertex(consumer, pose, 0.5F, -0.5F, 1.0F, 1.0F, color);
        addVertex(consumer, pose, 0.5F, 0.5F, 1.0F, 0.0F, color);
        addVertex(consumer, pose, -0.5F, 0.5F, 0.0F, 0.0F, color);
        addVertex(consumer, pose, -0.5F, -0.5F, 0.0F, 1.0F, color);
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v,
            int color) {
        consumer.addVertex(pose.pose(), x, y, 0.0F)
                .setColor(color >>> 16 & 0xFF, color >>> 8 & 0xFF, color & 0xFF, color >>> 24)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }
}
