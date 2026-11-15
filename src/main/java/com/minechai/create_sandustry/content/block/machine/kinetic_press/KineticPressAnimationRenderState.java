package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import net.minecraft.world.item.ItemStack;

/**
 * Display interpolation endpoints, corrected by authoritative snapshots and
 * bounded local prediction. Partial ticks never change authoritative tasks.
 */
public record KineticPressAnimationRenderState(int id, ItemStack stack, int totalTime,
                                               int workingHeight, double previousProgress,
                                               double currentProgress) {
    public KineticPressAnimationRenderState {
        stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        previousProgress = Math.max(0.0D, previousProgress);
        currentProgress = Math.max(previousProgress, currentProgress);
    }

    public double progressAt(float partialTick) {
        double fraction = Math.clamp(partialTick, 0.0D, 1.0D);
        return previousProgress + (currentProgress - previousProgress) * fraction;
    }
}
