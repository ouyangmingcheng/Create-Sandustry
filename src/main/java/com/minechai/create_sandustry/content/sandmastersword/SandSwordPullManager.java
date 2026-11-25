package com.minechai.create_sandustry.content.sandmastersword;

import com.minechai.create_sandustry.content.sandheart.SandHeartManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordPedestalBlockEntity;
import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.network.SwordPullHurtPayload;
import com.minechai.create_sandustry.network.SwordPullStatePayload;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * 无主沙噬剑的长按右键拔剑会话。
 *
 * <p>拔剑进度只控制 HUD 预览，不修改真实沙心。成功时一次性写入 12 点永久生命上限惩罚；
 * 松手、离开或剑座失效时只清空预览，不产生任何永久代价。</p>
 */
public final class SandSwordPullManager {
    public static final int TICKS_PER_HEART = 40;
    public static final int HEARTS_REQUIRED = 6;
    private static final double MAX_DISTANCE_SQR = 36.0D;
    private static final Vector3f PULL_DUST_COLOR = new Vector3f(0.88F, 0.76F, 0.45F);

    private static final ResourceKey<DamageType> OVERREACH_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sand_sword_pull_overreach"));

    private static final Map<UUID, PullSession> SESSIONS = new HashMap<>();
    /** 成功拔剑后等待玩家松开右键的交互锁，避免按住右键立刻把剑插回剑座。 */
    private static final Set<UUID> INPUT_LOCKS = new HashSet<>();

    private SandSwordPullManager() {
    }

    /** 尝试开始拔剑；同一剑座的重复右键不会重置进度。 */
    public static boolean tryStart(ServerPlayer player, BlockPos pos, SandMasterSwordPedestalBlockEntity pedestal) {
        if (!pedestal.isUnowned() || !player.isAlive() || player.isSpectator()) {
            return false;
        }

        ResourceKey<Level> dimension = player.serverLevel().dimension();
        PullSession existing = SESSIONS.get(player.getUUID());
        if (existing != null && existing.dimension.equals(dimension) && existing.pos.equals(pos)) {
            return true;
        }
        if (existing != null) {
            finish(player, existing, false);
        }

        if (!player.hasEffect(ModMobEffects.SAND_HARDCORE) || !SandHeartManager.isFilled(player)) {
            showImmovable(player);
            return false;
        }

        int sandHearts = Mth.ceil(SandHeartManager.getUnits(player)
                / (double) SandHeartManager.UNITS_PER_HEART);
        if (sandHearts <= 0) {
            return false;
        }

        long now = player.serverLevel().getGameTime();
        PullSession session = new PullSession(dimension, pos.immutable(), sandHearts, now + TICKS_PER_HEART);
        SESSIONS.put(player.getUUID(), session);
        player.setData(ModAttachments.SWORD_PULL_ACTIVE, true);
        player.setData(ModAttachments.SWORD_PULL_PROGRESS, 0);
        PacketDistributor.sendToPlayer(player, new SwordPullStatePayload(pos, true));
        return true;
    }

