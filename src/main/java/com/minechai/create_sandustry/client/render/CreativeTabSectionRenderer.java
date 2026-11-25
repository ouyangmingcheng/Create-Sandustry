package com.minechai.create_sandustry.client.render;

import com.minechai.create_sandustry.registry.ModCreativeTabs;
import com.minechai.create_sandustry.registry.ModCreativeTabs.ScreenLayout;
import com.minechai.create_sandustry.registry.ModCreativeTabs.SectionBanner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Sand-gold section banners occupy empty rows, so their icons cannot be taken as items. */
public final class CreativeTabSectionRenderer {
    public static void render(GuiGraphics graphics, ScreenLayout layout, int itemCount, float scroll) {
        int scrollableRows = (itemCount + ModCreativeTabs.COLUMNS - 1) / ModCreativeTabs.COLUMNS - 5;
        // Match ItemPickerMenu's rounding for both the wheel and scrollbar dragging.
        int firstRow = Math.max((int) ((double) (scroll * scrollableRows) + 0.5D), 0);
        for (SectionBanner banner : layout.banners()) {
            int visibleRow = banner.row() - firstRow;
            if (visibleRow < 0 || visibleRow >= 5) continue;
            int x = 8;
            int y = 17 + visibleRow * 18;
            graphics.fill(x, y, x + 162, y + 18, 0xFF413527);
            graphics.fill(x + 1, y + 1, x + 161, y + 17, 0xFF665039);
            graphics.fill(x + 1, y + 1, x + 161, y + 2, 0xFFC4A76A);
            graphics.renderItem(banner.section().icon(), x + 2, y + 1);
            graphics.drawString(Minecraft.getInstance().font, banner.section().title(), x + 23, y + 5,
                    0xFFFFE1A1, false);
        }
    }

    private CreativeTabSectionRenderer() {}
}
