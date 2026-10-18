package com.minechai.create_candustry.integration;

/**
 * Small shared state for the optional PoopSky integrations. It never references
 * PoopSky classes directly, so it is safe to load even when PoopSky is absent.
 */
public final class PoopskyCompat {
    private static final ThreadLocal<Boolean> REPLACE_POOP_BLOCK = ThreadLocal.withInitial(() -> false);

    public static void beginPoopBlockReplacement() {
        REPLACE_POOP_BLOCK.set(true);
    }

    public static void endPoopBlockReplacement() {
        REPLACE_POOP_BLOCK.set(false);
    }

    public static boolean shouldReplacePoopBlock() {
        return REPLACE_POOP_BLOCK.get();
    }

    private PoopskyCompat() {
    }
}
