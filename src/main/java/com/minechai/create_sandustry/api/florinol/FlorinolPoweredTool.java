package com.minechai.create_sandustry.api.florinol;

import net.minecraft.world.item.ItemStack;

/**
 * Opt-in interface for tools that can bind to a Florinol battery and consume FE.
 *
 * <p>Tools call {@link FlorinolToolEnergy#tryConsume} for each operation. The common
 * binding interaction and tooltip already recognize every implementation of this
 * interface, so no additional event registration is required.</p>
 */
public interface FlorinolPoweredTool {
    /** FE consumed for one completed operation, such as breaking one block. */
    int getFlorinolFeCostPerOperation(ItemStack stack);

    default boolean canBindFlorinolBattery(ItemStack stack) {
        return true;
    }
}