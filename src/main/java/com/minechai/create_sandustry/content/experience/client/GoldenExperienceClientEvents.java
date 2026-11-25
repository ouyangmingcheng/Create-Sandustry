package com.minechai.create_sandustry.content.experience.client;

import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = "createsandustry", value = Dist.CLIENT)
public final class GoldenExperienceClientEvents {
    private static ItemDescription.Modifier description;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ModItems.GOLDEN_EXPERIENCE_NUGGET.get())) return;
        if (description == null) {
            description = new ItemDescription.Modifier(ModItems.GOLDEN_EXPERIENCE_NUGGET.get(), Palette.STANDARD_CREATE);
        }
        description.modify(event);
    }
}
