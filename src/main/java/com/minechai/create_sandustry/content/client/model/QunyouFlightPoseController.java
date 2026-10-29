package com.minechai.create_sandustry.content.client.model;

import com.minechai.create_sandustry.content.entity.Qunyou;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 第三阶段专用的无状态飞行姿态采样器。
 *
 * <p>姿态完全由服务端同步状态、状态起始刻、实体俯仰角和当前速度计算。
 * 每次状态切换都在固定窗口内从上一姿态平滑过渡到新姿态，不在客户端保存
 * 会跨实体共享或随帧率漂移的可变动画计时。</p>
 */
@OnlyIn(Dist.CLIENT)
final class QunyouFlightPoseController {
    private static final float DEG_TO_RAD = Mth.DEG_TO_RAD;
    private static final float RAD_TO_DEG = (float) (180.0D / Math.PI);
    private static final float TRANSITION_TICKS = 10.0F;
    private static final float MIN_MOTION_SPEED = 0.025F;

    // 升天姿态
    private static final float ASCEND_BODY_PITCH = -0.28F;
    private static final float ASCEND_BODY_ROLL = 0.035F;
    private static final float ASCEND_HEAD_PITCH = -0.18F;
    private static final float ASCEND_LEG_BASE = 0.38F;
    private static final float ASCEND_FRONT_LEG_BASE = 0.34F;
    private static final float ASCEND_LEG_SWING = 0.18F;
    private static final float ASCEND_ELYTRA_X = -0.34F;
    private static final float ASCEND_ELYTRA_Z = 0.34F;
    // 滑翔姿态
    private static final float GLIDE_BODY_PITCH_FACTOR = 0.22F;
    private static final float GLIDE_BODY_PITCH_LIMIT = 0.22F;
    private static final float GLIDE_BODY_ROLL_FACTOR = 0.24F;
    private static final float GLIDE_BODY_ROLL_LIMIT = 0.18F;
    private static final float GLIDE_HEAD_PITCH = -0.16F;
    private static final float GLIDE_HIND_LEG = 0.55F;
    private static final float GLIDE_FRONT_LEG = -0.12F;
    private static final float GLIDE_LIMB_SWAY = 0.07F;
    private static final float GLIDE_ELYTRA_X = 0.34F;
    private static final float GLIDE_ELYTRA_Z = 1.38F;
    // 起飞姿态
    private static final float TAKEOFF_BODY_PITCH = 0.38F;
    private static final float TAKEOFF_PITCH_FACTOR = 0.08F;
    private static final float TAKEOFF_PITCH_LIMIT = 0.08F;
    private static final float TAKEOFF_ROLL_FACTOR = 0.12F;
    private static final float TAKEOFF_ROLL_LIMIT = 0.08F;
    private static final float TAKEOFF_HEAD_PITCH = -0.27F;
    private static final float TAKEOFF_HIND_LEG = 0.72F;
    private static final float TAKEOFF_FRONT_LEG = 0.46F;
    private static final float TAKEOFF_ELYTRA_X = -0.30F;
    private static final float TAKEOFF_ELYTRA_Z = 0.78F;
    // 毁灭打击专属蓄力与爆发起飞姿态
    private static final float DOOM_TAKEOFF_BURST_RISE_TICKS = 6.0F;
    private static final float DOOM_TAKEOFF_CHARGE_BODY_PITCH = -0.22F;
    private static final float DOOM_TAKEOFF_CHARGE_HEAD_PITCH = 0.20F;
    private static final float DOOM_TAKEOFF_BURST_BODY_PITCH = 1.10F;
    private static final float DOOM_TAKEOFF_END_BODY_PITCH = 0.20F;
    private static final float DOOM_TAKEOFF_BURST_HEAD_PITCH = -0.48F;
    private static final float DOOM_TAKEOFF_END_HEAD_PITCH = -0.14F;
    private static final float DOOM_TAKEOFF_CHARGE_LEG = 0.34F;
    private static final float DOOM_TAKEOFF_BURST_HIND_LEG = 0.82F;
    private static final float DOOM_TAKEOFF_BURST_FRONT_LEG = 0.55F;
    private static final float DOOM_TAKEOFF_BURST_ELYTRA_X = 0.28F;
    private static final float DOOM_TAKEOFF_BURST_ELYTRA_Z = 1.18F;
    // 降临俯冲姿态
    private static final float DESCEND_BODY_PITCH = 0.44F;
    private static final float DESCEND_PITCH_FACTOR = 0.16F;
    private static final float DESCEND_PITCH_LIMIT = 0.12F;
    private static final float DESCEND_ROLL_FACTOR = 0.18F;
    private static final float DESCEND_ROLL_LIMIT = 0.12F;
    private static final float DESCEND_HEAD_PITCH = -0.20F;
    private static final float DESCEND_HIND_LEG = 0.56F;
    private static final float DESCEND_FRONT_LEG = 0.22F;
    private static final float DESCEND_ELYTRA_X = 0.15F;
    private static final float DESCEND_ELYTRA_Z = 0.92F;
    // 毁灭打击高速俯冲姿态：相对水平约 54° 起步，并压到约 81°。
    private static final float DOOM_DIVE_BODY_PITCH_START = 0.95F;
    private static final float DOOM_DIVE_BODY_PITCH_FULL = 1.42F;
    private static final float DOOM_DIVE_HEAD_PITCH_START = 0.18F;
    private static final float DOOM_DIVE_HEAD_PITCH_FULL = 0.32F;
    private static final float DOOM_DIVE_HIND_LEG = 0.94F;
    private static final float DOOM_DIVE_FRONT_LEG = 0.68F;
    private static final float DOOM_DIVE_ELYTRA_X = 0.22F;
    private static final float DOOM_DIVE_ELYTRA_Z = 0.36F;
    private static final float DOOM_DIVE_ACCELERATION_TICKS = 18.0F;
    private static final float DOOM_DIVE_MAX_PITCH_RADIANS = 85.0F * DEG_TO_RAD;
    // 三连冲撞瞄准与冲击姿态
    private static final float COMBO_AIM_BODY_PITCH = 0.10F;
    private static final float COMBO_AIM_BODY_ROLL = 0.035F;
    private static final float COMBO_AIM_HEAD_PITCH = -0.12F;
    private static final float COMBO_AIM_HIND_LEG = 0.18F;
    private static final float COMBO_AIM_FRONT_LEG = -0.10F;
    private static final float COMBO_AIM_ELYTRA_X = 0.18F;
    private static final float COMBO_AIM_ELYTRA_Z = 0.58F;
    private static final float COMBO_CHARGE_BODY_PITCH = 0.34F;
    private static final float COMBO_CHARGE_PITCH_FACTOR = 0.10F;
    private static final float COMBO_CHARGE_PITCH_LIMIT = 0.08F;
    private static final float COMBO_CHARGE_BODY_ROLL = 0.07F;
    private static final float COMBO_CHARGE_HEAD_PITCH = -0.18F;
    private static final float COMBO_CHARGE_HIND_LEG = 0.78F;
    private static final float COMBO_CHARGE_FRONT_LEG = 0.70F;
    private static final float COMBO_CHARGE_ELYTRA_X = -0.08F;
    private static final float COMBO_CHARGE_ELYTRA_Z = 1.28F;
    // 地面逃离与收翼姿态
    private static final float RETREAT_BODY_PITCH = -0.08F;
    private static final float RETREAT_HEAD_PITCH = 0.02F;
    private static final float RETREAT_STRIDE = 0.75F;
    private static final float GROUND_ELYTRA_X = 0.26F;
    private static final float GROUND_ELYTRA_Z = 0.26F;