    /** 客户端检测到松开右键或离开剑座。 */
    public static void cancelFromClient(ServerPlayer player, BlockPos pos) {
        INPUT_LOCKS.remove(player.getUUID());
        PullSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.pos.equals(pos)) {
            return;
        }
        finish(player, session, true);
    }

    /** 剑座被移除或状态失效时取消所有指向它的会话。 */
    public static void cancelForPedestal(ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        for (UUID uuid : SESSIONS.keySet().toArray(UUID[]::new)) {
            PullSession session = SESSIONS.get(uuid);
            if (session == null || !session.dimension.equals(level.dimension()) || !session.pos.equals(pos)) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                SESSIONS.remove(uuid);
            } else {
                finish(player, session, true);
            }
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PullSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!isStillValid(player, session)) {
            finish(player, session, true);
            return;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        while (now >= session.nextPullTick && SESSIONS.get(player.getUUID()) == session) {
            session.nextPullTick += TICKS_PER_HEART;
            session.progress++;
            player.setData(ModAttachments.SWORD_PULL_PROGRESS, session.progress);
            level.playSound(null, session.pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    0.65F, 0.72F + session.progress * 0.045F);
            player.playSound(SoundEvents.PLAYER_HURT, 0.75F, 0.9F + session.progress * 0.03F);
            PacketDistributor.sendToPlayer(player, new SwordPullHurtPayload());
            spawnHeartLossBurst(level, session);

            // 对小于等于 6 颗心的玩家，第六次预览扣心会先清空沙心，必须在成功判定前死亡。
            if (session.progress >= session.availableSandHearts) {
                killForOverreach(player, session);
                return;
            }
            if (session.progress >= HEARTS_REQUIRED) {
                succeed(player, session);
                return;
            }
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        INPUT_LOCKS.remove(player.getUUID());
        PullSession session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            player.setData(ModAttachments.SWORD_PULL_ACTIVE, false);
            player.setData(ModAttachments.SWORD_PULL_PROGRESS, 0);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SESSIONS.remove(player.getUUID());
        INPUT_LOCKS.remove(player.getUUID());
        player.setData(ModAttachments.SWORD_PULL_ACTIVE, false);
        player.setData(ModAttachments.SWORD_PULL_PROGRESS, 0);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        INPUT_LOCKS.clear();
        SESSIONS.clear();
    }

    private static boolean isStillValid(ServerPlayer player, PullSession session) {
        if (!player.isAlive() || player.isSpectator()
                || !player.serverLevel().dimension().equals(session.dimension)
                || player.distanceToSqr(Vec3.atCenterOf(session.pos)) > MAX_DISTANCE_SQR) {
            return false;
        }
        HitResult hit = player.pick(6.0D, 0.0F, false);
        return hit instanceof BlockHitResult blockHit
                && blockHit.getBlockPos().equals(session.pos)
                && player.serverLevel().getBlockEntity(session.pos)
                instanceof SandMasterSwordPedestalBlockEntity pedestal
                && pedestal.isUnowned();
    }

    private static void succeed(ServerPlayer player, PullSession session) {
        ServerLevel level = player.serverLevel();
        if (!(level.getBlockEntity(session.pos) instanceof SandMasterSwordPedestalBlockEntity pedestal)) {
            finish(player, session, true);
            return;
        }
        ItemStack sword = pedestal.completePull();
        if (sword.isEmpty()) {
            finish(player, session, true);
            return;
        }

        sword.set(ModDataComponents.SAND_MASTER_SWORD_OWNER.get(), player.getUUID());
        if (!player.getInventory().add(sword)) {
            player.drop(sword, false);
        }

        INPUT_LOCKS.add(player.getUUID());
        SandSwordHealthPenalty.applySuccessfulPullPenalty(player);
        // 永久上限变化后让沙心数量按新容量重新夹取；这不是第二次扣除。
        SandHeartManager.setUnits(player, SandHeartManager.getUnits(player));
        finish(player, session, false);
        player.displayClientMessage(Component.translatable("message.createsandustry.sword_pull.success")
                .withStyle(ChatFormatting.GOLD), true);
    }

    /** 每次实际扣心时额外爆发一圈粒子，强化进度推进反馈。 */
    private static void spawnHeartLossBurst(ServerLevel level, PullSession session) {
        int count = 8 + session.progress * 3;
        double radius = 0.24D + session.progress * 0.035D;
        level.sendParticles(new DustParticleOptions(PULL_DUST_COLOR, 0.65F),
                session.pos.getX() + 0.5D,
                session.pos.getY() + 0.62D,
                session.pos.getZ() + 0.5D,
                count, radius, 0.32D, radius, 0.025D);
    }
    public static boolean isInteractionLocked(Player player) {
        return INPUT_LOCKS.contains(player.getUUID());
    }

    private static void showImmovable(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.createsandustry.sword_pull.immovable")
                .withStyle(ChatFormatting.YELLOW), true);
    }
    private static void killForOverreach(ServerPlayer player, PullSession session) {
        finish(player, session, false);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().source(OVERREACH_DAMAGE), Float.MAX_VALUE);
    }

    /** 清除会话与 HUD 预览；拔剑过程中从未修改真实沙心，因此无需恢复沙心数据。 */
    private static void finish(ServerPlayer player, PullSession session, boolean showFailure) {
        if (SESSIONS.remove(player.getUUID()) == null) {
            return;
        }
        player.setData(ModAttachments.SWORD_PULL_ACTIVE, false);
        player.setData(ModAttachments.SWORD_PULL_PROGRESS, 0);
        PacketDistributor.sendToPlayer(player, new SwordPullStatePayload(session.pos, false));
        if (showFailure && player.isAlive()) {
            player.displayClientMessage(Component.translatable("message.createsandustry.sword_pull.failed"),
                    true);
        }
    }

    private static final class PullSession {
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final int availableSandHearts;
        private long nextPullTick;
        private int progress;

        private PullSession(ResourceKey<Level> dimension, BlockPos pos, int availableSandHearts, long nextPullTick) {
            this.dimension = dimension;
            this.pos = pos;
            this.availableSandHearts = availableSandHearts;
            this.nextPullTick = nextPullTick;
        }
    }
}
