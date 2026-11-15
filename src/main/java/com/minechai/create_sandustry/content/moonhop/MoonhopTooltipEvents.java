package com.minechai.create_sandustry.content.moonhop;

import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client-visible progress information for the incomplete moonhop board. */
public final class MoonhopTooltipEvents {
    private static final String TITLE_KEY = "tooltip.createsandustry.incomplete_moonhop_board.title";
    private static final String PROGRESS_KEY = "tooltip.createsandustry.incomplete_moonhop_board.progress";
    private static final String NEXT_STEP_KEY = "tooltip.createsandustry.incomplete_moonhop_board.next_step";
    private static final String USED_HEIGHTS_KEY = "tooltip.createsandustry.incomplete_moonhop_board.used_heights";
    private static final String USED_HEIGHT_ENTRY_KEY = "tooltip.createsandustry.incomplete_moonhop_board.used_height_entry";

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.INCOMPLETE_MOONHOP_BOARD.get())) {
            return;
        }

        MoonhopProcessState state = stack.get(ModDataComponents.MOONHOP_PROCESS_STATE.get());
        if (state == null || !state.isValid() || hasProcessingTooltip(event)) {
            return;
        }

        event.getToolTip().add(CommonComponents.EMPTY);
        event.getToolTip().add(Component.translatable(TITLE_KEY).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                PROGRESS_KEY,
                state.progress(),
                state.target(),
                percentage(state.progress(), state.target())
        ).withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.translatable(NEXT_STEP_KEY).withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable(USED_HEIGHTS_KEY).withStyle(ChatFormatting.DARK_AQUA));
        for (int y : state.usedY()) {
            event.getToolTip().add(Component.literal("  ")
                    .append(Component.translatable(USED_HEIGHT_ENTRY_KEY, y))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static boolean hasProcessingTooltip(ItemTooltipEvent event) {
        for (Component line : event.getToolTip()) {
            if (line.getContents() instanceof TranslatableContents contents
                    && TITLE_KEY.equals(contents.getKey())) {
                return true;
            }
        }
        return false;
    }

    public static int percentage(int progress, int target) {
        return MoonhopProgress.percentage(progress, target);
    }

    private MoonhopTooltipEvents() {
    }
}


