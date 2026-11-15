package com.minechai.create_sandustry.content.moonhop;

import net.minecraft.util.Mth;

/** Shared progress math used by both the tooltip and the item decorator. */
public final class MoonhopProgress {
    public static final int BAR_PIXEL_WIDTH = 13;

    public static float ratio(int progress, int target) {
        if (target <= 0) {
            return 0.0F;
        }
        return Mth.clamp((float) progress / target, 0.0F, 1.0F);
    }

    public static int percentage(int progress, int target) {
        if (target <= 0) {
            return 0;
        }
        return Math.round(progress * 100.0F / target);
    }

    public static int barWidth(int progress, int target) {
        float ratio = ratio(progress, target);
        int width = Mth.floor(ratio * BAR_PIXEL_WIDTH);
        if (ratio > 0.0F && width == 0) {
            return 1;
        }
        return width;
    }

    public static int barColor(float ratio) {
        float clamped = Mth.clamp(ratio, 0.0F, 1.0F);
        int red = Math.round(255.0F + (70.0F - 255.0F) * clamped);
        int green = Math.round(192.0F + (255.0F - 192.0F) * clamped);
        int blue = Math.round(116.0F + (224.0F - 116.0F) * clamped);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private MoonhopProgress() {
    }
}


