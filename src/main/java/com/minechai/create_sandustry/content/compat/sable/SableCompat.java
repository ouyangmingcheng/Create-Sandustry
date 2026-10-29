package com.minechai.create_sandustry.content.compat.sable;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

/**
 * Optional Sable integration.
 *
 * <p>Sable is resolved reflectively so it never becomes a compile-time or hard
 * runtime dependency of Create: Sandustry. Callers should still check
 * {@code ModList.get().isLoaded("sable")} to avoid the reflection cost when the
 * mod is absent.</p>
 */
public final class SableCompat {
    private static Object helper;
    private static Method getContaining;
    private static boolean resolved;
    private static boolean available;

    /**
     * Returns whether the given block position belongs to a physicalized Sable
     * sub-level instead of the normal world.
     */
    public static boolean isInSubLevel(Level level, BlockPos pos) {
        if (!resolved) {
            resolve();
        }
        if (!available) {
            return false;
        }

        try {
            return getContaining.invoke(helper, level, pos) != null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            available = false;
            return false;
        }
    }

    private static void resolve() {
        resolved = true;
        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable", true, SableCompat.class.getClassLoader());
            helper = sableClass.getField("HELPER").get(null);
            getContaining = helper.getClass().getMethod("getContaining", Level.class, Vec3i.class);
            available = helper != null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            available = false;
        }
    }

    private SableCompat() {
    }
}