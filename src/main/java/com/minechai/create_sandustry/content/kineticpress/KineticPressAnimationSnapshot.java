package com.minechai.create_sandustry.content.kineticpress;

import net.minecraft.world.item.ItemStack;

/** Compact client-side animation state for one concurrently processed item. */
public record KineticPressAnimationSnapshot(int id, ItemStack stack, int progress, int totalTime,
                                            int workingHeight) {
    public KineticPressAnimationSnapshot {
        stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}