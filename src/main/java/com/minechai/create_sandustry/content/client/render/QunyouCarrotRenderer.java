package com.minechai.create_sandustry.content.client.render;

import com.minechai.create_sandustry.content.entity.QunyouCarrotProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * 群友胡萝卜的独立渲染器。
 *
 * <p>原版 {@code minecraft:item/carrot} 继承 {@code builtin/generated}。生成模型的主面是
 * SOUTH 面，纹理尖端 (2,14) 映射到局部 (2,2)，所以从模型中心指向尖端的局部轴为
 * {@link #LOCAL_TIP}。渲染器把由“尖端轴、侧轴、面法线”组成的局部正交基直接旋转到
 * “实际速度、目标侧轴、近似世界上方”的坐标系，避免拆分 yaw/pitch 时重复修正。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class QunyouCarrotRenderer<T extends QunyouCarrotProjectile> extends EntityRenderer<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float ITEM_SCALE = 0.65F;
    /** 悬浮时尖端向上倾斜，发射后仍完全使用实际速度朝向。 */
    private static final float HOVER_PITCH_DEGREES = 55.0F;
    private static final float HALF_SQRT_TWO = (float) (Math.sqrt(2.0D) * 0.5D);

    /** 纹理 (2,14) -> 模型 (2,2)，相对模型中心的尖端方向。 */
    private static final Vector3f LOCAL_TIP = new Vector3f(-HALF_SQRT_TWO, -HALF_SQRT_TWO, 0.0F);
    /** 原版生成模型 SOUTH 面的局部法线。 */
    private static final Vector3f LOCAL_NORMAL = new Vector3f(0.0F, 0.0F, 1.0F);
    /** LOCAL_NORMAL × LOCAL_TIP，补齐右手正交基。 */
    private static final Vector3f LOCAL_SIDE = new Vector3f(LOCAL_NORMAL).cross(LOCAL_TIP).normalize();
    private static final Matrix3f LOCAL_BASIS = new Matrix3f()
            .setColumn(0, LOCAL_TIP)
            .setColumn(1, LOCAL_SIDE)
            .setColumn(2, LOCAL_NORMAL);

    private static final boolean DEBUG_ORIENTATION =
            Boolean.getBoolean("createsandustry.debugCarrotOrientation");
    private static int lastDebugTick = Integer.MIN_VALUE;

    private final ItemRenderer itemRenderer;

    public QunyouCarrotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        Vec3 direction = entity.getDeltaMovement();
        boolean hovering = direction.lengthSqr() < 1.0E-8D;
        if (hovering) {
            float yawRadians = entity.getViewYRot(partialTicks) * Mth.DEG_TO_RAD;
            float pitchRadians = HOVER_PITCH_DEGREES * Mth.DEG_TO_RAD;
            float horizontal = Mth.cos(pitchRadians);
            direction = new Vec3(-Mth.sin(yawRadians) * horizontal, Mth.sin(pitchRadians),
                    Mth.cos(yawRadians) * horizontal);
        }
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }

        Quaternionf orientation = orientationFor(direction);
        if (hovering) {
            orientation.mul(new Quaternionf().rotationZ(Mth.PI));
        }
        logOrientation(entity, direction, orientation);

        poseStack.pushPose();
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        poseStack.mulPose(orientation);
        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * 局部基的三列分别是尖端方向、侧方向、模型面法线；目标基的三列分别是
     * 实际速度方向、与速度垂直的侧方向、尽量接近世界上方的法线。
     */
    private static Quaternionf orientationFor(Vec3 direction) {
        Vector3f forward = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)
                .normalize();

        Vector3f referenceUp = Math.abs(forward.y) > 0.999F
                ? new Vector3f(0.0F, 0.0F, 1.0F)
                : new Vector3f(0.0F, 1.0F, 0.0F);
        float projection = forward.dot(referenceUp);
        Vector3f targetNormal = new Vector3f(referenceUp)
                .sub(new Vector3f(forward).mul(projection))
                .normalize();
        Vector3f targetSide = new Vector3f(targetNormal).cross(forward).normalize();

        Matrix3f targetBasis = new Matrix3f()
                .setColumn(0, forward)
                .setColumn(1, targetSide)
                .setColumn(2, targetNormal);
        Matrix3f rotation = targetBasis.mul(LOCAL_BASIS.transpose(new Matrix3f()));
        return rotation.getNormalizedRotation(new Quaternionf());
    }

    private static void logOrientation(QunyouCarrotProjectile entity, Vec3 direction, Quaternionf orientation) {
        if (!DEBUG_ORIENTATION || lastDebugTick == entity.tickCount) {
            return;
        }
        lastDebugTick = entity.tickCount;
        Vector3f transformedTip = new Vector3f(LOCAL_TIP).rotate(orientation).normalize();
        Vector3f velocity = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z).normalize();
        LOGGER.info("Qunyou carrot orientation: localTip={}, velocity={}, transformedTip={}, alignment={}",
                LOCAL_TIP, velocity, transformedTip, transformedTip.dot(velocity));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}