package com.minechai.create_sandustry.content.block;

import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.content.block.entity.BrushProgressAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 可刷方块（可疑沙子、可疑沙砾、猫砂及同类方块）的“防重力摔碎”支持。
 *
 * <p>原版 {@code BrushableBlock#tick} 让方块掉落时会调用
 * {@code FallingBlockEntity#disableDrop()}，导致下落方块在落地时被直接销毁
 * （<b>不放置方块、也不掉落物品</b>，因为原版无法携带方块实体的考古数据）。</p>
 *
 * <p>配置开启时这里做两件事：</p>
 * <ol>
 *     <li>在 {@code FallingBlockEntity#fall} 移除原方块<b>之前</b>，把方块实体的持久化数据
 *         （战利品表 / 已掷出的战利品 / 命中方向）连同刷掘次数一起读出，交给下落方块实体携带；</li>
 *     <li>落地放置后由原版合并逻辑写回新的方块实体，从而保留考古内容。</li>
 * </ol>
 *
 * <p>作用范围以“方块是否为 {@link BrushableBlock}”判定，因此精确覆盖可疑沙子、可疑沙砾、
 * 猫砂以及未来可能出现的同类可刷方块；普通沙子、沙砾、其它特殊沙子与铁砧完全不受影响。</p>
 */
public final class BrushableFallProtection {
    /**
     * 携带刷掘次数用的合成 NBT 键。仅在“下落搬运”时临时写入下落方块实体的 TileEntityData，
     * 不会写进正常存档（{@code BrushableBlockEntity#saveAdditional} 不写它）。
     */
    public static final String BRUSH_COUNT_TAG = "CreatesandustryBrushCount";

    /** 携带刷掘进度“衰减窗口”用的合成 NBT 键（同 {@link #BRUSH_COUNT_TAG}，仅下落搬运时使用）。 */
    public static final String BRUSH_RESET_TICK_TAG = "CreatesandustryBrushResetTick";

    /** 同一次 {@code fall()} 调用内传递方块实体数据（服务端/客户端各自线程安全）。 */
    private static final ThreadLocal<CompoundTag> PENDING_BLOCK_DATA = new ThreadLocal<>();

    private BrushableFallProtection() {
    }

    /** 该方块是否受“防重力摔碎”保护（配置开启且方块是可刷方块）。 */
    public static boolean isProtected(BlockState state) {
        return ModConfigs.PREVENT_BRUSHABLE_BLOCKS_BREAKING_FROM_FALLING.get()
                && state.getBlock() instanceof BrushableBlock;
    }

    /** 暂存即将交给下落方块实体携带的方块实体数据。 */
    public static void stashPendingBlockData(@Nullable CompoundTag data) {
        if (data == null) {
            PENDING_BLOCK_DATA.remove();
        } else {
            PENDING_BLOCK_DATA.set(data);
        }
    }

    /** 取出并清空暂存的方块实体数据。 */
    @Nullable
    public static CompoundTag takePendingBlockData() {
        CompoundTag data = PENDING_BLOCK_DATA.get();
        PENDING_BLOCK_DATA.remove();
        return data;
    }

    /**
     * 在方块被 {@code FallingBlockEntity#fall} 移除之前读取其方块实体数据。
     * 返回 {@code null} 表示不适用（未开启保护、不是可刷方块、或没有方块实体）。
     */
    @Nullable
    public static CompoundTag captureBlockEntityData(Level level, BlockPos pos, BlockState state) {
        if (!isProtected(state)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        CompoundTag data = blockEntity.saveWithoutMetadata(level.registryAccess());
        // brushCount 不属于原版存档内容，这里额外携带，落地后可继续原刷掘进度。
        if (blockEntity instanceof BrushProgressAccess access) {
            data.putInt(BRUSH_COUNT_TAG, access.createsandustry$getBrushCount());
            data.putLong(BRUSH_RESET_TICK_TAG, access.createsandustry$getBrushCountResetsAtTick());
        }
        return data;
    }
}
