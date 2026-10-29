package com.minechai.create_sandustry.content.event;

import com.minechai.create_sandustry.content.item.ItemStackGoldAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class GoldenSelectionEvents {
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (((ItemStackGoldAccess) (Object) stack).hasGold()) {
            event.getToolTip().add(Component.translatable("tooltip.createsandustry.golden_selection")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private GoldenSelectionEvents() {
    }
}