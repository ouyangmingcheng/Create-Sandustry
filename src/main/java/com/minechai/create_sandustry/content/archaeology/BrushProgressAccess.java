package com.minechai.create_sandustry.content.archaeology;

/**
 * 由 {@code BrushableBlockEntityMixin} 实现，用于在“下落方块搬运”时读写刷掘次数
 * （{@code BrushableBlockEntity.brushCount}）。
 *
 * <p>刷掘次数在原版并不写入存档，这里只在把可刷方块交给下落方块实体搬运时临时携带，
 * 落地后由 {@code BrushableBlockEntityMixin} 恢复，使玩家能够接着原进度继续刷掘。</p>
 */
public interface BrushProgressAccess {
    int createsandustry$getBrushCount();

    void createsandustry$setBrushCount(int count);

    long createsandustry$getBrushCountResetsAtTick();

    void createsandustry$setBrushCountResetsAtTick(long tick);
}
