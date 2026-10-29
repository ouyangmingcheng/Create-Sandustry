package com.minechai.create_sandustry.content.timebearing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * 每个维度独立保存时钟轴承时间锁控制源。运行中的解锁状态和小数累计不会写入这里，
 * 以避免服务器重启或区块重载时补算停机期间的时间。
 */
final class TimeBearingSavedData extends SavedData {
    private static final String DATA_NAME = "createsandustry_time_bearing";
    private static final String CONTROLLER_POS_TAG = "ControllerPos";
    private static final String INITIAL_DAY_TIME_APPLIED_TAG = "InitialDayTimeApplied";

    @Nullable
    private BlockPos controllerPos;
    private boolean initialDayTimeApplied;

    static TimeBearingSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TimeBearingSavedData::new, TimeBearingSavedData::load),
                DATA_NAME);
    }

    @Nullable
    BlockPos getControllerPos() {
        return this.controllerPos;
    }

    void setControllerPos(@Nullable BlockPos pos) {
        BlockPos immutablePos = pos == null ? null : pos.immutable();
        if (java.util.Objects.equals(this.controllerPos, immutablePos)) {
            return;
        }
        this.controllerPos = immutablePos;
        this.setDirty();
    }

    boolean isInitialDayTimeApplied() {
        return this.initialDayTimeApplied;
    }

    void setInitialDayTimeApplied(boolean applied) {
        if (this.initialDayTimeApplied == applied) {
            return;
        }
        this.initialDayTimeApplied = applied;
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.controllerPos != null) {
            tag.putLong(CONTROLLER_POS_TAG, this.controllerPos.asLong());
        }
        tag.putBoolean(INITIAL_DAY_TIME_APPLIED_TAG, this.initialDayTimeApplied);
        return tag;
    }

    private static TimeBearingSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TimeBearingSavedData data = new TimeBearingSavedData();
        if (tag.contains(CONTROLLER_POS_TAG, Tag.TAG_LONG)) {
            data.controllerPos = BlockPos.of(tag.getLong(CONTROLLER_POS_TAG));
        }
        data.initialDayTimeApplied = tag.getBoolean(INITIAL_DAY_TIME_APPLIED_TAG);
        return data;
    }
}
