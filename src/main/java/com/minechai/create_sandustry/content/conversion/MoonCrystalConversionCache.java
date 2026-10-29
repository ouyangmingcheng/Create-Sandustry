package com.minechai.create_sandustry.content.conversion;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Keeps a simulated extraction result stable until its matching execute call.
 * Create's Mechanical Arm simulates extraction before committing it.
 */
public final class MoonCrystalConversionCache {
    private static final long CACHE_LIFETIME = 20L;

    private ItemStack source = ItemStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    private long rolledAt;

    public ItemStack resolve(Level level, ItemStack source) {
        long now = level.getGameTime();
        if (result.isEmpty()
                || now - rolledAt > CACHE_LIFETIME
                || !ItemStack.isSameItemSameComponents(this.source, source)) {
            this.source = source.copy();
            this.result = MoonCrystalConversion.rollResult(level, source);
            this.rolledAt = now;
        }
        return this.result.copyWithCount(1);
    }

    public void clear() {
        source = ItemStack.EMPTY;
        result = ItemStack.EMPTY;
        rolledAt = 0L;
    }
}