    private final PoseSample from = new PoseSample();
    private final PoseSample to = new PoseSample();

    private float bodyPitch;
    private float bodyRoll;
    private float headPitch;
    private float headRoll;
    private float rightHindLeg;
    private float leftHindLeg;
    private float rightFrontLeg;
    private float leftFrontLeg;
    private float elytraXRot;
    private float elytraZRot;

    void update(Qunyou entity, float partialTick, float ageInTicks,
            float baseHeadPitch, float baseRightHind, float baseLeftHind,
            float baseRightFront, float baseLeftFront) {
        byte state = entity.getState();
        int strike = entity.getPhase3ComboStrike();
        Vec3 motion = entity.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float speedFactor = (float) Mth.clamp(horizontalSpeed / 1.2D, 0.0D, 1.0D);
        float motionPitch = horizontalSpeed > MIN_MOTION_SPEED
                ? (float) Mth.atan2(-motion.y, Math.max(MIN_MOTION_SPEED, horizontalSpeed))
                : entity.getXRot() * DEG_TO_RAD;
        float yaw = entity.getYRot() * DEG_TO_RAD;
        float lateralSpeed = (float) (motion.x * Math.cos(yaw) - motion.z * Math.sin(yaw));

        byte previousState = entity.getPreviousState();
        float stateTicks = entity.getStateTicks(partialTick);
        float takeoffProgress = entity.getTakeoffProgress();
        boolean doomStrike = entity.isDoomStrikeActive();
        float previousTicks = previousState == Qunyou.TAKEOFF || previousState == Qunyou.DESCENDING_ATTACK
                ? 1000.0F : stateTicks;
        float previousTakeoffProgress = previousState == Qunyou.TAKEOFF ? 1.0F : takeoffProgress;
        sampleState(this.from, previousState, strike, ageInTicks, previousTicks, previousTakeoffProgress,
                doomStrike, motionPitch, lateralSpeed, speedFactor, baseHeadPitch,
                baseRightHind, baseLeftHind, baseRightFront, baseLeftFront);
        sampleState(this.to, state, strike, ageInTicks, stateTicks, takeoffProgress,
                doomStrike, motionPitch, lateralSpeed, speedFactor, baseHeadPitch,
                baseRightHind, baseLeftHind, baseRightFront, baseLeftFront);

        float progress = Mth.clamp(stateTicks / TRANSITION_TICKS, 0.0F, 1.0F);
        float blend = progress * progress * (3.0F - 2.0F * progress);
        this.bodyPitch = lerpRadians(blend, this.from.bodyPitch, this.to.bodyPitch);
        this.bodyRoll = lerpRadians(blend, this.from.bodyRoll, this.to.bodyRoll);
        this.headPitch = lerpRadians(blend, this.from.headPitch, this.to.headPitch);
        this.headRoll = lerpRadians(blend, this.from.headRoll, this.to.headRoll);
        this.rightHindLeg = Mth.lerp(blend, this.from.rightHindLeg, this.to.rightHindLeg);
        this.leftHindLeg = Mth.lerp(blend, this.from.leftHindLeg, this.to.leftHindLeg);
        this.rightFrontLeg = Mth.lerp(blend, this.from.rightFrontLeg, this.to.rightFrontLeg);
        this.leftFrontLeg = Mth.lerp(blend, this.from.leftFrontLeg, this.to.leftFrontLeg);
        this.elytraXRot = lerpRadians(blend, this.from.elytraXRot, this.to.elytraXRot);
        this.elytraZRot = lerpRadians(blend, this.from.elytraZRot, this.to.elytraZRot);
    }

