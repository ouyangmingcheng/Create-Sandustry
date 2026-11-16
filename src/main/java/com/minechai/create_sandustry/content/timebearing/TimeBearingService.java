package com.minechai.create_sandustry.content.timebearing;

import java.util.Map;
import java.util.WeakHashMap;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * 时钟轴承时间锁的服务端入口。运行时状态按 ServerLevel 弱引用保存，避免切换存档后泄漏。
 */
public final class TimeBearingService {
    private static final Map<ServerLevel, WorldTimeController> CONTROLLERS = new WeakHashMap<>();
    private static final long LET_VANILLA_HANDLE_TIME = -1L;

    private TimeBearingService() {
    }

    public static void onClockworkBearingTick(
            ClockworkBearingBlockEntity bearing,
            boolean assembled,
            boolean stalled,
            float networkCapacity) {
        if (!(bearing.getLevel() instanceof ServerLevel level)) {
            return;
        }
        controller(level).onClockworkBearingTick(bearing, assembled, stalled, networkCapacity);
    }

    public static void onBearingRemoved(ServerLevel level, BlockPos pos) {
        WorldTimeController controller = peek(level);
        if (controller != null) {
            controller.onControllerRemoved(pos);
        }
    }

    public static void onChunkLoaded(ServerLevel level, ChunkPos chunkPos) {
        WorldTimeController controller = peek(level);
        if (controller != null) {
            controller.onChunkLoaded(chunkPos);
        }
    }

    public static void onChunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
        WorldTimeController controller = peek(level);
        if (controller != null) {
            controller.onChunkUnloaded(chunkPos);
        }
    }

    /**
     * 返回 -1 表示保留原版时间推进；其他返回值直接作为本 tick 的昼时增量。
     */
    public static long tryAdvanceDaytime(ServerLevel level) {
        if (!ModConfigs.TIME_BEARING_ENABLED.get()) {
            return LET_VANILLA_HANDLE_TIME;
        }

        WorldTimeController controller = peek(level);
        if (level.getDayTimePerTick() >= 0.0F) {
            if (controller != null) {
                controller.clearPending();
            }
            return LET_VANILLA_HANDLE_TIME;
        }

        return controller(level).consumeDaytimeAdvance();
    }

    /**
     * 时间锁玩法使用 doDaylightCycle=false 冻结原版时间；时间轴承启用时仍需让
     * ServerLevel.tickTime 进入 advanceDaytime 分支，由本机制返回 0 或解锁后的固定时间速率。
     */
    public static boolean shouldOverrideDaylightCycle(ServerLevel level) {
        return ModConfigs.TIME_BEARING_ENABLED.get() && level.getDayTimePerTick() < 0.0F;
    }

    public static boolean shouldAllowSleepTimeSkip(ServerLevel level) {
        return shouldOverrideDaylightCycle(level)
                && ModConfigs.TIME_BEARING_ALLOW_SLEEP_TIME_SKIP.get();
    }

    public static void initializeServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            controller(level).initialize();
        }
    }

    public static void tickExistingControllers(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            WorldTimeController controller = peek(level);
            if (controller != null) {
                controller.serverTick();
            }
        }
    }

    public static void onLevelUnloaded(ServerLevel level) {
        CONTROLLERS.remove(level);
    }

    public static void clearServer(MinecraftServer server) {
        CONTROLLERS.keySet().removeIf(level -> level.getServer() == server);
    }

    private static WorldTimeController controller(ServerLevel level) {
        return CONTROLLERS.computeIfAbsent(level, WorldTimeController::new);
    }

    @Nullable
    private static WorldTimeController peek(ServerLevel level) {
        return CONTROLLERS.get(level);
    }
}
