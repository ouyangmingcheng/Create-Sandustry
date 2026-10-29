package com.minechai.create_sandustry.content.client.event;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.network.SwordPullCancelPayload;
import com.minechai.create_sandustry.network.SwordPullStatePayload;
import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/** 客户端对服务端拔剑会话的右键按住检测；只负责动画反馈与发送取消/松手意图。 */
@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class SwordPullClient {
    private static BlockPos targetPos;
    private static ResourceKey<Level> targetDimension;
    private static boolean releaseLockPending;
    private static BlockPos releaseLockPos;
    private static final Vector3f PULL_DUST_COLOR = new Vector3f(0.88F, 0.76F, 0.45F);

    private SwordPullClient() {
    }

    public static void handleState(SwordPullStatePayload payload) {
        if (!payload.active()) {
            clearTarget();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.options.keyUse.isDown()) {
                // 成功拔剑时玩家通常仍按着右键；保留一个松手检测，服务端据此解除交互锁。
                releaseLockPending = true;
                releaseLockPos = payload.pos().immutable();
            } else {
                PacketDistributor.sendToServer(new SwordPullCancelPayload(payload.pos()));
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        targetPos = payload.pos().immutable();
        targetDimension = minecraft.level.dimension();
        releaseLockPending = false;
        releaseLockPos = null;
    }

    /** 每次拔剑扣心由独立包触发，确保最后一次与成功/死亡同 tick 时也不会丢反馈。 */
    public static void handleHurt() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // 只播放受伤表现，不修改生命值。
        minecraft.player.animateHurt(0.0F);
        minecraft.player.walkAnimation.setSpeed(1.5F);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (releaseLockPending && (minecraft.player == null || !minecraft.options.keyUse.isDown())) {
            if (releaseLockPos != null) {
                PacketDistributor.sendToServer(new SwordPullCancelPayload(releaseLockPos));
            }
            releaseLockPending = false;
            releaseLockPos = null;
        }
        if (targetPos == null) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.level.dimension().equals(targetDimension)
                || !minecraft.player.isAlive()
                || !minecraft.options.keyUse.isDown()
                || minecraft.player.distanceToSqr(Vec3.atCenterOf(targetPos)) > 36.0D
                || !(minecraft.hitResult instanceof BlockHitResult blockHit)
                || !blockHit.getBlockPos().equals(targetPos)) {
            cancel();
            return;
        }

        int progress = minecraft.player.getData(ModAttachments.SWORD_PULL_PROGRESS);
        spawnPullingParticles(minecraft, progress);
        // 清除第一次右键留下的残余挥臂动画，持续姿态由 HumanoidModelMixin 固定。
        minecraft.player.swingTime = 0;
        minecraft.player.swinging = false;
    }

    /** 客户端逐 tick 生成，使用显式速度让粒子从上表面中心向上、向外扩散。 */
    private static void spawnPullingParticles(Minecraft minecraft, int progress) {
        if (minecraft.level == null || targetPos == null) {
            return;
        }
        int count = 1 + progress / 2;
        double centerX = targetPos.getX() + 0.5D;
        double surfaceY = targetPos.getY() + 0.36D;
        double centerZ = targetPos.getZ() + 0.5D;
        double outwardSpeed = 0.050D + progress * 0.004D;
        double upwardSpeed = outwardSpeed * Math.tan(Math.toRadians(20.0D));

        for (int i = 0; i < count; i++) {
            double angle = minecraft.level.random.nextDouble() * Math.PI * 2.0D;
            double startRadius = 0.02D + minecraft.level.random.nextDouble() * 0.04D;
            double vx = Math.cos(angle) * outwardSpeed;
            double vz = Math.sin(angle) * outwardSpeed;
            double x = centerX + Math.cos(angle) * startRadius;
            double z = centerZ + Math.sin(angle) * startRadius;

            minecraft.level.addParticle(new DustParticleOptions(PULL_DUST_COLOR, 0.9F),
                    x, surfaceY, z, vx, upwardSpeed, vz);
            minecraft.level.addParticle(new DustParticleOptions(PULL_DUST_COLOR, 0.2F),
                    x, surfaceY + 0.05D, z, vx, upwardSpeed, vz);
        }
    }
    public static boolean isReleaseLockPending() {
        return releaseLockPending;
    }
    private static void cancel() {
        BlockPos pos = targetPos;
        clearTarget();
        if (pos != null) {
            PacketDistributor.sendToServer(new SwordPullCancelPayload(pos));
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.keyUse.isDown()) {
                releaseLockPending = true;
                releaseLockPos = pos;
            }
        }
    }

    private static void clearTarget() {
        targetPos = null;
        targetDimension = null;
    }
}
