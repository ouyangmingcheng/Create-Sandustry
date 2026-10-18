package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.entity.SandMasterSwordBeam;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

/**
 * 剑气渲染：一张圆形能量贴图的四边形，<b>盘面基本平行于地面</b>，并绕盘面法线持续自转。
 *
 * <p><b>局部坐标系</b>：四边形顶点位于局部 XY 平面（z = 0），因此它的法线是局部 <b>+Z</b>；
 * 贴图的旋转对应“绕局部 Z 轴旋转”。</p>
 *
 * <p><b>旋转顺序</b>（{@link Quaternionf#mul} 的右侧先作用）：</p>
 * <ol>
 *     <li>先绕局部 Z 轴按 {@code tickCount + partialTick} 自转（贴图旋转，帧间平滑）；</li>
 *     <li>再把局部 +Z 对准计算出的<b>盘面法线</b>（{@link Quaternionf#rotateTo}）。</li>
 * </ol>
 *
 * <p><b>盘面法线</b>：水平射击时法线 = +Y ⇒ 盘面平行地面；俯仰越大，法线向飞行方向的反侧倾斜，
 * 最多 {@value #MAX_TILT_DEGREES}°，所以向上/向下射击时只是适度倾斜，永远不会回到
 * “盘面完全垂直于飞行方向”的旧效果。正上/正下射击时水平分量为 0，任取一个水平方向即可
 * （圆形盘在该退化情形下朝向无差别）。</p>
 *
 * <p><b>尺寸</b>：基础视觉直径 {@value #SIZE} 格（横扫之刃 0 级，原 1.2 格的两倍），并随横扫之刃等级
 * 每级 +20%；实体碰撞箱仍是实体类型定义的 0.9 格，
 * 不随视觉放大。为避免放大后的四边形在屏幕边缘被 0.9 格碰撞箱的视锥裁剪误裁，{@link #shouldRender}
 * 用略放大的包围盒判断。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SandMasterSwordBeamRenderer extends EntityRenderer<SandMasterSwordBeam> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID,
            "textures/entity/sand_master_sword_beam.png");
    /** 基础视觉直径（格，横扫之刃 0 级时）：原 1.2 格的两倍；有横扫之刃时再按等级放大。 */
    private static final float SIZE = 2.4F;
    /** 自转速度：度/tick。 */
    private static final float SPIN_DEGREES_PER_TICK = 28.0F;
    /** 俯仰倾斜上限（度）：保证盘面永远不会完全垂直于飞行方向。 */
    private static final double MAX_TILT_DEGREES = 45.0D;

    public SandMasterSwordBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SandMasterSwordBeam entity) {
        return TEXTURE;
    }

    @Override
    public void render(SandMasterSwordBeam entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 dir = motion.normalize();

        // 水平分量：决定倾斜方向；正上/正下射击时退化，任取一个水平方向。
        double horizontalLength = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        Vec3 flat = horizontalLength < 1.0E-4D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(dir.x / horizontalLength, 0.0D, dir.z / horizontalLength);

        // 盘面法线：水平射击 = +Y；俯仰越大越向飞行方向反侧倾斜（最多 45°）。
        double tilt = Math.toRadians(MAX_TILT_DEGREES * dir.y);
        Vec3 normal = new Vec3(-flat.x * Math.sin(tilt), Math.cos(tilt), -flat.z * Math.sin(tilt)).normalize();

        // 先绕局部 Z 自转，再把局部 +Z（四边形法线）对准盘面法线；右侧先作用 ⇒ facing.mul(spin)。
        Quaternionf spin = new Quaternionf().rotationZ(
                (entity.tickCount + partialTick) * SPIN_DEGREES_PER_TICK * Mth.DEG_TO_RAD);
        Quaternionf rotation = new Quaternionf()
                .rotateTo(0.0F, 0.0F, 1.0F, (float) normal.x, (float) normal.y, (float) normal.z)
                .mul(spin);

        // 横扫之刃每级 +20% 视觉直径（碰撞箱不变，命中范围在实体逻辑里按同一倍率做群伤扫掠）。
        float scale = SIZE * (float) entity.getSizeScale();
        poseStack.pushPose();
        poseStack.mulPose(rotation);
        poseStack.scale(scale, scale, scale);

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        float half = 0.5F;
        addVertex(consumer, pose, -half, -half, 0.0F, 1.0F);
        addVertex(consumer, pose, -half, half, 0.0F, 0.0F);
        addVertex(consumer, pose, half, half, 1.0F, 0.0F);
        addVertex(consumer, pose, half, -half, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(SandMasterSwordBeam entity, Frustum frustum, double camX, double camY, double camZ) {
        // 视觉尺寸大于 0.9 格碰撞箱：用放大的包围盒做视锥裁剪，避免屏幕边缘整块消失。
        float scale = SIZE * (float) entity.getSizeScale();
        return frustum.isVisible(entity.getBoundingBox().inflate(scale * 0.75D));
    }

    /** 单个顶点：不透明白色 + 全亮光照（剑气自发光），UV 由参数给出。 */
    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.addVertex(pose.pose(), x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}