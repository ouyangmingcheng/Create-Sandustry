package com.minechai.create_sandustry.content.timebearing;

import java.util.HashSet;
import java.util.Set;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 每个服务端维度的时间锁控制状态。原版时钟轴承是唯一解锁来源。
 */
final class WorldTimeController {
    private static final long TICKS_PER_DAY = 24000L;

    private final ServerLevel level;
    private final TimeBearingSavedData savedData;
    private final Set<BlockPos> warnedSecondaryBearings = new HashSet<>();
    private final DayTimeAccumulator unlockedDayTime = new DayTimeAccumulator();

    private boolean controllerLoaded;
    private ClockworkBearingGate.Mode mode = ClockworkBearingGate.Mode.LOCKED;
    private double dayTimePerTick;

    WorldTimeController(ServerLevel level) {
        this.level = level;
        this.savedData = TimeBearingSavedData.get(level);
    }

    void initialize() {
        if (ModConfigs.TIME_BEARING_ENABLED.get()) {
            this.applyInitialDayTimeIfNeeded();
        }
    }

    void onClockworkBearingTick(
            ClockworkBearingBlockEntity bearing,
            boolean assembled,
            boolean stalled,
            float networkCapacity) {
        if (!ModConfigs.TIME_BEARING_ENABLED.get()) {
            this.lockNow();
            return;
        }
        if (this.level.getDayTimePerTick() >= 0.0F) {
            // 管理员或其他模组已接管时间倍率，本机制让出控制权。
            this.lockNow();
            return;
        }
        if (bearing.getLevel() != this.level) {
            return;
        }

        BlockPos bearingPos = bearing.getBlockPos();
        BlockPos controllerPos = this.savedData.getControllerPos();

        if (controllerPos == null) {
            if (!assembled) {
                return;
            }
            this.savedData.setControllerPos(bearingPos);
            this.controllerLoaded = true;
            CreateSandustry.LOGGER.info("时钟轴承已在维度 {} 的位置 {} 取得时间锁控制权",
                    this.level.dimension().location(), bearingPos);
            controllerPos = bearingPos;
        }

        if (!controllerPos.equals(bearingPos)) {
            if (assembled && this.warnedSecondaryBearings.add(bearingPos)) {
                CreateSandustry.LOGGER.warn("维度 {} 已由 {} 的时钟轴承控制，忽略位置 {} 的额外时钟轴承",
                        this.level.dimension().location(), controllerPos, bearingPos);
            }
            return;
        }

        this.controllerLoaded = true;
        ClockworkBearingGate.Mode previousMode = this.mode;
        ClockworkBearingGate.State state = ClockworkBearingGate.evaluate(
                assembled,
                stalled,
                bearing.getSpeed(),
                networkCapacity,
                ModConfigs.TIME_BEARING_MINIMUM_NETWORK_CAPACITY.get(),
                ModConfigs.TIME_BEARING_RPM_DRIVEN_MINIMUM_NETWORK_CAPACITY.get(),
                ModConfigs.TIME_BEARING_UNLOCKED_DAY_TIME_PER_TICK.get(),
                ModConfigs.TIME_BEARING_RPM_DAY_TIME_MULTIPLIER.get());
        this.mode = state.mode();
        this.dayTimePerTick = state.dayTimePerTick();

        if (previousMode != this.mode) {
            CreateSandustry.LOGGER.info("维度 {} 的时钟轴承时间锁进入 {} 模式（容量 {} SU，转速 {} RPM，时间速率 {} tick/tick）",
                    this.level.dimension().location(),
                    this.mode,
                    networkCapacity,
                    bearing.getSpeed(),
                    this.dayTimePerTick);
        }

        if (!state.isUnlocked()) {
            // 锁定时不保留小数余量，恢复后不补算锁定期间的时间。
            this.unlockedDayTime.clear();
        }
    }

    void clearPending() {
        this.unlockedDayTime.clear();
    }

    void serverTick() {
        BlockPos controllerPos = this.savedData.getControllerPos();
        if (controllerPos == null) {
            this.lockNow();
            this.controllerLoaded = false;
            return;
        }

        if (!this.level.isLoaded(controllerPos)) {
            this.pauseForUnloadedChunk();
            return;
        }

        BlockEntity blockEntity = this.level.getBlockEntity(controllerPos);
        if (blockEntity instanceof ClockworkBearingBlockEntity) {
            this.controllerLoaded = true;
            return;
        }

        this.clearController("控制源时钟轴承已不存在");
    }

    long consumeDaytimeAdvance() {
        BlockPos controllerPos = this.savedData.getControllerPos();
        if (controllerPos == null
                || !this.controllerLoaded
                || this.mode == ClockworkBearingGate.Mode.LOCKED) {
            this.unlockedDayTime.clear();
            return 0L;
        }

        this.unlockedDayTime.add(this.dayTimePerTick);
        return this.unlockedDayTime.consumeWholeTicks();
    }

    void onControllerRemoved(BlockPos pos) {
        if (pos.equals(this.savedData.getControllerPos())) {
            this.clearController("控制源时钟轴承已被拆除");
        }
    }

    void onChunkLoaded(ChunkPos chunkPos) {
        if (containsController(chunkPos)) {
            // ChunkEvent.Load 可能早于 FULL 状态，只暂停并在下一游戏刻重新验证。
            this.controllerLoaded = false;
            this.lockNow();
        }
    }

    void onChunkUnloaded(ChunkPos chunkPos) {
        if (containsController(chunkPos)) {
            this.pauseForUnloadedChunk();
        }
    }

    private void applyInitialDayTimeIfNeeded() {
        if (this.savedData.isInitialDayTimeApplied()) {
            return;
        }

        if (ModConfigs.TIME_BEARING_APPLY_INITIAL_DAY_TIME_ONCE.get()) {
            long targetTime = initialDayTimeTarget(
                    this.level.getDayTime(),
                    ModConfigs.TIME_BEARING_INITIAL_DAY_TIME.get());
            if (this.level.getDayTime() != targetTime) {
                this.level.setDayTime(targetTime);
                this.level.getServer().forceTimeSynchronization();
            }
            CreateSandustry.LOGGER.info("时间锁初始化维度 {} 的日内时间为 {} tick",
                    this.level.dimension().location(), ModConfigs.TIME_BEARING_INITIAL_DAY_TIME.get());
        }

        this.savedData.setInitialDayTimeApplied(true);
    }

    static long initialDayTimeTarget(long currentDayTime, int initialDayTime) {
        long dayBase = currentDayTime - Math.floorMod(currentDayTime, TICKS_PER_DAY);
        return dayBase + Math.floorMod(initialDayTime, (int) TICKS_PER_DAY);
    }

    private boolean containsController(ChunkPos chunkPos) {
        BlockPos controllerPos = this.savedData.getControllerPos();
        return controllerPos != null && new ChunkPos(controllerPos).equals(chunkPos);
    }

    private void pauseForUnloadedChunk() {
        this.controllerLoaded = false;
        this.lockNow();
    }

    private void lockNow() {
        this.mode = ClockworkBearingGate.Mode.LOCKED;
        this.dayTimePerTick = 0.0D;
        this.unlockedDayTime.clear();
    }

    private void clearController(String reason) {
        CreateSandustry.LOGGER.info("维度 {} 的时间锁控制已释放：{}",
                this.level.dimension().location(), reason);
        this.savedData.setControllerPos(null);
        this.controllerLoaded = false;
        this.lockNow();
        this.warnedSecondaryBearings.clear();
    }
}
