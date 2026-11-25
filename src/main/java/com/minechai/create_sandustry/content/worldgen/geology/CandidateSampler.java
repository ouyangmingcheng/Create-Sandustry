package com.minechai.create_sandustry.content.worldgen.geology;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

/**
 * 有上限的蓄水池采样器，避免局部扫描顺序导致替换量总集中在区块同一侧。
 */
public final class CandidateSampler {
    private final int limit;
    private final List<BlockPos> selected = new ArrayList<>();
    private int seen;

    public CandidateSampler(int limit) {
        this.limit = Math.max(0, limit);
    }

    public void offer(BlockPos pos, RandomSource random) {
        if (limit == 0) {
            return;
        }

        seen++;
        BlockPos immutable = pos.immutable();
        if (selected.size() < limit) {
            selected.add(immutable);
            return;
        }

        int slot = random.nextInt(seen);
        if (slot < limit) {
            selected.set(slot, immutable);
        }
    }

    public List<BlockPos> selected() {
        return selected;
    }
}
