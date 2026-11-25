package com.minechai.create_sandustry.content.moonhop.client;

import com.minechai.create_sandustry.content.moonhop.MoonhopProcessState;
import com.minechai.create_sandustry.content.moonhop.MoonhopProgress;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/** Draws the incomplete moonhop board's progress bar over its item icon. */
public final class MoonhopBoardItemDecorator {
    private static final int BACKGROUND = 0xFF000000;

    private static final IItemDecorator DECORATOR = (guiGraphics, font, stack, xOffset, yOffset) -> {
        if (!stack.is(ModItems.INCOMPLETE_MOONHOP_BOARD.get())) {
            return false;
        }

        MoonhopProcessState state = stack.get(ModDataComponents.MOONHOP_PROCESS_STATE.get());
        if (state == null || !state.isValid()) {
            return false;
        }

        float ratio = MoonhopProgress.ratio(state.progress(), state.target());
        int width = MoonhopProgress.barWidth(state.progress(), state.target());
        int x = xOffset + 2;
        int y = yOffset + 13;

        guiGraphics.fill(x, y, x + MoonhopProgress.BAR_PIXEL_WIDTH, y + 2, BACKGROUND);
        if (width > 0) {
            guiGraphics.fill(x, y, x + width, y + 1, MoonhopProgress.barColor(ratio));
        }
        return false;
    };

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MoonhopBoardItemDecorator::onRegisterItemDecorations);
    }

    private static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModItems.INCOMPLETE_MOONHOP_BOARD.get(), DECORATOR);
    }

    private MoonhopBoardItemDecorator() {
    }
}

