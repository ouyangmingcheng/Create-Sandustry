package com.minechai.create_sandustry.content.client.model;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.content.entity.Qunyou;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 群友使用原版猪几何和纹理，只在此处叠加状态动画。
 *
 * <p>模型本体不替换猪的头部、身体和四条腿结构；倒立/翻转通过渲染姿态绕模型中心旋转，
 * 碰撞箱不参与旋转。所有状态和时间来自服务端同步的 {@link Qunyou} 数据。</p>
 */
public final class QunyouModel<T extends Qunyou> extends PigModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "qunyou"), "main");

    private static final float BODY_BASE_X_ROT = (float) (Math.PI / 2.0D);
    private final QunyouFlightPoseController flightPose = new QunyouFlightPoseController();
    private float modelRoll;

    public QunyouModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        return PigModel.createBodyLayer(CubeDeformation.NONE);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        byte state = entity.getState();
        float partial = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        float ticks = entity.getStateTicks(partial);
        float duration = stateDuration(state);
        float progress = Mth.clamp(ticks / Math.max(1.0F, duration), 0.0F, 1.0F);
        float pulse = Mth.sin(progress * Mth.PI);

        // Reset parts changed by the previous frame. QuadrupedModel resets head and leg X rotations.
        this.body.xRot = BODY_BASE_X_ROT;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.head.zRot = 0.0F;
        this.rightHindLeg.zRot = 0.0F;
        this.leftHindLeg.zRot = 0.0F;
        this.rightFrontLeg.zRot = 0.0F;
        this.leftFrontLeg.zRot = 0.0F;
        this.modelRoll = 0.0F;

        if (entity.isThirdPhase() && !entity.isDeadOrDying()) {
            this.flightPose.update(entity, partial, ageInTicks,
                    this.head.xRot,
                    this.rightHindLeg.xRot, this.leftHindLeg.xRot,
                    this.rightFrontLeg.xRot, this.leftFrontLeg.xRot);
            this.body.xRot += this.flightPose.getBodyPitch();
            this.body.zRot += this.flightPose.getBodyRoll();
            this.head.xRot += this.flightPose.getHeadPitch();
            this.head.zRot += this.flightPose.getHeadRoll();
            this.rightHindLeg.xRot = this.flightPose.getRightHindLeg();
            this.leftHindLeg.xRot = this.flightPose.getLeftHindLeg();
            this.rightFrontLeg.xRot = this.flightPose.getRightFrontLeg();
            this.leftFrontLeg.xRot = this.flightPose.getLeftFrontLeg();
            if (state == Qunyou.PHASE3_AIR_MANEUVER) {
                float maneuverDuration = Math.max(1.0F,
                        ModConfigs.QUNYOU_PIG_MANEUVER_DURATION.get().floatValue());
                float maneuverProgress = Mth.clamp(ticks / maneuverDuration, 0.0F, 1.0F);
                float eased = maneuverProgress * maneuverProgress * (3.0F - 2.0F * maneuverProgress);
                this.modelRoll = Mth.PI * 2.0F * eased;
            }
        } else if (!entity.isThirdPhase()) {
            switch (state) {
            case Qunyou.MELEE -> {
                this.body.xRot += 0.16F * pulse;
                this.head.xRot -= 0.24F * pulse;
            }
            case Qunyou.CHARGE_PREPARE -> {
                this.body.xRot -= 0.28F * pulse;
                this.head.xRot += 0.18F * pulse;
            }
            case Qunyou.CHARGE -> {
                boolean boosted = entity.isChargeBoostActive();
                this.body.xRot += boosted ? 0.34F : 0.22F;
                this.head.xRot += boosted ? 0.2F : 0.12F;
                float stride = Mth.sin(ageInTicks * (boosted ? 3.2F : 2.2F)) * (boosted ? 1.15F : 0.9F);
                this.rightHindLeg.xRot = stride;
                this.leftHindLeg.xRot = -stride;
                this.rightFrontLeg.xRot = -stride;
                this.leftFrontLeg.xRot = stride;
            }
            case Qunyou.DODGE -> {
                // Model +X is the pig's left. Right dodge must therefore swing the legs toward +X.
                float side = entity.isDodgeRight() ? -0.36F : 0.36F;
                this.rightHindLeg.zRot = side;
                this.leftHindLeg.zRot = side;
                this.rightFrontLeg.zRot = side;
                this.leftFrontLeg.zRot = side;
                this.body.zRot = -side * 0.45F;
            }
            case Qunyou.RETREAT -> {
                float stride = Mth.sin(ageInTicks * 1.7F) * 0.75F;
                this.rightHindLeg.xRot = stride;
                this.leftHindLeg.xRot = -stride;
                this.rightFrontLeg.xRot = -stride;
                this.leftFrontLeg.xRot = stride;
                this.body.xRot -= 0.08F;
            }
            case Qunyou.INVERTED -> {
                this.modelRoll = Mth.PI;
                this.head.xRot += 0.22F;
            }
            case Qunyou.SLEEPING -> {
                this.modelRoll = Mth.PI;
                this.head.xRot += 0.35F;
                this.rightHindLeg.xRot = 0.55F;
                this.leftHindLeg.xRot = 0.55F;
                this.rightFrontLeg.xRot = 0.55F;
                this.leftFrontLeg.xRot = 0.55F;
            }
            case Qunyou.FLIP -> {
                float eased = progress * progress * (3.0F - 2.0F * progress);
                this.modelRoll = Mth.PI * (1.0F - eased);
            }
            case Qunyou.SUMMON -> {
                this.body.xRot += 0.035F * Mth.sin(ageInTicks * 0.2F);
                this.head.xRot += 0.08F * Mth.sin(ageInTicks * 0.16F);
            }
            case Qunyou.DEFENDING -> {
                this.body.xRot -= 0.18F;
                this.head.xRot += 0.16F;
                this.rightFrontLeg.xRot = -0.12F;
                this.leftFrontLeg.xRot = -0.12F;
            }
            case Qunyou.JUMPING -> {
                this.body.xRot += 0.24F;
                this.head.xRot -= 0.14F;
                this.rightHindLeg.xRot = 0.45F;
                this.leftHindLeg.xRot = 0.45F;
                this.rightFrontLeg.xRot = -0.35F;
                this.leftFrontLeg.xRot = -0.35F;
            }
            case Qunyou.PHASE3_PENDING -> {
                this.body.xRot += 0.06F * Mth.sin(ageInTicks * 0.7F);
            }
            case Qunyou.ASCENDING -> {
                this.body.xRot -= 0.25F;
                this.head.xRot -= 0.2F;
                this.rightHindLeg.xRot = 0.5F;
                this.leftHindLeg.xRot = 0.5F;
                this.rightFrontLeg.xRot = 0.5F;
                this.leftFrontLeg.xRot = 0.5F;
            }
            case Qunyou.GLIDING -> {
                this.body.xRot += Mth.clamp(entity.getXRot() * Mth.DEG_TO_RAD * 0.45F, -0.35F, 0.35F);
                this.head.xRot -= 0.16F;
                this.rightHindLeg.xRot = 0.58F;
                this.leftHindLeg.xRot = 0.58F;
                this.rightFrontLeg.xRot = 0.42F;
                this.leftFrontLeg.xRot = 0.42F;
            }
            case Qunyou.TAKEOFF -> {
                this.body.xRot += 0.12F;
                this.head.xRot -= 0.18F;
                this.rightHindLeg.xRot = 0.4F;
                this.leftHindLeg.xRot = 0.4F;
            }
            case Qunyou.DESCENDING_ATTACK -> {
                this.body.xRot += 0.42F;
                this.head.xRot -= 0.24F;
                this.rightHindLeg.xRot = 0.62F;
                this.leftHindLeg.xRot = 0.62F;
            }
            default -> {
                // The vanilla pig idle animation remains.
            }
            }
        }

        if (entity.isEating()) {
            float chew = Mth.sin(ageInTicks * 1.7F);
            this.body.xRot += 0.035F * chew;
            this.head.xRot += 0.18F + 0.12F * chew;
        }

        int phaseTicks = entity.getPhaseTicks(partial);
        if (phaseTicks >= 0 && phaseTicks < Qunyou.PHASE_TRANSITION_DURATION) {
            float intensity = 1.0F - phaseTicks / (float) Qunyou.PHASE_TRANSITION_DURATION;
            float shake = Mth.sin(ageInTicks * 2.4F) * 0.12F * intensity;
            this.body.zRot += shake;
            this.head.yRot += shake;
        }

        int breakTicks = entity.getShieldBreakTicks(partial);
        if (breakTicks >= 0 && breakTicks < Qunyou.SHIELD_BREAK_FEEDBACK_DURATION) {
            float intensity = 1.0F - breakTicks / (float) Qunyou.SHIELD_BREAK_FEEDBACK_DURATION;
            float shake = Mth.sin(ageInTicks * 2.8F) * 0.2F * intensity;
            this.body.zRot += shake;
            this.head.zRot += shake;
        }
    }

    /** 将 PoseStack 精确转换到原版猪 body 部件的局部坐标系。 */
    public void translateToBody(PoseStack poseStack) {
        this.body.translateAndRotate(poseStack);
    }

    /** 将 PoseStack 转换到原版猪 head 部件局部坐标。 */
    public void translateToHead(PoseStack poseStack) {
        this.head.translateAndRotate(poseStack);
    }

    public float getElytraXRot() {
        return this.flightPose.getElytraXRot();
    }

    public float getElytraZRot() {
        return this.flightPose.getElytraZRot();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
            int color) {
        if (Math.abs(this.modelRoll) < 1.0E-4F) {
            super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
            return;
        }

        // Rotate around the model's mid-height so the inverted pig stays visually grounded.
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(this.modelRoll));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    private static float stateDuration(byte state) {
        return switch (state) {
            case Qunyou.MELEE -> Qunyou.MELEE_DURATION;
            case Qunyou.CHARGE_PREPARE -> Qunyou.CHARGE_PREPARE_DURATION;
            case Qunyou.CHARGE -> Qunyou.CHARGE_DURATION;
            case Qunyou.DODGE -> Qunyou.DODGE_DURATION;
            case Qunyou.RETREAT -> Qunyou.RETREAT_DURATION;
            case Qunyou.INVERTED -> Qunyou.INVERTED_DURATION;
            case Qunyou.SLEEPING -> Qunyou.SLEEP_DURATION;
            case Qunyou.FLIP -> Qunyou.FLIP_DURATION;
            case Qunyou.SUMMON -> Qunyou.SUMMON_HOVER_TICKS + 2 * Qunyou.SUMMON_LAUNCH_INTERVAL
                    + Qunyou.SUMMON_END_DELAY;
            case Qunyou.DEFENDING -> Qunyou.DEFENSE_DURATION;
            case Qunyou.JUMPING -> Qunyou.JUMP_DURATION;
            case Qunyou.ASCENDING -> Qunyou.ASCEND_DURATION;
            case Qunyou.GLIDING -> 1.0F;
            case Qunyou.TAKEOFF -> Qunyou.TAKEOFF_MAX_DURATION;
            case Qunyou.DESCENDING_ATTACK -> Qunyou.DESCEND_MAX_DURATION;
            case Qunyou.PHASE3_COMBO_AIM -> Qunyou.PHASE3_COMBO_AIM_DURATION;
            case Qunyou.PHASE3_COMBO_CHARGE -> Qunyou.PHASE3_COMBO_CHARGE_MAX_DURATION;
            case Qunyou.PHASE3_EJECTION_CHARGE -> ModConfigs.QUNYOU_PIG_EJECTION_CHARGE_TICKS.get();
            case Qunyou.PHASE3_EJECTION_RUSH -> ModConfigs.QUNYOU_PIG_EJECTION_MAX_TICKS.get();
            case Qunyou.PHASE3_EJECTION_RECOVER -> ModConfigs.QUNYOU_PIG_EJECTION_CHASE_RECOVERY_TICKS.get();
            case Qunyou.PHASE3_EJECTION_ASCEND -> 80.0F;
            case Qunyou.PHASE3_PIG_RETREAT_CHARGE ->
                    ModConfigs.QUNYOU_PIG_RETREAT_CHARGE_TICKS.get();
            case Qunyou.PHASE3_PIG_RETREAT_HORIZONTAL ->
                    ModConfigs.QUNYOU_PIG_RETREAT_HORIZONTAL_TICKS.get();
            case Qunyou.PHASE3_PIG_RETREAT_ASCEND -> 100.0F;
            case Qunyou.PHASE3_AIR_MANEUVER ->
                    ModConfigs.QUNYOU_PIG_MANEUVER_DURATION.get();
            default -> 1.0F;
        };
    }
}
