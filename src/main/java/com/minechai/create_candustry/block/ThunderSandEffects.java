package com.minechai.create_candustry.block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ThunderSandEffects {
    public static final String EFFECT_ID = CreateSandustry.MODID + ":thunder_lightning";

    private static final int STRIKE_DELAY_MIN = 6;
    private static final int STRIKE_DELAY_RANDOM = 7;
    private static final double STRIKE_OFFSET_RADIUS = 2.0D;
    private static final List<PendingStrike> PENDING_STRIKES = new ArrayList<>();

    public static void scheduleStrikes(ServerLevel level, Vec3 origin, int strikeCount) {
        if (strikeCount <= 0) {
            return;
        }
        PENDING_STRIKES.add(new PendingStrike(level, origin, strikeCount,
                level.getServer().getTickCount() + nextDelay(level)));
    }

    public static void triggerSingleStrike(ServerLevel level, BlockPos pos) {
        spawnStrike(level, Vec3.atCenterOf(pos), false);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        List<PendingStrike> nextStrikes = new ArrayList<>();
        Iterator<PendingStrike> iterator = PENDING_STRIKES.iterator();

        while (iterator.hasNext()) {
            PendingStrike pending = iterator.next();
            if (pending.level().getServer() != event.getServer()) {
                iterator.remove();
                continue;
            }

            if (pending.strikeTick() <= now) {
                iterator.remove();
                spawnStrike(pending.level(), pending.origin(), true);

                if (pending.remaining() > 1) {
                    nextStrikes.add(new PendingStrike(
                            pending.level(),
                            pending.origin(),
                            pending.remaining() - 1,
                            now + nextDelay(pending.level())));
                }
            }
        }

        PENDING_STRIKES.addAll(nextStrikes);
    }

    private static int nextDelay(ServerLevel level) {
        return STRIKE_DELAY_MIN + level.random.nextInt(STRIKE_DELAY_RANDOM);
    }

    private static void spawnStrike(ServerLevel level, Vec3 origin, boolean randomizeOffset) {
        BlockPos intendedStrikePos = randomizeOffset
                ? intendedStrikePosition(level, origin)
                : BlockPos.containing(origin.x, origin.y, origin.z);
        BlockPos strikePos = findLightningRod(level, intendedStrikePos)
                .map(BlockPos::above)
                .orElse(intendedStrikePos);

        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(level);
        if (lightningBolt == null) {
            return;
        }

        lightningBolt.moveTo(Vec3.atBottomCenterOf(strikePos));
        level.addFreshEntity(lightningBolt);
    }

    private static BlockPos intendedStrikePosition(ServerLevel level, Vec3 origin) {
        double offsetX = (level.random.nextDouble() * 2.0D - 1.0D) * STRIKE_OFFSET_RADIUS;
        double offsetZ = (level.random.nextDouble() * 2.0D - 1.0D) * STRIKE_OFFSET_RADIUS;
        return BlockPos.containing(origin.x + offsetX, origin.y, origin.z + offsetZ);
    }

    private static Optional<BlockPos> findLightningRod(ServerLevel level, BlockPos center) {
        int rodRange = ModConfigs.THUNDER_SAND_LIGHTNING_ROD_RANGE.get();
        int rodRangeSquared = rodRange * rodRange;
        BlockPos min = center.offset(-rodRange, -rodRange, -rodRange);
        BlockPos max = center.offset(rodRange, rodRange, rodRange);
        BlockPos nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
            if (!(level.getBlockState(candidate).getBlock() instanceof LightningRodBlock)) {
                continue;
            }

            double distanceSquared = candidate.distSqr(center);
            if (distanceSquared <= rodRangeSquared && distanceSquared < nearestDistanceSquared) {
                nearest = candidate.immutable();
                nearestDistanceSquared = distanceSquared;
            }
        }

        return Optional.ofNullable(nearest);
    }

    private record PendingStrike(ServerLevel level, Vec3 origin, int remaining, int strikeTick) {
    }

    private ThunderSandEffects() {
    }
}