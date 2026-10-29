package com.minechai.create_sandustry.content.client.effect;

import com.minechai.create_sandustry.content.item.ItemStackGoldAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GoldenGlintContext {
    private static final ThreadLocal<Boolean> GOLDEN = new ThreadLocal<>();

    public static void begin(ItemStack stack) {
        if (((ItemStackGoldAccess) (Object) stack).hasGold()) {
            GOLDEN.set(Boolean.TRUE);
        } else {
            GOLDEN.remove();
        }
    }

    public static boolean isGolden() {
        return Boolean.TRUE.equals(GOLDEN.get());
    }

    public static void end() {
        GOLDEN.remove();
    }

    private GoldenGlintContext() {
    }
}