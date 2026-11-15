package com.minechai.create_sandustry.content.gravity;

import com.minechai.create_sandustry.registry.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/** Permanent interruption history belongs to the stack; total bounces are statistics only. */
public final class GravityMechanismData {
    private GravityMechanismData() {
    }

    public static int totalBounces(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get(), 0));
    }

    public static int interruptions(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get(), 0));
    }

    public static int incrementInterruptions(ItemStack stack) {
        int previous = interruptions(stack);
        int next = previous == Integer.MAX_VALUE ? previous : previous + 1;
        stack.set(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get(), next);
        return next;
    }

    /** Call on a copied stack, then use ItemEntity#setItem to synchronize the changed component. */
    public static int incrementTotal(ItemStack stack) {
        int previous = totalBounces(stack);
        int next = previous == Integer.MAX_VALUE ? previous : previous + 1;
        stack.set(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get(), next);
        return next;
    }
}
