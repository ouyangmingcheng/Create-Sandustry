package com.minechai.create_sandustry.content.gravity;

import java.util.function.Supplier;
import java.util.function.Predicate;

import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** The single interruption commit point. Removal/save callbacks never alter wear. */
public final class GravityChainLifecycle {
    private GravityChainLifecycle() {}

    public static void interrupt(ItemEntity entity) {
        if (!entity.hasData(ModAttachments.GRAVITY_BOUNCE)) return;
        GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
        if (tracker.transferring) return;
        if (!entity.isRemoved() && tracker.consecutiveBounces() > 0
                && GravityMechanismAssembly.isAwaitingActivation(entity.level(), entity.getItem())) {
            ItemStack stack = entity.getItem().copy();
            GravityMechanismData.incrementInterruptions(stack);
            entity.setItem(stack);
        }
        tracker.reset();
    }

    /** Prepare BEFORE any inventory copy, commit on actual collection, roll back on rejection. */
    public static <T> T collect(Entity source, Supplier<T> operation) {
        return collect(source, operation, result -> false);
    }

    public static Entity changeDimension(ItemEntity source, Supplier<Entity> operation) {
        Entity result = collect(source, operation, moved -> moved != null);
        if (result instanceof ItemEntity moved) moved.removeData(ModAttachments.GRAVITY_BOUNCE);
        return result;
    }

    private static <T> T collect(Entity source, Supplier<T> operation, Predicate<T> accepted) {
        if (!(source instanceof ItemEntity entity) || entity.level().isClientSide || entity.isRemoved()
                || !entity.hasData(ModAttachments.GRAVITY_BOUNCE)
                || !GravityMechanismAssembly.isAwaitingActivation(entity.level(), entity.getItem())) return operation.get();
        GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
        if (tracker.transferring || tracker.consecutiveBounces() == 0) return operation.get();
        ItemStack before = entity.getItem().copy();
        ItemStack prepared = before.copy();
        GravityMechanismData.incrementInterruptions(prepared);
        tracker.transferring = true;
        entity.setItem(prepared);
        T result = null;
        boolean completed = false;
        try {
            result = operation.get();
            completed = true;
            return result;
        } finally {
            tracker.transferring = false;
            if (entity.isRemoved() || entity.getItem().getCount() < before.getCount()
                    || completed && accepted.test(result)) {
                // The destination already owns the prepared component, even if the source is gone.
                tracker.reset();
            } else if (GravityMechanismAssembly.isAwaitingActivation(entity.level(), entity.getItem())) {
                entity.setItem(before);
            }
        }
    }

    /** Chute suction has already passed acceptance checks and is about to copy into storage. */
    public static ItemStack collectedCopy(ItemEntity entity, ItemStack copy) {
        interrupt(entity);
        // The argument may have been copied before the interruption was recorded.
        ItemStack result = copy.copy();
        if (GravityMechanismAssembly.isAwaitingActivation(entity.level(), result))
            result.set(com.minechai.create_sandustry.registry.ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get(),
                    GravityMechanismData.interruptions(entity.getItem()));
        return result;
    }
}
