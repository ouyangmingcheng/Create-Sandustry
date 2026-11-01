package com.minechai.create_sandustry.content.steamwhistle;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

/**
 * Create 蒸汽笛在逻辑服务端上出现一次新的红石上升沿时发布。
 *
 * <p>该事件只表示方块从 {@code POWERED=false} 变为 {@code POWERED=true}。持续通电
 * 不会重复触发；断电也不会触发本事件。事件不修改蒸汽笛原有状态或发声逻辑。</p>
 */
public final class SteamWhistlePerformanceEvent extends Event {
    /** 三档音区在 pitch=0 时对应的 MIDI 音符。 */
    public static final int SMALL_BASE_MIDI = 78;  // F#5
    public static final int MEDIUM_BASE_MIDI = 66; // F#4
    public static final int LARGE_BASE_MIDI = 54;  // F#3

    private final ServerLevel level;
    private final BlockPos whistlePos;
    private final WhistleBlockEntity whistle;
    private final BlockState previousState;
    private final BlockState newState;
    private final BlockPos sourcePos;
    private final BlockState sourceState;
    private final Block notifierBlock;
    private final WhistleSize size;
    private final int pitch;
    private final int pitchId;
    private final int midiNote;
    private final boolean audibleOnServer;
    private final long gameTime;

    public SteamWhistlePerformanceEvent(
            ServerLevel level,
            BlockPos whistlePos,
            WhistleBlockEntity whistle,
            BlockState previousState,
            BlockState newState,
            BlockPos sourcePos,
            BlockState sourceState,
            Block notifierBlock) {
        this.level = level;
        this.whistlePos = whistlePos.immutable();
        this.whistle = whistle;
        this.previousState = previousState;
        this.newState = newState;
        this.sourcePos = sourcePos.immutable();
        this.sourceState = sourceState;
        this.notifierBlock = notifierBlock;
        this.size = whistle.getBlockState().getValue(WhistleBlock.SIZE);
        this.pitchId = whistle.getPitchId();
        this.pitch = pitchId - 100 * size.ordinal();
        this.midiNote = toMidiNote(size, pitch);
        this.audibleOnServer = hasAudibleSteam(whistle);
        this.gameTime = level.getGameTime();
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getWhistlePos() {
        return whistlePos;
    }

    public WhistleBlockEntity getWhistle() {
        return whistle;
    }

    public BlockState getPreviousState() {
        return previousState;
    }

    public BlockState getNewState() {
        return newState;
    }

    /** 本次邻居更新所来自的方块坐标；不一定是最终红石电源本体。 */
    public BlockPos getSourcePos() {
        return sourcePos;
    }

    /** 邻居更新来源坐标当前的实际方块状态。 */
    public BlockState getSourceState() {
        return sourceState;
    }

    /** neighborChanged 回调传入的方块参数；可能代表变化前状态，不要当作电源类型。 */
    public Block getNotifierBlock() {
        return notifierBlock;
    }

    public WhistleSize getSize() {
        return size;
    }

    /** Create 内部音高值：从该音区的 F# 向下计算的半音数。 */
    public int getPitch() {
        return pitch;
    }

    /** Create 的公开编码：pitch + 100 * size.ordinal()。 */
    public int getPitchId() {
        return pitchId;
    }

    public int getMidiNote() {
        return midiNote;
    }

    /**
     * 按客户端同款锅炉条件计算的服务端可发声状态。
     *
     * <p>这里只判断锅炉是否有热源；不检查客户端距离、声音音量或声音事件。</p>
     */
    public boolean isAudibleOnServer() {
        return audibleOnServer;
    }

    public long getGameTime() {
        return gameTime;
    }

    public static int toMidiNote(WhistleSize size, int pitch) {
        int base = switch (size) {
            case SMALL -> SMALL_BASE_MIDI;
            case MEDIUM -> MEDIUM_BASE_MIDI;
            case LARGE -> LARGE_BASE_MIDI;
        };
        return base - pitch;
    }

    private static boolean hasAudibleSteam(WhistleBlockEntity whistle) {
        FluidTankBlockEntity tank = whistle.getTank();
        return tank != null
                && tank.boiler != null
                && tank.boiler.isActive()
                && (tank.boiler.passiveHeat || tank.boiler.activeHeat > 0);
    }
}
