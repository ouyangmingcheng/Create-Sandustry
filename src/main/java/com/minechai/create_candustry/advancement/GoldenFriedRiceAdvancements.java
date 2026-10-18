package com.minechai.create_candustry.advancement;

import com.minechai.create_candustry.item.ItemStackGoldAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Shared helper that detects when a player actually obtains a Golden Selection
 * Egg Fried Rice and awards the {@code golden_fried_rice} advancement.
 *
 * <p>This deliberately does not depend on which cooking device produced the
 * stack or whether that device recorded a maker. It only checks the concrete
 * {@link ItemStack} that entered the player's inventory/hand.</p>
 */
public final class GoldenFriedRiceAdvancements {
    private static final ResourceLocation EGG_FRIED_RICE_ID =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "egg_fried_rice");

    private GoldenFriedRiceAdvancements() {
    }

    /**
     * Returns {@code true} when the stack is a non-empty Kaleidoscope Cookery
     * egg fried rice whose {@code golden_selection} component is true.
     */
    public static boolean isGoldenEggFriedRice(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() != BuiltInRegistries.ITEM.get(EGG_FRIED_RICE_ID)) {
            return false;
        }
        return ((ItemStackGoldAccess) (Object) stack).hasGold();
    }

    /**
     * Awards the advancement to the player if it is a server player.
     */
    public static void award(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancementTriggers.GOLDEN_FRIED_RICE.get().trigger(serverPlayer);
        }
    }
}