    private static float lerpRadians(float delta, float from, float to) {
        return Mth.rotLerp(delta, from * RAD_TO_DEG, to * RAD_TO_DEG) * DEG_TO_RAD;
    }

    private static void sampleState(PoseSample out, byte state, int strike, float ageInTicks,
            float stateTicks, float takeoffProgress, boolean doomStrike, float motionPitch,
            float lateralSpeed, float speedFactor, float baseHeadPitch, float baseRightHind,
            float baseLeftHind, float baseRightFront, float baseLeftFront) {
        ground(out, baseHeadPitch, baseRightHind, baseLeftHind, baseRightFront, baseLeftFront);
        switch (state) {
            case Qunyou.ASCENDING -> {
                out.bodyPitch = ASCEND_BODY_PITCH;
                out.bodyRoll = Mth.sin(ageInTicks * 0.12F) * ASCEND_BODY_ROLL;
                out.headPitch = ASCEND_HEAD_PITCH;
                float swing = Mth.sin(ageInTicks * 0.55F) * ASCEND_LEG_SWING;
                out.rightHindLeg = ASCEND_LEG_BASE + swing;
                out.leftHindLeg = ASCEND_LEG_BASE - swing;
                out.rightFrontLeg = ASCEND_FRONT_LEG_BASE - swing;
                out.leftFrontLeg = ASCEND_FRONT_LEG_BASE + swing;
                out.elytraXRot = ASCEND_ELYTRA_X;
                out.elytraZRot = ASCEND_ELYTRA_Z;
            }
            case Qunyou.GLIDING -> {
                out.bodyPitch = Mth.clamp(motionPitch * GLIDE_BODY_PITCH_FACTOR,
                        -GLIDE_BODY_PITCH_LIMIT, GLIDE_BODY_PITCH_LIMIT);
                out.bodyRoll = Mth.clamp(-lateralSpeed * GLIDE_BODY_ROLL_FACTOR,
                        -GLIDE_BODY_ROLL_LIMIT, GLIDE_BODY_ROLL_LIMIT);
                out.headPitch = GLIDE_HEAD_PITCH;
                float sway = Mth.sin(ageInTicks * 0.32F) * GLIDE_LIMB_SWAY * speedFactor;
                out.rightHindLeg = GLIDE_HIND_LEG + sway;
                out.leftHindLeg = GLIDE_HIND_LEG - sway;
                out.rightFrontLeg = GLIDE_FRONT_LEG - sway;
                out.leftFrontLeg = GLIDE_FRONT_LEG + sway;
                out.elytraXRot = GLIDE_ELYTRA_X;
                out.elytraZRot = GLIDE_ELYTRA_Z;
            }
            case Qunyou.TAKEOFF -> {
                if (doomStrike) {
                    if (stateTicks < Qunyou.DOOM_STRIKE_TAKEOFF_CHARGE_TICKS) {
                        float charge = smoothStep(Mth.clamp(stateTicks / Qunyou.DOOM_STRIKE_TAKEOFF_CHARGE_TICKS, 0.0F, 1.0F));
                        out.bodyPitch = Mth.lerp(charge, 0.0F, DOOM_TAKEOFF_CHARGE_BODY_PITCH)
                                + Mth.sin(ageInTicks * 2.5F) * 0.018F * charge;
                        out.headPitch = Mth.lerp(charge, 0.0F, DOOM_TAKEOFF_CHARGE_HEAD_PITCH);
                        out.rightHindLeg = Mth.lerp(charge, 0.0F, DOOM_TAKEOFF_CHARGE_LEG);
                        out.leftHindLeg = out.rightHindLeg;
                        out.rightFrontLeg = Mth.lerp(charge, 0.0F, DOOM_TAKEOFF_CHARGE_LEG * 0.82F);
                        out.leftFrontLeg = out.rightFrontLeg;
                        out.elytraXRot = GROUND_ELYTRA_X;
                        out.elytraZRot = GROUND_ELYTRA_Z;
                    } else {
                        float burst = smoothStep(Mth.clamp((stateTicks - Qunyou.DOOM_STRIKE_TAKEOFF_CHARGE_TICKS)
                                / DOOM_TAKEOFF_BURST_RISE_TICKS, 0.0F, 1.0F));
                        float ascent = smoothStep(Mth.clamp(takeoffProgress, 0.0F, 1.0F));
                        float ascentBodyPitch = Mth.lerp(ascent,
                                DOOM_TAKEOFF_BURST_BODY_PITCH, DOOM_TAKEOFF_END_BODY_PITCH);
                        float ascentHeadPitch = Mth.lerp(ascent,
                                DOOM_TAKEOFF_BURST_HEAD_PITCH, DOOM_TAKEOFF_END_HEAD_PITCH);
                        out.bodyPitch = Mth.lerp(burst, DOOM_TAKEOFF_CHARGE_BODY_PITCH, ascentBodyPitch);
                        out.bodyRoll = Mth.clamp(-lateralSpeed * 0.10F, -0.07F, 0.07F);
                        out.headPitch = Mth.lerp(burst, DOOM_TAKEOFF_CHARGE_HEAD_PITCH, ascentHeadPitch);
                        float hindTarget = Mth.lerp(ascent,
                                DOOM_TAKEOFF_BURST_HIND_LEG, GLIDE_HIND_LEG);
                        float frontTarget = Mth.lerp(ascent,
                                DOOM_TAKEOFF_BURST_FRONT_LEG, GLIDE_FRONT_LEG);
                        out.rightHindLeg = Mth.lerp(burst, DOOM_TAKEOFF_CHARGE_LEG, hindTarget);
                        out.leftHindLeg = out.rightHindLeg;
                        out.rightFrontLeg = Mth.lerp(burst, DOOM_TAKEOFF_CHARGE_LEG * 0.82F, frontTarget);
                        out.leftFrontLeg = out.rightFrontLeg;
                        out.elytraXRot = Mth.lerp(burst, GROUND_ELYTRA_X,
                                Mth.lerp(ascent, DOOM_TAKEOFF_BURST_ELYTRA_X, GLIDE_ELYTRA_X));
                        out.elytraZRot = Mth.lerp(burst, GROUND_ELYTRA_Z,
                                Mth.lerp(ascent, DOOM_TAKEOFF_BURST_ELYTRA_Z, GLIDE_ELYTRA_Z));
                    }
                } else {
                    out.bodyPitch = TAKEOFF_BODY_PITCH + Mth.clamp(motionPitch * TAKEOFF_PITCH_FACTOR,
                            -TAKEOFF_PITCH_LIMIT, TAKEOFF_PITCH_LIMIT);
                    out.bodyRoll = Mth.clamp(-lateralSpeed * TAKEOFF_ROLL_FACTOR,
                            -TAKEOFF_ROLL_LIMIT, TAKEOFF_ROLL_LIMIT);
                    out.headPitch = TAKEOFF_HEAD_PITCH;
                    out.rightHindLeg = TAKEOFF_HIND_LEG;
                    out.leftHindLeg = TAKEOFF_HIND_LEG;
                    out.rightFrontLeg = TAKEOFF_FRONT_LEG;
                    out.leftFrontLeg = TAKEOFF_FRONT_LEG;
                    out.elytraXRot = TAKEOFF_ELYTRA_X;
                    out.elytraZRot = TAKEOFF_ELYTRA_Z;
                }
            }
            case Qunyou.DESCENDING_ATTACK -> {
                if (doomStrike) {
                    float acceleration = Mth.clamp(stateTicks / DOOM_DIVE_ACCELERATION_TICKS, 0.0F, 1.0F);
                    float pitchFromMotion = Mth.clamp(Math.max(0.0F, motionPitch)
                            / DOOM_DIVE_MAX_PITCH_RADIANS, 0.0F, 1.0F);
                    float diveAmount = Mth.clamp(Math.max(acceleration * 0.72F, pitchFromMotion), 0.0F, 1.0F);
                    out.bodyPitch = Mth.lerp(diveAmount,
                            DOOM_DIVE_BODY_PITCH_START, DOOM_DIVE_BODY_PITCH_FULL);
                    out.bodyRoll = Mth.clamp(-lateralSpeed * 0.10F, -0.07F, 0.07F);
                    out.headPitch = Mth.lerp(diveAmount,
                            DOOM_DIVE_HEAD_PITCH_START, DOOM_DIVE_HEAD_PITCH_FULL);
                    out.rightHindLeg = DOOM_DIVE_HIND_LEG;
                    out.leftHindLeg = DOOM_DIVE_HIND_LEG;
                    out.rightFrontLeg = DOOM_DIVE_FRONT_LEG;
                    out.leftFrontLeg = DOOM_DIVE_FRONT_LEG;
                    out.elytraXRot = DOOM_DIVE_ELYTRA_X;
                    out.elytraZRot = DOOM_DIVE_ELYTRA_Z;
                } else {
                    out.bodyPitch = DESCEND_BODY_PITCH + Mth.clamp(motionPitch * DESCEND_PITCH_FACTOR,
                            -DESCEND_PITCH_LIMIT, DESCEND_PITCH_LIMIT);
                    out.bodyRoll = Mth.clamp(-lateralSpeed * DESCEND_ROLL_FACTOR,
                            -DESCEND_ROLL_LIMIT, DESCEND_ROLL_LIMIT);
                    out.headPitch = DESCEND_HEAD_PITCH;
                    out.rightHindLeg = DESCEND_HIND_LEG;
                    out.leftHindLeg = DESCEND_HIND_LEG;
                    out.rightFrontLeg = DESCEND_FRONT_LEG;
                    out.leftFrontLeg = DESCEND_FRONT_LEG;
                    out.elytraXRot = DESCEND_ELYTRA_X;
                    out.elytraZRot = DESCEND_ELYTRA_Z;
                }
            }
            case Qunyou.PHASE3_PIG_RETREAT_CHARGE -> {
                out.bodyPitch = -0.18F;
                out.headPitch = 0.16F;
                out.rightHindLeg = 0.40F;
                out.leftHindLeg = 0.40F;
                out.rightFrontLeg = 0.32F;
                out.leftFrontLeg = 0.32F;
                out.elytraXRot = GROUND_ELYTRA_X;
                out.elytraZRot = GROUND_ELYTRA_Z;
            }
            case Qunyou.PHASE3_PIG_RETREAT_HORIZONTAL -> {
                out.bodyPitch = 0.55F;
                out.headPitch = -0.22F;
                out.rightHindLeg = 0.82F;
                out.leftHindLeg = 0.82F;
                out.rightFrontLeg = 0.68F;
                out.leftFrontLeg = 0.68F;
                out.elytraXRot = -0.04F;
                out.elytraZRot = 1.15F;
            }
            case Qunyou.PHASE3_PIG_RETREAT_ASCEND -> {
                out.bodyPitch = 0.65F;
                out.headPitch = -0.28F;
                out.rightHindLeg = 0.70F;
                out.leftHindLeg = 0.70F;
                out.rightFrontLeg = 0.42F;
                out.leftFrontLeg = 0.42F;
                out.elytraXRot = 0.26F;
                out.elytraZRot = 1.30F;
            }
            case Qunyou.PHASE3_AIR_MANEUVER -> {
                out.bodyPitch = 0.40F;
                out.headPitch = -0.16F;
                out.rightHindLeg = 0.78F;
                out.leftHindLeg = 0.78F;
                out.rightFrontLeg = 0.58F;
                out.leftFrontLeg = 0.58F;
                out.elytraXRot = 0.12F;
                out.elytraZRot = 0.96F;
            }
            case Qunyou.PHASE3_EJECTION_CHARGE -> {
                out.bodyPitch = -0.24F;
                out.headPitch = 0.20F;
                out.rightHindLeg = 0.42F;
                out.leftHindLeg = 0.42F;
                out.rightFrontLeg = 0.34F;
                out.leftFrontLeg = 0.34F;
                out.elytraXRot = GROUND_ELYTRA_X;
                out.elytraZRot = GROUND_ELYTRA_Z;
            }
            case Qunyou.PHASE3_EJECTION_RUSH -> {
                out.bodyPitch = 0.58F;
                out.bodyRoll = Mth.clamp(-lateralSpeed * 0.10F, -0.07F, 0.07F);
                out.headPitch = -0.24F;
                out.rightHindLeg = 0.86F;
                out.leftHindLeg = 0.86F;
                out.rightFrontLeg = 0.72F;
                out.leftFrontLeg = 0.72F;
                out.elytraXRot = -0.05F;
                out.elytraZRot = 1.18F;
            }
            case Qunyou.PHASE3_EJECTION_RECOVER -> {
                out.bodyPitch = 0.06F;
                out.headPitch = -0.10F;
                out.rightHindLeg = 0.30F;
                out.leftHindLeg = 0.30F;
                out.rightFrontLeg = 0.10F;
                out.leftFrontLeg = 0.10F;
                out.elytraXRot = 0.20F;
                out.elytraZRot = 0.72F;
            }
            case Qunyou.PHASE3_EJECTION_ASCEND -> {
                out.bodyPitch = 0.62F;
                out.headPitch = -0.28F;
                out.rightHindLeg = 0.72F;
                out.leftHindLeg = 0.72F;
                out.rightFrontLeg = 0.44F;
                out.leftFrontLeg = 0.44F;
                out.elytraXRot = 0.26F;
                out.elytraZRot = 1.30F;
            }
            case Qunyou.PHASE3_COMBO_AIM -> {
                out.bodyPitch = COMBO_AIM_BODY_PITCH;
                out.bodyRoll = (strike % 2 == 0 ? 1.0F : -1.0F) * COMBO_AIM_BODY_ROLL;
                out.headPitch = COMBO_AIM_HEAD_PITCH;
                out.rightHindLeg = COMBO_AIM_HIND_LEG;
                out.leftHindLeg = COMBO_AIM_HIND_LEG;
                out.rightFrontLeg = COMBO_AIM_FRONT_LEG;
                out.leftFrontLeg = COMBO_AIM_FRONT_LEG;
                out.elytraXRot = COMBO_AIM_ELYTRA_X;
                out.elytraZRot = COMBO_AIM_ELYTRA_Z;
            }
            case Qunyou.PHASE3_COMBO_CHARGE -> {
                out.bodyPitch = COMBO_CHARGE_BODY_PITCH + Mth.clamp(motionPitch * COMBO_CHARGE_PITCH_FACTOR,
                        -COMBO_CHARGE_PITCH_LIMIT, COMBO_CHARGE_PITCH_LIMIT);
                out.bodyRoll = (strike % 2 == 0 ? 1.0F : -1.0F) * COMBO_CHARGE_BODY_ROLL;
                out.headPitch = COMBO_CHARGE_HEAD_PITCH;
                out.rightHindLeg = COMBO_CHARGE_HIND_LEG;
                out.leftHindLeg = COMBO_CHARGE_HIND_LEG;
                out.rightFrontLeg = COMBO_CHARGE_FRONT_LEG;
                out.leftFrontLeg = COMBO_CHARGE_FRONT_LEG;
                out.elytraXRot = COMBO_CHARGE_ELYTRA_X;
                out.elytraZRot = COMBO_CHARGE_ELYTRA_Z;
            }
            case Qunyou.SUMMON -> {
                // 第三阶段继续复用二阶段胡萝卜射击的轻微蓄力摆动。
                out.bodyPitch = Mth.sin(ageInTicks * 0.20F) * 0.035F;
                out.headPitch = Mth.sin(ageInTicks * 0.16F) * 0.08F;
            }
            case Qunyou.RETREAT -> {
                float stride = Mth.sin(ageInTicks * 1.7F) * RETREAT_STRIDE;
                out.bodyPitch = RETREAT_BODY_PITCH;
                out.headPitch = RETREAT_HEAD_PITCH;
                out.rightHindLeg = stride;
                out.leftHindLeg = -stride;
                out.rightFrontLeg = -stride;
                out.leftFrontLeg = stride;
                out.elytraXRot = GROUND_ELYTRA_X;
                out.elytraZRot = GROUND_ELYTRA_Z;
            }
            default -> {
                // PHASE3_GROUND_WAIT、IDLE 等地面状态恢复原版猪的自然支撑姿态。
            }
        }
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void ground(PoseSample out, float baseHeadPitch, float baseRightHind, float baseLeftHind,
            float baseRightFront, float baseLeftFront) {
        out.bodyPitch = 0.0F;
        out.bodyRoll = 0.0F;
        out.headPitch = 0.0F;
        out.headRoll = 0.0F;
        out.rightHindLeg = baseRightHind;
        out.leftHindLeg = baseLeftHind;
        out.rightFrontLeg = baseRightFront;
        out.leftFrontLeg = baseLeftFront;
        out.elytraXRot = GROUND_ELYTRA_X;
        out.elytraZRot = GROUND_ELYTRA_Z;
    }

    float getBodyPitch() {
        return this.bodyPitch;
    }

    float getBodyRoll() {
        return this.bodyRoll;
    }

    float getHeadPitch() {
        return this.headPitch;
    }

    float getHeadRoll() {
        return this.headRoll;
    }

    float getRightHindLeg() {
        return this.rightHindLeg;
    }

    float getLeftHindLeg() {
        return this.leftHindLeg;
    }

    float getRightFrontLeg() {
        return this.rightFrontLeg;
    }

    float getLeftFrontLeg() {
        return this.leftFrontLeg;
    }

    float getElytraXRot() {
        return this.elytraXRot;
    }

    float getElytraZRot() {
        return this.elytraZRot;
    }

    private static final class PoseSample {
        private float bodyPitch;
        private float bodyRoll;
        private float headPitch;
        private float headRoll;
        private float rightHindLeg;
        private float leftHindLeg;
        private float rightFrontLeg;
        private float leftFrontLeg;
        private float elytraXRot;
        private float elytraZRot;
    }
}
